package com.example.pmp.question;

public record StudyHighlight(
        String id,
        StudyHighlightTarget target,
        String targetKey,
        int startOffset,
        int endOffset,
        String text,
        StudyHighlightColor color
) {}
