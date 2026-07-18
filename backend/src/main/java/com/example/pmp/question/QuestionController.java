package com.example.pmp.question;

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

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<QuestionResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(search, category, difficulty, page, size);
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/random")
    public List<QuestionResponse> random(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Difficulty difficulty
    ) {
        return service.random(count, category, difficulty);
    }

    @PostMapping(value = "/import/csv", consumes = "multipart/form-data")
    public ImportResult importCsv(@RequestPart("file") MultipartFile file) {
        return service.importCsv(file);
    }

    @PostMapping(value = "/import/json", consumes = "multipart/form-data")
    public ImportResult importJson(@RequestPart("file") MultipartFile file) {
        return service.importJson(file);
    }
}
