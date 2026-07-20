package com.example.pmp.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    Optional<Question> findByExternalId(String externalId);
    Optional<Question> findByExamNameIgnoreCase(String examName);
    long countByCategories_Id(Long categoryId);
}
