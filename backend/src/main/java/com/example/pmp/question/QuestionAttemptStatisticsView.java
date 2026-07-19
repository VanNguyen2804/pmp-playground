package com.example.pmp.question;

public interface QuestionAttemptStatisticsView {
    Long getQuestionId();
    Long getTotalAttempts();
    Long getCorrectAttempts();
    Long getIncorrectAttempts();
}
