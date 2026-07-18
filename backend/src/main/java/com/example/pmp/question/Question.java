package com.example.pmp.question;

import com.example.pmp.category.Category;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pmp_questions", indexes = {
        @Index(name = "idx_question_external_id", columnList = "external_id"),
        @Index(name = "idx_question_type", columnList = "question_type"),
        @Index(name = "idx_question_source", columnList = "source")
})
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, length = 80)
    private String externalId;

    @Column(name = "exam_name", length = 180)
    private String examName;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType = QuestionType.MCQ;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String pmaExplanation;

    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(columnDefinition = "TEXT")
    private String finalExplanation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FinalExplanationSource finalExplanationSource = FinalExplanationSource.NONE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExplanationReviewStatus explanationReviewStatus = ExplanationReviewStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String explanationReviewNotes;

    private Instant explanationReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExplanationStatus explanationStatus = ExplanationStatus.NOT_PROVIDED;

    @Column(length = 40)
    private String explanationType;

    private Integer explanationPromptVersion;

    private Integer numberOfAnswers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource source = QuestionSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String rawSourceJson;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<MatchingPair> matchingPairs = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "question_categories",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_question_category", columnNames = {"question_id", "category_id"}))
    private Set<Category> categories = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public void replaceOptions(List<QuestionOption> newOptions) {
        options.clear();
        newOptions.forEach(option -> { option.setQuestion(this); options.add(option); });
    }

    public void replaceMatchingPairs(List<MatchingPair> newPairs) {
        matchingPairs.clear();
        newPairs.forEach(pair -> { pair.setQuestion(this); matchingPairs.add(pair); });
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPmaExplanation() { return pmaExplanation; }
    public void setPmaExplanation(String pmaExplanation) { this.pmaExplanation = pmaExplanation; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public String getFinalExplanation() { return finalExplanation; }
    public void setFinalExplanation(String finalExplanation) { this.finalExplanation = finalExplanation; }
    public FinalExplanationSource getFinalExplanationSource() { return finalExplanationSource; }
    public void setFinalExplanationSource(FinalExplanationSource finalExplanationSource) { this.finalExplanationSource = finalExplanationSource; }
    public ExplanationReviewStatus getExplanationReviewStatus() { return explanationReviewStatus; }
    public void setExplanationReviewStatus(ExplanationReviewStatus explanationReviewStatus) { this.explanationReviewStatus = explanationReviewStatus; }
    public String getExplanationReviewNotes() { return explanationReviewNotes; }
    public void setExplanationReviewNotes(String explanationReviewNotes) { this.explanationReviewNotes = explanationReviewNotes; }
    public Instant getExplanationReviewedAt() { return explanationReviewedAt; }
    public void setExplanationReviewedAt(Instant explanationReviewedAt) { this.explanationReviewedAt = explanationReviewedAt; }
    public ExplanationStatus getExplanationStatus() { return explanationStatus; }
    public void setExplanationStatus(ExplanationStatus explanationStatus) { this.explanationStatus = explanationStatus; }
    public String getExplanationType() { return explanationType; }
    public void setExplanationType(String explanationType) { this.explanationType = explanationType; }
    public Integer getExplanationPromptVersion() { return explanationPromptVersion; }
    public void setExplanationPromptVersion(Integer explanationPromptVersion) { this.explanationPromptVersion = explanationPromptVersion; }
    public Integer getNumberOfAnswers() { return numberOfAnswers; }
    public void setNumberOfAnswers(Integer numberOfAnswers) { this.numberOfAnswers = numberOfAnswers; }
    public QuestionSource getSource() { return source; }
    public void setSource(QuestionSource source) { this.source = source; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getRawSourceJson() { return rawSourceJson; }
    public void setRawSourceJson(String rawSourceJson) { this.rawSourceJson = rawSourceJson; }
    public List<QuestionOption> getOptions() { return options; }
    public List<MatchingPair> getMatchingPairs() { return matchingPairs; }
    public Set<Category> getCategories() { return categories; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
