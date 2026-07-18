package com.example.pmp.question;

import com.example.pmp.category.Taxonomy;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService service;

    public QuestionController(QuestionService service) { this.service = service; }

    @GetMapping
    public Page<QuestionResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Taxonomy taxonomy,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) ExplanationReviewStatus reviewStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, categoryCode, taxonomy, difficulty, questionType, reviewStatus, page, size);
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) { return service.update(id, request); }

    @PutMapping("/{id}/explanations")
    public QuestionResponse updateExplanations(@PathVariable Long id,
                                               @RequestBody ExplanationReviewRequest request) {
        return service.updateExplanationReview(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/random")
    public List<QuestionResponse> random(@RequestParam(defaultValue = "10") int count,
                                         @RequestParam(required = false) String categoryCode,
                                         @RequestParam(required = false) QuestionType questionType) {
        return service.random(count, categoryCode, questionType);
    }

    @PostMapping(value = "/import/csv", consumes = "multipart/form-data")
    public ImportResult importCsv(@RequestPart("file") MultipartFile file) { return service.importCsv(file); }

    @PostMapping(value = "/import/json", consumes = "multipart/form-data")
    public ImportResult importJson(@RequestPart("file") MultipartFile file) { return service.importJson(file); }

    @PostMapping(value = "/import/pma-exam", consumes = "multipart/form-data")
    public ImportResult importPmaExam(@RequestPart("file") MultipartFile file) { return service.importPmaExam(file); }
}
