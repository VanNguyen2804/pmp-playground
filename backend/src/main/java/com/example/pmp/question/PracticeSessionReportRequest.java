package com.example.pmp.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PracticeSessionReportRequest(
        @NotBlank @Size(max = 80) String sessionId,
        @NotEmpty @Size(max = 100) List<@NotNull Long> questionIds
) {}
