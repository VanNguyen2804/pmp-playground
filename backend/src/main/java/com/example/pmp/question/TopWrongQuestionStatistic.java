package com.example.pmp.question;

import java.time.Instant;
import java.util.List;

public record TopWrongQuestionStatistic(
        Long questionId,
        String examName,
        String questionNumber,
        String questionText,
        List<String> categoryNames,
        long totalAttempts,
        long correctAttempts,
        long incorrectAttempts,
        Boolean lastAnswerCorrect,
        Instant lastAnsweredAt
) {
}
