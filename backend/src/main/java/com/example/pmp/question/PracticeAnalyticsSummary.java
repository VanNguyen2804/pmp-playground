package com.example.pmp.question;

public record PracticeAnalyticsSummary(
        long totalAttempts,
        long correctAttempts,
        long incorrectAttempts,
        double accuracyPercentage,
        long previousTotalAttempts,
        double previousAccuracyPercentage,
        double improvementPercentagePoints,
        String progressStatus,
        String mostWrongCategoryCode,
        String mostWrongCategoryName,
        long mostWrongCategoryIncorrectAttempts
) {
}
