package com.example.pmp.question;

public record ExplanationReviewRequest(
        String pmaExplanation,
        String aiExplanation,
        String finalExplanation,
        FinalExplanationSource finalExplanationSource,
        ExplanationReviewStatus explanationReviewStatus,
        String explanationReviewNotes
) {}
