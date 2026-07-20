package com.example.pmp.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.DateTimeException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionAnswerHistoryService {
    private final QuestionRepository questionRepository;
    private final QuestionAnswerAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;
    private final Pmbok8StudyRecommendationService recommendationService;

    public QuestionAnswerHistoryService(QuestionRepository questionRepository,
                                        QuestionAnswerAttemptRepository attemptRepository,
                                        ObjectMapper objectMapper,
                                        Pmbok8StudyRecommendationService recommendationService) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
        this.recommendationService = recommendationService;
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

    @Transactional(readOnly = true)
    public PracticeDashboardResponse dashboard(String requestedTimeZone) {
        ZoneId zoneId = resolveZoneId(requestedTimeZone);
        LocalDate today = LocalDate.now(zoneId);
        Instant todayStart = today.atStartOfDay(zoneId).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(zoneId).toInstant();

        List<QuestionAnswerAttempt> all = attemptRepository.findAllByOrderByAnsweredAtDescIdDesc();
        Map<Long, QuestionAnswerAttempt> latestByQuestion = new LinkedHashMap<>();
        for (QuestionAnswerAttempt attempt : all) {
            latestByQuestion.putIfAbsent(attempt.getQuestion().getId(), attempt);
        }

        long wrongQuestions = latestByQuestion.values().stream()
                .filter(attempt -> !attempt.isCorrect())
                .count();
        long answeredQuestions = latestByQuestion.size();
        long totalQuestions = questionRepository.count();
        double questionBankCoverage = percentage(answeredQuestions, totalQuestions);

        List<QuestionAnswerAttempt> todayAttempts = all.stream()
                .filter(attempt -> !attempt.getAnsweredAt().isBefore(todayStart))
                .filter(attempt -> attempt.getAnsweredAt().isBefore(tomorrowStart))
                .toList();
        List<QuestionAnswerAttempt> yesterdayAttempts = all.stream()
                .filter(attempt -> !attempt.getAnsweredAt().isBefore(yesterdayStart))
                .filter(attempt -> attempt.getAnsweredAt().isBefore(todayStart))
                .toList();

        long todayCorrect = todayAttempts.stream().filter(QuestionAnswerAttempt::isCorrect).count();
        long yesterdayCorrect = yesterdayAttempts.stream().filter(QuestionAnswerAttempt::isCorrect).count();
        double todayAccuracy = percentage(todayCorrect, todayAttempts.size());
        double yesterdayAccuracy = percentage(yesterdayCorrect, yesterdayAttempts.size());
        double dailyDelta = round2(todayAccuracy - yesterdayAccuracy);
        String dailyPerformanceStatus = progressStatus(
                todayAttempts.size(), yesterdayAttempts.size(), dailyDelta);

        long streak = 0;
        for (QuestionAnswerAttempt attempt : all) {
            if (!attempt.isCorrect()) break;
            streak++;
        }

        return new PracticeDashboardResponse(
                wrongQuestions,
                todayAccuracy,
                todayCorrect,
                todayAttempts.size(),
                yesterdayAccuracy,
                yesterdayCorrect,
                yesterdayAttempts.size(),
                dailyDelta,
                dailyPerformanceStatus,
                answeredQuestions,
                totalQuestions,
                questionBankCoverage,
                streak,
                wrongQuestions,
                zoneId.getId());
    }

    @Transactional(readOnly = true)
    public PracticeAnalyticsResponse analytics(int requestedDays, int requestedTop, String requestedTimeZone) {
        int days = Math.min(Math.max(requestedDays, 7), 90);
        int top = Math.min(Math.max(requestedTop, 1), 25);
        ZoneId zoneId = resolveZoneId(requestedTimeZone);
        Instant now = Instant.now();
        Instant currentFrom = now.minus(Duration.ofDays(days));
        Instant previousFrom = now.minus(Duration.ofDays(days * 2L));

        List<QuestionAnswerAttempt> attempts = attemptRepository
                .findByAnsweredAtGreaterThanEqualOrderByAnsweredAtDescIdDesc(previousFrom);
        List<QuestionAnswerAttempt> current = attempts.stream()
                .filter(attempt -> !attempt.getAnsweredAt().isBefore(currentFrom))
                .toList();
        List<QuestionAnswerAttempt> previous = attempts.stream()
                .filter(attempt -> attempt.getAnsweredAt().isBefore(currentFrom))
                .toList();

        long currentCorrect = current.stream().filter(QuestionAnswerAttempt::isCorrect).count();
        long currentIncorrect = current.size() - currentCorrect;
        long previousCorrect = previous.stream().filter(QuestionAnswerAttempt::isCorrect).count();
        double currentAccuracy = percentage(currentCorrect, current.size());
        double previousAccuracy = percentage(previousCorrect, previous.size());
        double improvement = round2(currentAccuracy - previousAccuracy);
        String progressStatus = progressStatus(current.size(), previous.size(), improvement);

        Map<String, CategoryAccumulator> categoryMap = new LinkedHashMap<>();
        for (QuestionAnswerAttempt attempt : current) {
            Question question = attempt.getQuestion();
            if (question.getCategories().isEmpty()) {
                categoryMap.computeIfAbsent("UNCATEGORIZED",
                                ignored -> new CategoryAccumulator("UNCATEGORIZED", "Chưa phân loại"))
                        .accept(attempt);
                continue;
            }
            question.getCategories().forEach(category -> categoryMap
                    .computeIfAbsent(category.getCode(),
                            ignored -> new CategoryAccumulator(category.getCode(), category.getName()))
                    .accept(attempt));
        }

        List<CategoryMistakeStatistic> categoryStats = categoryMap.values().stream()
                .map(CategoryAccumulator::toRecord)
                .sorted(Comparator.comparingLong(CategoryMistakeStatistic::incorrectAttempts).reversed()
                        .thenComparingDouble(CategoryMistakeStatistic::accuracyPercentage)
                        .thenComparing(CategoryMistakeStatistic::categoryName))
                .toList();

        CategoryMistakeStatistic mostWrong = categoryStats.stream().findFirst().orElse(null);
        PracticeAnalyticsSummary summary = new PracticeAnalyticsSummary(
                current.size(),
                currentCorrect,
                currentIncorrect,
                currentAccuracy,
                previous.size(),
                previousAccuracy,
                improvement,
                progressStatus,
                mostWrong == null ? null : mostWrong.categoryCode(),
                mostWrong == null ? null : mostWrong.categoryName(),
                mostWrong == null ? 0 : mostWrong.incorrectAttempts()
        );

        Map<LocalDate, DailyAccumulator> dailyMap = new LinkedHashMap<>();
        LocalDate firstDate = LocalDate.now(zoneId).minusDays(days - 1L);
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = firstDate.plusDays(offset);
            dailyMap.put(date, new DailyAccumulator(date));
        }
        for (QuestionAnswerAttempt attempt : current) {
            LocalDate date = attempt.getAnsweredAt().atZone(zoneId).toLocalDate();
            DailyAccumulator accumulator = dailyMap.get(date);
            if (accumulator != null) accumulator.accept(attempt);
        }
        List<DailyAccuracyStatistic> dailyTrend = dailyMap.values().stream()
                .map(DailyAccumulator::toRecord)
                .toList();

        Map<Long, QuestionAccumulator> questionMap = new LinkedHashMap<>();
        for (QuestionAnswerAttempt attempt : current) {
            questionMap.computeIfAbsent(attempt.getQuestion().getId(),
                            ignored -> new QuestionAccumulator(attempt.getQuestion()))
                    .accept(attempt);
        }
        List<TopWrongQuestionStatistic> topWrongQuestions = questionMap.values().stream()
                .filter(accumulator -> accumulator.incorrect > 0)
                .sorted(Comparator.comparingLong((QuestionAccumulator value) -> value.incorrect).reversed()
                        .thenComparingLong(value -> value.correct)
                        .thenComparing(value -> value.question.getId()))
                .limit(top)
                .map(QuestionAccumulator::toRecord)
                .toList();

        return new PracticeAnalyticsResponse(
                now,
                days,
                zoneId.getId(),
                summary,
                categoryStats,
                dailyTrend,
                topWrongQuestions
        );
    }

    @Transactional(readOnly = true)
    public PracticeSessionReportResponse sessionReport(PracticeSessionReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Session report request is required.");
        }
        String sessionId = blankToNull(request.sessionId());
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required.");
        }

        LinkedHashSet<Long> requestedIds = request.questionIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            throw new IllegalArgumentException("At least one question ID is required.");
        }

        Map<Long, QuestionAnswerAttempt> latestByQuestion = new LinkedHashMap<>();
        for (QuestionAnswerAttempt attempt : attemptRepository.findBySessionIdOrderByAnsweredAtDescIdDesc(sessionId)) {
            Long questionId = attempt.getQuestion().getId();
            if (requestedIds.contains(questionId)) {
                latestByQuestion.putIfAbsent(questionId, attempt);
            }
        }

        long answered = latestByQuestion.size();
        long correct = latestByQuestion.values().stream().filter(QuestionAnswerAttempt::isCorrect).count();
        long incorrect = answered - correct;
        PracticeSessionSummary summary = new PracticeSessionSummary(
                requestedIds.size(),
                answered,
                Math.max(0, requestedIds.size() - answered),
                correct,
                incorrect,
                percentage(correct, answered)
        );

        Map<String, SessionCategoryAccumulator> categoryMap = new LinkedHashMap<>();
        for (QuestionAnswerAttempt attempt : latestByQuestion.values()) {
            List<com.example.pmp.category.Category> knowledgeCategories = attempt.getQuestion().getCategories().stream()
                    .filter(com.example.pmp.category.Category::isActive)
                    .filter(category -> !isFormatOnlyCategory(category.getCode()))
                    .toList();
            if (knowledgeCategories.isEmpty()) {
                categoryMap.computeIfAbsent("UNCATEGORIZED",
                                ignored -> new SessionCategoryAccumulator("UNCATEGORIZED", "Chưa phân loại"))
                        .accept(attempt);
            } else {
                knowledgeCategories.forEach(category -> categoryMap
                        .computeIfAbsent(category.getCode(),
                                ignored -> new SessionCategoryAccumulator(category.getCode(), category.getName()))
                        .accept(attempt));
            }
        }

        List<PracticeSessionCategoryResult> categoryResults = categoryMap.values().stream()
                .map(SessionCategoryAccumulator::toRecord)
                .sorted(Comparator.comparingLong(PracticeSessionCategoryResult::incorrectAnswers).reversed()
                        .thenComparingDouble(PracticeSessionCategoryResult::accuracyPercentage)
                        .thenComparing(PracticeSessionCategoryResult::categoryName))
                .toList();

        return new PracticeSessionReportResponse(
                Instant.now(),
                sessionId,
                summary,
                categoryResults,
                recommendationService.suggestionsFor(categoryResults)
        );
    }

    @Transactional(readOnly = true)
    public WrongQuestionReviewResponse wrongQuestions(String categoryCode, int minIncorrect, int count, boolean shuffle) {
        List<QuestionAnswerAttempt> all = attemptRepository.findAllByOrderByAnsweredAtDescIdDesc();
        Map<Long, QuestionAnswerAttempt> latestByQuestion = new LinkedHashMap<>();
        Map<Long, Long> incorrectCounts = new HashMap<>();
        for (QuestionAnswerAttempt attempt : all) {
            Long questionId = attempt.getQuestion().getId();
            latestByQuestion.putIfAbsent(questionId, attempt);
            if (!attempt.isCorrect()) incorrectCounts.merge(questionId, 1L, Long::sum);
        }

        List<Long> ids = latestByQuestion.entrySet().stream()
                .filter(entry -> !entry.getValue().isCorrect())
                .filter(entry -> incorrectCounts.getOrDefault(entry.getKey(), 0L) >= Math.max(1, minIncorrect))
                .map(Map.Entry::getKey)
                .toList();

        List<Question> questions = new ArrayList<>(questionRepository.findAllById(ids));
        if (categoryCode != null && !categoryCode.isBlank()) {
            String normalized = categoryCode.trim();
            questions.removeIf(question -> question.getCategories().stream()
                    .noneMatch(category -> category.getCode().equalsIgnoreCase(normalized)));
        }
        long total = questions.size();
        if (shuffle) Collections.shuffle(questions);
        int limit = Math.min(Math.max(count, 1), 100);
        List<QuestionResponse> result = questions.stream().limit(limit).map(QuestionResponse::from).toList();
        return new WrongQuestionReviewResponse(total, result);
    }

    private ZoneId resolveZoneId(String requestedTimeZone) {
        if (requestedTimeZone == null || requestedTimeZone.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(requestedTimeZone.trim());
        } catch (DateTimeException ex) {
            return ZoneOffset.UTC;
        }
    }

    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : round2(numerator * 100.0 / denominator);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String progressStatus(long currentTotal, long previousTotal, double improvement) {
        if (currentTotal == 0) return "NO_DATA";
        if (previousTotal == 0) return "NEW_BASELINE";
        if (improvement >= 3.0) return "IMPROVING";
        if (improvement <= -3.0) return "DECLINING";
        return "STABLE";
    }

    private String extractQuestionNumber(Question question) {
        Pattern pattern = Pattern.compile("(?:-|#|question\\s*)\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
        for (String source : List.of(
                question.getExamName() == null ? "" : question.getExamName(),
                question.getExternalId() == null ? "" : question.getExternalId())) {
            Matcher matcher = pattern.matcher(source.trim());
            if (matcher.find()) {
                try {
                    return String.valueOf(Long.parseLong(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    return matcher.group(1);
                }
            }
        }
        return "Không xác định";
    }

    private boolean isFormatOnlyCategory(String code) {
        return "TOPIC_LONG_QUESTION".equalsIgnoreCase(code)
                || "TOPIC_CHART".equalsIgnoreCase(code);
    }

    private final class SessionCategoryAccumulator {
        private final String code;
        private final String name;
        private long answered;
        private long correct;
        private long incorrect;
        private final Set<Long> wrongQuestionIds = new LinkedHashSet<>();

        private SessionCategoryAccumulator(String code, String name) {
            this.code = code;
            this.name = name;
        }

        private void accept(QuestionAnswerAttempt attempt) {
            answered++;
            if (attempt.isCorrect()) {
                correct++;
            } else {
                incorrect++;
                wrongQuestionIds.add(attempt.getQuestion().getId());
            }
        }

        private PracticeSessionCategoryResult toRecord() {
            return new PracticeSessionCategoryResult(
                    code,
                    name,
                    answered,
                    correct,
                    incorrect,
                    percentage(correct, answered),
                    List.copyOf(wrongQuestionIds)
            );
        }
    }

    private final class CategoryAccumulator {
        private final String code;
        private final String name;
        private long total;
        private long correct;
        private long incorrect;
        private final Set<Long> uniqueWrongQuestions = new HashSet<>();

        private CategoryAccumulator(String code, String name) {
            this.code = code;
            this.name = name;
        }

        private void accept(QuestionAnswerAttempt attempt) {
            total++;
            if (attempt.isCorrect()) {
                correct++;
            } else {
                incorrect++;
                uniqueWrongQuestions.add(attempt.getQuestion().getId());
            }
        }

        private CategoryMistakeStatistic toRecord() {
            return new CategoryMistakeStatistic(
                    code,
                    name,
                    total,
                    correct,
                    incorrect,
                    uniqueWrongQuestions.size(),
                    percentage(correct, total),
                    percentage(incorrect, total)
            );
        }
    }

    private final class DailyAccumulator {
        private final LocalDate date;
        private long total;
        private long correct;

        private DailyAccumulator(LocalDate date) {
            this.date = date;
        }

        private void accept(QuestionAnswerAttempt attempt) {
            total++;
            if (attempt.isCorrect()) correct++;
        }

        private DailyAccuracyStatistic toRecord() {
            return new DailyAccuracyStatistic(date, total, correct, total - correct, percentage(correct, total));
        }
    }

    private final class QuestionAccumulator {
        private final Question question;
        private long total;
        private long correct;
        private long incorrect;
        private Boolean lastAnswerCorrect;
        private Instant lastAnsweredAt;

        private QuestionAccumulator(Question question) {
            this.question = question;
        }

        private void accept(QuestionAnswerAttempt attempt) {
            total++;
            if (attempt.isCorrect()) correct++; else incorrect++;
            if (lastAnsweredAt == null || attempt.getAnsweredAt().isAfter(lastAnsweredAt)) {
                lastAnsweredAt = attempt.getAnsweredAt();
                lastAnswerCorrect = attempt.isCorrect();
            }
        }

        private TopWrongQuestionStatistic toRecord() {
            List<String> categoryNames = question.getCategories().stream()
                    .map(com.example.pmp.category.Category::getName)
                    .sorted()
                    .toList();
            return new TopWrongQuestionStatistic(
                    question.getId(),
                    question.getExamName(),
                    extractQuestionNumber(question),
                    question.getQuestionText(),
                    categoryNames,
                    total,
                    correct,
                    incorrect,
                    lastAnswerCorrect,
                    lastAnsweredAt
            );
        }
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
