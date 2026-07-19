package com.example.pmp.question;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionStudyAnnotationRepository extends JpaRepository<QuestionStudyAnnotation, Long> {
    Optional<QuestionStudyAnnotation> findByQuestion_Id(Long questionId);
    List<QuestionStudyAnnotation> findAllByQuestion_IdIn(Collection<Long> questionIds);
}
