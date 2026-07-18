package com.example.pmp.question;

import jakarta.persistence.*;

@Entity
@Table(name = "matching_pairs")
public class MatchingPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String leftText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rightText;

    @Column(nullable = false)
    private int displayOrder;

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public String getLeftText() { return leftText; }
    public void setLeftText(String leftText) { this.leftText = leftText; }
    public String getRightText() { return rightText; }
    public void setRightText(String rightText) { this.rightText = rightText; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
