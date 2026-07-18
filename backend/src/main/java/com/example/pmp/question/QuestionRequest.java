package com.example.pmp.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public record QuestionRequest(
        String externalId,
        String examName,
        @NotNull QuestionType questionType,
        @NotBlank String questionText,
        String imageUrl,
        @Valid List<OptionRequest> options,
        Set<String> correctAnswers,
        @Valid List<MatchingPairRequest> matchingPairs,
        String pmaExplanation,
        String aiExplanation,
        String finalExplanation,
        FinalExplanationSource finalExplanationSource,
        ExplanationReviewStatus explanationReviewStatus,
        String explanationReviewNotes,
        Difficulty difficulty,
        QuestionSource source,
        String reference,
        String tags,
        Set<String> categoryCodes
) {}
