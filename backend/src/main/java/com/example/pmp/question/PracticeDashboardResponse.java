package com.example.pmp.question;

public record PracticeDashboardResponse(
        long wrongQuestions,
        double todayAccuracyPercentage,
        long todayCorrectAttempts,
        long todayTotalAttempts,
        double yesterdayAccuracyPercentage,
        long yesterdayCorrectAttempts,
        long yesterdayTotalAttempts,
        double dailyAccuracyDeltaPercentagePoints,
        String dailyPerformanceStatus,
        long answeredQuestions,
        long totalQuestions,
        double questionBankCoveragePercentage,
        long currentCorrectStreak,
        long dueToday,
        String timeZone
) {}
