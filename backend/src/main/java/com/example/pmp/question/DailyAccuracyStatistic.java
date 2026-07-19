package com.example.pmp.question;

import java.time.LocalDate;

public record DailyAccuracyStatistic(
        LocalDate date,
        long totalAttempts,
        long correctAttempts,
        long incorrectAttempts,
        double accuracyPercentage
) {
}
