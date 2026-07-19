package com.example.pmp.question;

import java.util.List;

public record CategoryStudySuggestion(
        String categoryCode,
        String categoryName,
        String priority,
        String pmbokReference,
        List<String> focusAreas,
        String decisionRule,
        String recommendedPractice
) {}
