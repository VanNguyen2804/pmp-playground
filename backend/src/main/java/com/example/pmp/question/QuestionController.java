package com.example.pmp.question;

import com.example.pmp.category.Taxonomy;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService service;
    private final QuestionCsvExportService csvExportService;
    private final QuestionAnswerHistoryService answerHistoryService;
    private final QuestionStudyAnnotationService studyAnnotationService;
    private final PracticeQuestionSelectionService practiceQuestionSelectionService;

    public QuestionController(QuestionService service,
                              QuestionCsvExportService csvExportService,
                              QuestionAnswerHistoryService answerHistoryService,
                              QuestionStudyAnnotationService studyAnnotationService,
                              PracticeQuestionSelectionService practiceQuestionSelectionService) {
        this.service = service;
        this.csvExportService = csvExportService;
        this.answerHistoryService = answerHistoryService;
        this.studyAnnotationService = studyAnnotationService;
        this.practiceQuestionSelectionService = practiceQuestionSelectionService;
    }

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

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv() {
        QuestionCsvExport export = csvExportService.exportAll();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.filename()).build().toString())
                .body(export.content());
    }

    @GetMapping("/practice/dashboard")
    public PracticeDashboardResponse practiceDashboard() {
        return answerHistoryService.dashboard();
    }

    @GetMapping("/practice/analytics")
    public ResponseEntity<PracticeAnalyticsResponse> practiceAnalytics(
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(defaultValue = "UTC") String timeZone) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(answerHistoryService.analytics(days, top, timeZone));
    }

    @GetMapping("/practice/priority")
    public ResponseEntity<List<QuestionResponse>> priorityPracticeQuestions(
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) QuestionType questionType) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(practiceQuestionSelectionService.selectPriorityQuestions(count, categoryCode, questionType));
    }

    @GetMapping("/review/wrong")
    public WrongQuestionReviewResponse wrongQuestionReview(
            @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "1") int minIncorrect,
            @RequestParam(defaultValue = "50") int count,
            @RequestParam(defaultValue = "true") boolean shuffle) {
        return answerHistoryService.wrongQuestions(categoryCode, minIncorrect, count, shuffle);
    }

    @PostMapping("/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerAttemptResponse submitAttempt(@PathVariable Long id,
                                               @Valid @RequestBody AnswerAttemptRequest request) {
        return answerHistoryService.submit(id, request);
    }

    @GetMapping("/{id}/attempts")
    public Page<AnswerAttemptResponse> answerHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return answerHistoryService.history(id, page, size);
    }

    @GetMapping("/{id}/attempts/summary")
    public AnswerHistorySummary answerHistorySummary(@PathVariable Long id) {
        return answerHistoryService.summary(id);
    }

    @PostMapping("/study-annotations/batch")
    public List<StudyAnnotationResponse> studyAnnotations(@RequestBody StudyAnnotationBatchRequest request) {
        return studyAnnotationService.batch(request);
    }

    @GetMapping("/{id}/study-annotation")
    public ResponseEntity<StudyAnnotationResponse> studyAnnotation(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(studyAnnotationService.get(id));
    }

    @PutMapping("/{id}/study-annotation")
    public StudyAnnotationResponse saveStudyAnnotation(@PathVariable Long id,
                                                       @RequestBody StudyAnnotationRequest request) {
        return studyAnnotationService.save(id, request);
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

    @PostMapping("/reclassify")
    public ReclassificationResult reclassifyAll() {
        return service.reclassifyAll();
    }

    @PostMapping(value = "/import/csv", consumes = "multipart/form-data")
    public ImportResult importCsv(@RequestPart("file") MultipartFile file) { return service.importCsv(file); }

    @PostMapping(value = "/import/json", consumes = "multipart/form-data")
    public ImportResult importJson(@RequestPart("file") MultipartFile file) { return service.importJson(file); }

    @PostMapping(value = "/import/pma-exam", consumes = "multipart/form-data")
    public ImportResult importPmaExam(@RequestPart("file") MultipartFile file) { return service.importPmaExam(file); }
}
