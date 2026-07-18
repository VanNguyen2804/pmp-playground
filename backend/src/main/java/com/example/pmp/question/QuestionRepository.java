package com.example.pmp.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
        select q from Question q
        where (:search is null or lower(q.questionText) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.explanation, '')) like lower(concat('%', :search, '%'))
            or lower(coalesce(q.tags, '')) like lower(concat('%', :search, '%')))
          and (:category is null or lower(q.category) = lower(:category))
          and (:difficulty is null or q.difficulty = :difficulty)
        """)
    Page<Question> search(
            @Param("search") String search,
            @Param("category") String category,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable
    );
}
