package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.Taxonomy;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QuestionSpecifications {
    private QuestionSpecifications() {
    }

    static Specification<Question> filters(
            String search,
            String categoryCode,
            Taxonomy taxonomy,
            Difficulty difficulty,
            QuestionType questionType,
            ExplanationReviewStatus reviewStatus
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        containsIgnoreCase(criteriaBuilder, root.<String>get("questionText"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("pmaExplanation"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("aiExplanation"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("finalExplanation"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("explanationReviewNotes"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("tags"), pattern),
                        containsIgnoreCase(criteriaBuilder, root.<String>get("examName"), pattern)
                ));
            }

            if (categoryCode != null || taxonomy != null) {
                Join<Question, Category> category = root.join("categories", JoinType.INNER);
                query.distinct(true);

                if (categoryCode != null) {
                    predicates.add(criteriaBuilder.equal(
                            criteriaBuilder.lower(category.<String>get("code")),
                            categoryCode.toLowerCase(Locale.ROOT)
                    ));
                }
                if (taxonomy != null) {
                    predicates.add(criteriaBuilder.equal(category.get("taxonomy"), taxonomy));
                }
            }

            if (difficulty != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }
            if (questionType != null) {
                predicates.add(criteriaBuilder.equal(root.get("questionType"), questionType));
            }
            if (reviewStatus != null) {
                Predicate selectedStatus = criteriaBuilder.equal(root.get("explanationReviewStatus"), reviewStatus);
                if (reviewStatus == ExplanationReviewStatus.PENDING) {
                    predicates.add(criteriaBuilder.or(
                            selectedStatus,
                            criteriaBuilder.isNull(root.get("explanationReviewStatus"))
                    ));
                } else {
                    predicates.add(selectedStatus);
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate containsIgnoreCase(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Expression<String> field,
            String pattern
    ) {
        return criteriaBuilder.like(
                criteriaBuilder.lower(criteriaBuilder.coalesce(field, "")),
                pattern
        );
    }
}
