package com.example.pmp.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuestionAnswerAttemptRepository extends JpaRepository<QuestionAnswerAttempt, Long> {
    Page<QuestionAnswerAttempt> findByQuestion_IdOrderByAnsweredAtDescIdDesc(Long questionId, Pageable pageable);
    long countByQuestion_Id(Long questionId);
    long countByQuestion_IdAndCorrectTrue(Long questionId);
    Optional<QuestionAnswerAttempt> findTopByQuestion_IdOrderByAnsweredAtDescIdDesc(Long questionId);
    List<QuestionAnswerAttempt> findAllByOrderByAnsweredAtDescIdDesc();
    List<QuestionAnswerAttempt> findByAnsweredAtGreaterThanEqualOrderByAnsweredAtDescIdDesc(Instant from);
}
