package com.example.pmp.question;

import java.util.List;

public record ImportResult(int totalRows, int importedRows, int skippedRows, List<String> errors) {}
