package com.example.pmp.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionRequest(
        @NotBlank String questionText,
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String optionC,
        @NotBlank String optionD,
        @NotNull CorrectOption correctOption,
        String explanation,
        String category,
        Difficulty difficulty,
        String source,
        String reference,
        String tags
) {}
