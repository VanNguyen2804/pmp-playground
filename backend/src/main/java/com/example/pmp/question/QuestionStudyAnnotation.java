package com.example.pmp.question;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "question_study_annotations", indexes = {
        @Index(name = "idx_study_annotation_question", columnList = "question_id", unique = true)
})
public class QuestionStudyAnnotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_study_annotation_question"))
    private Question question;

    @Column(name = "note_text", columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "highlights_json", nullable = false, columnDefinition = "TEXT")
    private String highlightsJson = "[]";

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (highlightsJson == null || highlightsJson.isBlank()) highlightsJson = "[]";
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
    public String getHighlightsJson() { return highlightsJson; }
    public void setHighlightsJson(String highlightsJson) { this.highlightsJson = highlightsJson; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
