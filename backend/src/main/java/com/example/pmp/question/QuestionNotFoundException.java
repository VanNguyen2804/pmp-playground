package com.example.pmp.question;

public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(Long id) {
        super("Question not found: " + id);
    }
}
