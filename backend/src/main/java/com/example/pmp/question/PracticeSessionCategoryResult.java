package com.example.pmp.question;

import java.util.List;

public record PracticeSessionCategoryResult(
        String categoryCode,
        String categoryName,
        long answeredQuestions,
        long correctAnswers,
        long incorrectAnswers,
        double accuracyPercentage,
        List<Long> wrongQuestionIds
) {}
