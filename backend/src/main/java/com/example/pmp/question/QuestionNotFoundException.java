package com.example.pmp.question;

public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(Long id) {
        super("Không tìm thấy câu hỏi có ID " + id + ".");
    }
}
