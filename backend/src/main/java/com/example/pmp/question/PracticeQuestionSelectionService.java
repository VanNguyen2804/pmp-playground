package com.example.pmp.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
public class PracticeQuestionSelectionService {
    private static final long MASTERED_CORRECT_THRESHOLD = 5L;

    private final QuestionRepository questionRepository;
    private final QuestionAnswerAttemptRepository attemptRepository;

    public PracticeQuestionSelectionService(QuestionRepository questionRepository,
                                            QuestionAnswerAttemptRepository attemptRepository) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<QuestionResponse> selectPriorityQuestions(int requestedCount,
                                                          String categoryCode,
                                                          QuestionType questionType) {
        int count = Math.min(Math.max(requestedCount, 1), 100);
        var specification = QuestionSpecifications.filters(
                null,
                normalize(categoryCode),
                null,
                null,
                questionType,
                null
        );

        List<Question> eligibleQuestions = new ArrayList<>(questionRepository.findAll(specification));
        if (eligibleQuestions.isEmpty()) return List.of();

        List<Long> questionIds = eligibleQuestions.stream().map(Question::getId).toList();
        Map<Long, PracticeAttemptStats> statsByQuestion = loadStats(questionIds);

        return eligibleQuestions.stream()
                .map(question -> new WeightedQuestion(
                        question,
                        weightedRandomKey(weightFor(statsByQuestion.getOrDefault(
                                question.getId(), PracticeAttemptStats.unanswered())))))
                .sorted(Comparator.comparingDouble(WeightedQuestion::key))
                .limit(count)
                .map(WeightedQuestion::question)
                .map(QuestionResponse::from)
                .toList();
    }

    private Map<Long, PracticeAttemptStats> loadStats(List<Long> questionIds) {
        Map<Long, MutablePracticeAttemptStats> mutable = new HashMap<>();

        for (QuestionAttemptStatisticsView row : attemptRepository.summarizeByQuestionIds(questionIds)) {
            mutable.put(row.getQuestionId(), new MutablePracticeAttemptStats(
                    safe(row.getTotalAttempts()),
                    safe(row.getCorrectAttempts()),
                    safe(row.getIncorrectAttempts()),
                    null
            ));
        }

        for (QuestionAnswerAttempt latest : attemptRepository.findLatestByQuestionIds(questionIds)) {
            Long questionId = latest.getQuestion().getId();
            MutablePracticeAttemptStats value = mutable.get(questionId);
            if (value != null) value.latestCorrect = latest.isCorrect();
        }

        Map<Long, PracticeAttemptStats> result = new HashMap<>();
        mutable.forEach((questionId, value) -> result.put(questionId, value.toImmutable()));
        return result;
    }

    static double weightFor(PracticeAttemptStats stats) {
        if (stats.totalAttempts() == 0) {
            // Questions never attempted should appear frequently so the bank is covered.
            return 12.0;
        }

        if (Boolean.FALSE.equals(stats.latestCorrect())) {
            // A currently-wrong question is the highest priority. Repeated mistakes increase it further.
            return 16.0 + Math.min(stats.incorrectAttempts(), 8L) * 1.5;
        }

        if (stats.correctAttempts() >= MASTERED_CORRECT_THRESHOLD) {
            // Mastered questions remain possible, but occur much less often.
            return 0.75;
        }

        // Partially learned questions remain in normal rotation. Historical mistakes add some priority.
        return 5.0
                + Math.min(stats.incorrectAttempts(), 4L) * 0.75
                + Math.max(0L, MASTERED_CORRECT_THRESHOLD - stats.correctAttempts()) * 0.5;
    }

    private double weightedRandomKey(double weight) {
        double random = Math.max(ThreadLocalRandom.current().nextDouble(), 1.0e-12);
        return -Math.log(random) / Math.max(weight, 0.01);
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record PracticeAttemptStats(long totalAttempts,
                                long correctAttempts,
                                long incorrectAttempts,
                                Boolean latestCorrect) {
        static PracticeAttemptStats unanswered() {
            return new PracticeAttemptStats(0, 0, 0, null);
        }
    }

    private record WeightedQuestion(Question question, double key) {}

    private static final class MutablePracticeAttemptStats {
        private final long totalAttempts;
        private final long correctAttempts;
        private final long incorrectAttempts;
        private Boolean latestCorrect;

        private MutablePracticeAttemptStats(long totalAttempts,
                                            long correctAttempts,
                                            long incorrectAttempts,
                                            Boolean latestCorrect) {
            this.totalAttempts = totalAttempts;
            this.correctAttempts = correctAttempts;
            this.incorrectAttempts = incorrectAttempts;
            this.latestCorrect = latestCorrect;
        }

        private PracticeAttemptStats toImmutable() {
            return new PracticeAttemptStats(totalAttempts, correctAttempts, incorrectAttempts, latestCorrect);
        }
    }
}
