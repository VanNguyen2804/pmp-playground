package com.example.pmp.category;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        Taxonomy taxonomy,
        int displayOrder,
        long questionCount
) {}
