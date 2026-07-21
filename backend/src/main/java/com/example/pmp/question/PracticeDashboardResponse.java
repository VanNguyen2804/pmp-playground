package com.example.pmp.question;

import java.util.List;

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
        double averageDailyAccuracyPercentage,
        long activePerformanceDays,
        List<DailyAccuracyStatistic> dailyPerformanceHistory,
        long answeredQuestions,
        long totalQuestions,
        double questionBankCoveragePercentage,
        long currentCorrectStreak,
        long dueToday,
        String timeZone
) {}
