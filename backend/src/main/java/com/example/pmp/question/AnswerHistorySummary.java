package com.example.pmp.question;

import java.time.Instant;

public record AnswerHistorySummary(
        Long questionId,
        long totalAttempts,
        long correctAttempts,
        long incorrectAttempts,
        double accuracyPercentage,
        Instant lastAnsweredAt,
        Boolean lastAnswerCorrect
) {
}
