package com.example.pmp.question;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record AnswerAttemptResponse(
        Long id,
        Long questionId,
        QuestionType questionType,
        Set<String> selectedAnswers,
        Map<String, String> matchingAnswers,
        boolean correct,
        Set<String> correctAnswers,
        Map<String, String> correctMatchingAnswers,
        String sessionId,
        Instant answeredAt,
        AnswerHistorySummary summary
) {
}
