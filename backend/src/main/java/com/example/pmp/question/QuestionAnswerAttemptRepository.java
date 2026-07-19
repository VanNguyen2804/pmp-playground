package com.example.pmp.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"question", "question.categories"})
    List<QuestionAnswerAttempt> findBySessionIdOrderByAnsweredAtDescIdDesc(String sessionId);

    @Query("""
            select a.question.id as questionId,
                   count(a.id) as totalAttempts,
                   sum(case when a.correct = true then 1 else 0 end) as correctAttempts,
                   sum(case when a.correct = false then 1 else 0 end) as incorrectAttempts
            from QuestionAnswerAttempt a
            where a.question.id in :questionIds
            group by a.question.id
            """)
    List<QuestionAttemptStatisticsView> summarizeByQuestionIds(@Param("questionIds") List<Long> questionIds);

    @Query("""
            select a
            from QuestionAnswerAttempt a
            where a.question.id in :questionIds
              and not exists (
                  select later.id
                  from QuestionAnswerAttempt later
                  where later.question.id = a.question.id
                    and (later.answeredAt > a.answeredAt
                         or (later.answeredAt = a.answeredAt and later.id > a.id))
              )
            """)
    List<QuestionAnswerAttempt> findLatestByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
