package com.example.pmp.question;

import java.util.List;

public record WrongQuestionReviewResponse(
        long totalQuestions,
        List<QuestionResponse> questions
) {}
