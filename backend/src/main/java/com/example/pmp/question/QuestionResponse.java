package com.example.pmp.question;

import java.time.Instant;

public record QuestionResponse(
        Long id,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        CorrectOption correctOption,
        String explanation,
        String category,
        Difficulty difficulty,
        String source,
        String reference,
        String tags,
        Instant createdAt,
        Instant updatedAt
) {
    static QuestionResponse from(Question q) {
        return new QuestionResponse(
                q.getId(), q.getQuestionText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption(), q.getExplanation(), q.getCategory(), q.getDifficulty(), q.getSource(),
                q.getReference(), q.getTags(), q.getCreatedAt(), q.getUpdatedAt()
        );
    }
}
