package com.example.pmp.question;

import java.util.List;

public record StudyAnnotationRequest(
        String note,
        List<StudyHighlight> highlights
) {}
