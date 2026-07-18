package com.example.pmp.question;

import java.util.List;

public class QuestionImportException extends RuntimeException {
    private final List<String> details;

    public QuestionImportException(String message) {
        this(message, List.of(), null);
    }

    public QuestionImportException(String message, Throwable cause) {
        this(message, List.of(), cause);
    }

    public QuestionImportException(String message, List<String> details) {
        this(message, details, null);
    }

    private QuestionImportException(String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
