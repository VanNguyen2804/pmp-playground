package com.example.pmp.question;

import java.time.Instant;
import java.util.List;

public record PracticeAnalyticsResponse(
        Instant generatedAt,
        int periodDays,
        String timeZone,
        PracticeAnalyticsSummary summary,
        List<CategoryMistakeStatistic> categoryStats,
        List<DailyAccuracyStatistic> dailyTrend,
        List<TopWrongQuestionStatistic> topWrongQuestions
) {
}
