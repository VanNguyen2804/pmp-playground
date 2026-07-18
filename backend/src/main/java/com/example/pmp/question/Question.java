package com.example.pmp.question;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String questionText;

    @Lob
    @Column(nullable = false)
    private String optionA;

    @Lob
    @Column(nullable = false)
    private String optionB;

    @Lob
    @Column(nullable = false)
    private String optionC;

    @Lob
    @Column(nullable = false)
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private CorrectOption correctOption;

    @Lob
    private String explanation;

    private String category;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private String source;
    private String reference;
    private String tags;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public CorrectOption getCorrectOption() { return correctOption; }
    public void setCorrectOption(CorrectOption correctOption) { this.correctOption = correctOption; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
