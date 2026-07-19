package com.example.pmp.question;

import java.time.Instant;
import java.util.List;

public record StudyAnnotationResponse(
        Long questionId,
        String note,
        List<StudyHighlight> highlights,
        long version,
        Instant updatedAt
) {}
