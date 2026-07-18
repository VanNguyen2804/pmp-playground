package com.example.pmp.question;

import com.example.pmp.category.Taxonomy;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record QuestionResponse(
        Long id,
        String externalId,
        String examName,
        QuestionType questionType,
        String questionText,
        String imageUrl,
        List<OptionResponse> options,
        Set<String> correctAnswers,
        List<MatchingPairResponse> matchingPairs,
        String pmaExplanation,
        String aiExplanation,
        String finalExplanation,
        FinalExplanationSource finalExplanationSource,
        ExplanationReviewStatus explanationReviewStatus,
        String explanationReviewNotes,
        Instant explanationReviewedAt,
        ExplanationStatus explanationStatus,
        String explanationType,
        Integer explanationPromptVersion,
        Integer numberOfAnswers,
        QuestionSource source,
        Difficulty difficulty,
        String reference,
        String tags,
        List<CategoryItem> categories,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionResponse from(Question q) {
        List<OptionResponse> optionResponses = q.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getOptionKey(), o.getOptionText(), o.isCorrect(), o.getDisplayOrder()))
                .toList();
        Set<String> answers = q.getOptions().stream().filter(QuestionOption::isCorrect)
                .map(QuestionOption::getOptionKey).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        List<MatchingPairResponse> pairs = q.getMatchingPairs().stream()
                .map(p -> new MatchingPairResponse(p.getId(), p.getLeftText(), p.getRightText(), p.getDisplayOrder()))
                .toList();
        List<CategoryItem> categories = q.getCategories().stream()
                .sorted(java.util.Comparator.comparing((com.example.pmp.category.Category x) -> x.getTaxonomy().name())
                        .thenComparingInt(com.example.pmp.category.Category::getDisplayOrder))
                .map(c -> new CategoryItem(c.getCode(), c.getName(), c.getTaxonomy()))
                .toList();
        return new QuestionResponse(q.getId(), q.getExternalId(), q.getExamName(), q.getQuestionType(),
                q.getQuestionText(), q.getImageUrl(), optionResponses, answers, pairs,
                q.getPmaExplanation(), q.getAiExplanation(), q.getFinalExplanation(),
                q.getFinalExplanationSource() == null ? FinalExplanationSource.NONE : q.getFinalExplanationSource(),
                q.getExplanationReviewStatus() == null ? ExplanationReviewStatus.PENDING : q.getExplanationReviewStatus(),
                q.getExplanationReviewNotes(), q.getExplanationReviewedAt(),
                q.getExplanationStatus(), q.getExplanationType(), q.getExplanationPromptVersion(),
                q.getNumberOfAnswers(), q.getSource(), q.getDifficulty(), q.getReference(), q.getTags(),
                categories, q.getCreatedAt(), q.getUpdatedAt());
    }

    public record OptionResponse(Long id, String key, String text, boolean correct, int displayOrder) {}
    public record MatchingPairResponse(Long id, String left, String right, int displayOrder) {}
    public record CategoryItem(String code, String name, Taxonomy taxonomy) {}
}
