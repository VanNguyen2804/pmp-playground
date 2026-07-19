package com.example.pmp.question;

public record CategoryMistakeStatistic(
        String categoryCode,
        String categoryName,
        long totalAttempts,
        long correctAttempts,
        long incorrectAttempts,
        long uniqueWrongQuestions,
        double accuracyPercentage,
        double incorrectPercentage
) {
}
