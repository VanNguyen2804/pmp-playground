package com.example.pmp.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionAnswerHistoryService {
    private final QuestionRepository questionRepository;
    private final QuestionAnswerAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;

    public QuestionAnswerHistoryService(QuestionRepository questionRepository,
                                        QuestionAnswerAttemptRepository attemptRepository,
                                        ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
    }

    public AnswerAttemptResponse submit(Long questionId, AnswerAttemptRequest request) {
        Question question = findQuestion(questionId);
        SubmittedAnswer submitted = normalizeAndValidate(question, request);
        boolean correct = evaluate(question, submitted);

        QuestionAnswerAttempt attempt = new QuestionAnswerAttempt();
        attempt.setQuestion(question);
        attempt.setQuestionType(question.getQuestionType());
        attempt.setSubmittedAnswerJson(writeSubmittedAnswer(submitted));
        attempt.setCorrect(correct);
        attempt.setSessionId(blankToNull(request.sessionId()));

        QuestionAnswerAttempt saved = attemptRepository.saveAndFlush(attempt);
        return toResponse(saved, question, submitted, summary(questionId));
    }

    @Transactional(readOnly = true)
    public Page<AnswerAttemptResponse> history(Long questionId, int page, int size) {
        Question question = findQuestion(questionId);
        AnswerHistorySummary summary = summary(questionId);
        return attemptRepository.findByQuestion_IdOrderByAnsweredAtDescIdDesc(
                        questionId,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(attempt -> toResponse(attempt, question, readSubmittedAnswer(attempt), summary));
    }

    @Transactional(readOnly = true)
    public AnswerHistorySummary summary(Long questionId) {
        findQuestion(questionId);
        long total = attemptRepository.countByQuestion_Id(questionId);
        long correct = attemptRepository.countByQuestion_IdAndCorrectTrue(questionId);
        long incorrect = total - correct;
        var last = attemptRepository.findTopByQuestion_IdOrderByAnsweredAtDescIdDesc(questionId);
        double accuracy = total == 0 ? 0.0 : Math.round((correct * 10_000.0) / total) / 100.0;

        return new AnswerHistorySummary(
                questionId,
                total,
                correct,
                incorrect,
                accuracy,
                last.map(QuestionAnswerAttempt::getAnsweredAt).orElse(null),
                last.map(QuestionAnswerAttempt::isCorrect).orElse(null)
        );
    }

    private SubmittedAnswer normalizeAndValidate(Question question, AnswerAttemptRequest request) {
        if (request == null) throw new IllegalArgumentException("Answer request is required.");

        if (question.getQuestionType() == QuestionType.MATCHING) {
            Map<String, String> submittedMatching = normalizeMatching(request.matchingAnswers());
            Set<String> expectedLeftItems = question.getMatchingPairs().stream()
                    .map(MatchingPair::getLeftText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!submittedMatching.keySet().equals(expectedLeftItems)) {
                throw new IllegalArgumentException("Matching answer must contain exactly one answer for every left-side item.");
            }
            return new SubmittedAnswer(new LinkedHashSet<>(), submittedMatching);
        }

        Set<String> selected = normalizeSelected(request.selectedAnswers());
        Set<String> availableKeys = question.getOptions().stream()
                .map(QuestionOption::getOptionKey)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!availableKeys.containsAll(selected)) {
            throw new IllegalArgumentException("Selected answer contains an option that does not belong to this question.");
        }
        if (question.getQuestionType() == QuestionType.MCQ && selected.size() != 1) {
            throw new IllegalArgumentException("MCQ requires exactly one selected answer.");
        }
        if (question.getQuestionType() == QuestionType.MRQ && selected.isEmpty()) {
            throw new IllegalArgumentException("MRQ requires at least one selected answer.");
        }
        return new SubmittedAnswer(selected, new LinkedHashMap<>());
    }

    private boolean evaluate(Question question, SubmittedAnswer submitted) {
        if (question.getQuestionType() == QuestionType.MATCHING) {
            Map<String, String> expected = correctMatchingAnswers(question);
            return expected.equals(submitted.matchingAnswers());
        }
        return correctAnswerKeys(question).equals(submitted.selectedAnswers());
    }

    private AnswerAttemptResponse toResponse(QuestionAnswerAttempt attempt, Question question,
                                             SubmittedAnswer submitted, AnswerHistorySummary summary) {
        return new AnswerAttemptResponse(
                attempt.getId(),
                question.getId(),
                attempt.getQuestionType(),
                Collections.unmodifiableSet(submitted.selectedAnswers()),
                Collections.unmodifiableMap(submitted.matchingAnswers()),
                attempt.isCorrect(),
                Collections.unmodifiableSet(correctAnswerKeys(question)),
                Collections.unmodifiableMap(correctMatchingAnswers(question)),
                attempt.getSessionId(),
                attempt.getAnsweredAt(),
                summary
        );
    }

    private Set<String> correctAnswerKeys(Question question) {
        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .map(QuestionOption::getOptionKey)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, String> correctMatchingAnswers(Question question) {
        Map<String, String> expected = new LinkedHashMap<>();
        question.getMatchingPairs().forEach(pair -> expected.put(pair.getLeftText(), pair.getRightText()));
        return expected;
    }

    private Set<String> normalizeSelected(Set<String> values) {
        if (values == null) return new LinkedHashSet<>();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, String> normalizeMatching(Map<String, String> values) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (values == null) return normalized;
        values.forEach((left, right) -> {
            String normalizedLeft = left == null ? "" : left.trim();
            String normalizedRight = right == null ? "" : right.trim();
            if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
                throw new IllegalArgumentException("Matching answers cannot contain blank values.");
            }
            if (normalized.put(normalizedLeft, normalizedRight) != null) {
                throw new IllegalArgumentException("Matching answer contains a duplicate left-side item.");
            }
        });
        return normalized;
    }

    private String writeSubmittedAnswer(SubmittedAnswer answer) {
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize submitted answer.", ex);
        }
    }

    private SubmittedAnswer readSubmittedAnswer(QuestionAnswerAttempt attempt) {
        try {
            return objectMapper.readValue(attempt.getSubmittedAnswerJson(), SubmittedAnswer.class);
        } catch (JsonProcessingException ex) {
            return new SubmittedAnswer(new LinkedHashSet<>(), new LinkedHashMap<>());
        }
    }

    private Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record SubmittedAnswer(Set<String> selectedAnswers, Map<String, String> matchingAnswers) {
        private SubmittedAnswer {
            selectedAnswers = selectedAnswers == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(selectedAnswers);
            matchingAnswers = matchingAnswers == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(matchingAnswers);
        }
    }
}
