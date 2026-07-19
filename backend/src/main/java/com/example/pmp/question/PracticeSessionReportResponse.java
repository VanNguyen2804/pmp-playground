package com.example.pmp.question;

import java.time.Instant;
import java.util.List;

public record PracticeSessionReportResponse(
        Instant generatedAt,
        String sessionId,
        PracticeSessionSummary summary,
        List<PracticeSessionCategoryResult> categoryResults,
        List<CategoryStudySuggestion> suggestions
) {}
