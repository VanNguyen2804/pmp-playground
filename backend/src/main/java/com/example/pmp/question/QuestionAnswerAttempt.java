package com.example.pmp.question;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "question_answer_attempts", indexes = {
        @Index(name = "idx_answer_attempt_question", columnList = "question_id"),
        @Index(name = "idx_answer_attempt_question_time", columnList = "question_id, answered_at"),
        @Index(name = "idx_answer_attempt_correct", columnList = "correct")
})
public class QuestionAnswerAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_answer_attempt_question"))
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "submitted_answer_json", nullable = false, columnDefinition = "TEXT")
    private String submittedAnswerJson;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "session_id", length = 80)
    private String sessionId;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    @PrePersist
    void prePersist() {
        if (answeredAt == null) answeredAt = Instant.now();
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public String getSubmittedAnswerJson() { return submittedAnswerJson; }
    public void setSubmittedAnswerJson(String submittedAnswerJson) { this.submittedAnswerJson = submittedAnswerJson; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Instant getAnsweredAt() { return answeredAt; }
}
