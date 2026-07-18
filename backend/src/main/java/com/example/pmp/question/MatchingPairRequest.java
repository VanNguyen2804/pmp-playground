package com.example.pmp.question;

import jakarta.validation.constraints.NotBlank;

public record MatchingPairRequest(
        @NotBlank String left,
        @NotBlank String right
) {}
