package com.example.pmp.question;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
        @NotBlank String key,
        @NotBlank String text
) {}
