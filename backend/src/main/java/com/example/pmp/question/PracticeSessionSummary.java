package com.example.pmp.question;

public record PracticeSessionSummary(
        long totalQuestions,
        long answeredQuestions,
        long skippedQuestions,
        long correctAnswers,
        long incorrectAnswers,
        double accuracyPercentage
) {}
