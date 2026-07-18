package com.example.pmp.question;

import com.example.pmp.category.Taxonomy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByExternalId(String externalId);
    long countByCategories_Id(Long categoryId);

    @Query("""
        select distinct q from Question q
        left join q.categories c
        where (:search is null or lower(q.questionText) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.pmaExplanation, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.aiExplanation, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.finalExplanation, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.explanationReviewNotes, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.tags, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.examName, '')) like lower(concat('%', :search, '%')))
          and (:categoryCode is null or lower(c.code) = lower(:categoryCode))
          and (:taxonomy is null or c.taxonomy = :taxonomy)
          and (:difficulty is null or q.difficulty = :difficulty)
          and (:questionType is null or q.questionType = :questionType)
          and (:reviewStatus is null or q.explanationReviewStatus = :reviewStatus
            or (:includeUninitializedPending = true and q.explanationReviewStatus is null))
        """)
    Page<Question> search(
            @Param("search") String search,
            @Param("categoryCode") String categoryCode,
            @Param("taxonomy") Taxonomy taxonomy,
            @Param("difficulty") Difficulty difficulty,
            @Param("questionType") QuestionType questionType,
            @Param("reviewStatus") ExplanationReviewStatus reviewStatus,
            @Param("includeUninitializedPending") boolean includeUninitializedPending,
            Pageable pageable
    );
}
