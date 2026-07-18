package com.example.pmp.question;

import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.Set;

public record AnswerAttemptRequest(
        Set<String> selectedAnswers,
        Map<String, String> matchingAnswers,
        @Size(max = 80, message = "sessionId must not exceed 80 characters") String sessionId
) {
}
