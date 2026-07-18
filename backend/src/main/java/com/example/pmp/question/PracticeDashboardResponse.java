package com.example.pmp.question;

public record PracticeDashboardResponse(
        long wrongQuestions,
        double weeklyAccuracyPercentage,
        long weeklyCorrectAttempts,
        long weeklyTotalAttempts,
        long currentCorrectStreak,
        long dueToday
) {}
