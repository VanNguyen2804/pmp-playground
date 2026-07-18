package com.example.pmp.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class QuestionService {
    private final QuestionRepository repository;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> list(String search, String category, Difficulty difficulty, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return repository.search(blankToNull(search), blankToNull(category), difficulty, pageable)
                .map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(Long id) {
        return QuestionResponse.from(find(id));
    }

    public QuestionResponse create(QuestionRequest request) {
        Question question = new Question();
        apply(question, request);
        return QuestionResponse.from(repository.save(question));
    }

    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = find(id);
        apply(question, request);
        return QuestionResponse.from(repository.save(question));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> random(int count, String category, Difficulty difficulty) {
        List<Question> all = new ArrayList<>(repository.search(null, blankToNull(category), difficulty,
                Pageable.unpaged()).getContent());
        Collections.shuffle(all);
        return all.stream().limit(Math.min(Math.max(count, 1), 100)).map(QuestionResponse::from).toList();
    }

    public ImportResult importCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int total = 0;
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .get()
                    .parse(reader);

            for (CSVRecord record : records) {
                total++;
                try {
                    QuestionRequest request = fromCsv(record);
                    create(request);
                    imported++;
                } catch (Exception ex) {
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read CSV file: " + ex.getMessage(), ex);
        }
        return new ImportResult(total, imported, total - imported, errors);
    }

    public ImportResult importJson(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        List<QuestionRequest> requests;
        try {
            requests = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read JSON file: " + ex.getMessage(), ex);
        }
        for (int i = 0; i < requests.size(); i++) {
            try {
                create(requests.get(i));
                imported++;
            } catch (Exception ex) {
                errors.add("Item " + (i + 1) + ": " + ex.getMessage());
            }
        }
        return new ImportResult(requests.size(), imported, requests.size() - imported, errors);
    }

    private QuestionRequest fromCsv(CSVRecord r) {
        return new QuestionRequest(
                required(r, "questionText"),
                required(r, "optionA"),
                required(r, "optionB"),
                required(r, "optionC"),
                required(r, "optionD"),
                CorrectOption.valueOf(required(r, "correctOption").trim().toUpperCase(Locale.ROOT)),
                optional(r, "explanation"),
                optional(r, "category"),
                parseDifficulty(optional(r, "difficulty")),
                optional(r, "source"),
                optional(r, "reference"),
                optional(r, "tags")
        );
    }

    private String required(CSVRecord r, String name) {
        String value = optional(r, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private String optional(CSVRecord r, String name) {
        try {
            String value = r.get(name);
            return blankToNull(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Difficulty parseDifficulty(String value) {
        return value == null ? null : Difficulty.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private void apply(Question q, QuestionRequest r) {
        q.setQuestionText(r.questionText().trim());
        q.setOptionA(r.optionA().trim());
        q.setOptionB(r.optionB().trim());
        q.setOptionC(r.optionC().trim());
        q.setOptionD(r.optionD().trim());
        q.setCorrectOption(r.correctOption());
        q.setExplanation(blankToNull(r.explanation()));
        q.setCategory(blankToNull(r.category()));
        q.setDifficulty(r.difficulty());
        q.setSource(blankToNull(r.source()));
        q.setReference(blankToNull(r.reference()));
        q.setTags(blankToNull(r.tags()));
    }

    private Question find(Long id) {
        return repository.findById(id).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
