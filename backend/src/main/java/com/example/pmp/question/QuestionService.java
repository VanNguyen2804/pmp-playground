package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import com.example.pmp.category.Taxonomy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional
public class QuestionService {
    private final QuestionRepository repository;
    private final CategoryRepository categoryRepository;
    private final QuestionClassifier classifier;
    private final PmaExamImporter pmaExamImporter;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository repository, CategoryRepository categoryRepository,
                           QuestionClassifier classifier, PmaExamImporter pmaExamImporter, ObjectMapper objectMapper) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.classifier = classifier;
        this.pmaExamImporter = pmaExamImporter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> list(String search, String categoryCode, Taxonomy taxonomy,
                                       Difficulty difficulty, QuestionType questionType,
                                       ExplanationReviewStatus reviewStatus, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        var specification = QuestionSpecifications.filters(
                blankToNull(search), blankToNull(categoryCode), taxonomy, difficulty, questionType, reviewStatus);
        return repository.findAll(specification, pageable).map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(Long id) { return QuestionResponse.from(find(id)); }

    public QuestionResponse create(QuestionRequest request) {
        Question question = new Question();
        apply(question, request, false);
        return QuestionResponse.from(repository.save(question));
    }

    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = find(id);
        apply(question, request, true);
        return QuestionResponse.from(repository.save(question));
    }

    public void delete(Long id) { repository.delete(find(id)); }

    public QuestionResponse updateExplanationReview(Long id, ExplanationReviewRequest request) {
        Question question = find(id);
        question.setPmaExplanation(blankToNull(request.pmaExplanation()));
        question.setExplanationStatus(question.getPmaExplanation() == null
                ? ExplanationStatus.NOT_PROVIDED : ExplanationStatus.MANUAL);
        applyExplanationReview(question, request.aiExplanation(), request.finalExplanation(),
                request.finalExplanationSource(), request.explanationReviewStatus(), request.explanationReviewNotes());
        return QuestionResponse.from(repository.save(question));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> random(int count, String categoryCode, QuestionType questionType) {
        var specification = QuestionSpecifications.filters(
                null, blankToNull(categoryCode), null, null, questionType, null);
        List<Question> all = new ArrayList<>(repository.findAll(specification));
        Collections.shuffle(all);
        return all.stream().limit(Math.min(Math.max(count, 1), 100)).map(QuestionResponse::from).toList();
    }

    public ReclassificationResult reclassifyAll() {
        List<Question> questions = repository.findAll();
        int changed = 0;
        for (Question question : questions) {
            String combined = question.getQuestionText() + " "
                    + question.getOptions().stream().map(QuestionOption::getOptionText).reduce("", (a, b) -> a + " " + b);
            Set<Category> classified = classifier.classify(combined);
            question.getCategories().clear();
            question.getCategories().addAll(classified);
            repository.save(question);
            changed++;
        }
        return new ReclassificationResult(questions.size(), changed);
    }

    public ImportResult importJson(MultipartFile file) {
        validateUpload(file, ".json");
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            if (pmaExamImporter.supports(root)) return pmaExamImporter.importRoot(root);
            if (!root.isArray()) throw new QuestionImportException("JSON phải là một array hoặc có exam_attempt.exam_content.questions.");
            List<QuestionRequest> requests = objectMapper.convertValue(root, new TypeReference<List<QuestionRequest>>() {});
            if (requests.isEmpty()) throw new QuestionImportException("File JSON không chứa câu hỏi nào.");
            return importRequests(requests);
        } catch (QuestionImportException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new QuestionImportException("Không thể chuyển JSON thành câu hỏi: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new QuestionImportException("Không thể đọc file JSON: " + ex.getMessage(), ex);
        }
    }

    public ImportResult importPmaExam(MultipartFile file) {
        validateUpload(file, ".json");
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            return pmaExamImporter.importRoot(root);
        } catch (QuestionImportException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new QuestionImportException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new QuestionImportException("Không thể đọc PMA exam JSON: " + ex.getMessage(), ex);
        }
    }

    public ImportResult importCsv(MultipartFile file) {
        validateUpload(file, ".csv");
        List<QuestionRequest> requests = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true).setTrim(true).get().parse(reader);
            for (CSVRecord record : records) {
                total++;
                try { requests.add(fromCsv(record)); }
                catch (Exception ex) { errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage()); }
            }
        } catch (Exception ex) {
            throw new QuestionImportException("Không thể đọc file CSV: " + ex.getMessage(), ex);
        }
        if (total == 0) throw new QuestionImportException("File CSV không chứa dòng dữ liệu nào.");
        ImportResult result = importRequests(requests);
        List<String> merged = new ArrayList<>(errors); merged.addAll(result.errors());
        return new ImportResult(total, result.importedRows(), result.updatedRows(), total - result.importedRows() - result.updatedRows(), merged);
    }

    private ImportResult importRequests(List<QuestionRequest> requests) {
        int inserted = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            try {
                QuestionRequest request = requests.get(i);
                Question question = blankToNull(request.externalId()) == null ? new Question()
                        : repository.findByExternalId(request.externalId()).orElseGet(Question::new);
                boolean isNew = question.getId() == null;
                apply(question, request, false);
                repository.save(question);
                if (isNew) inserted++; else updated++;
            } catch (Exception ex) {
                skipped++;
                errors.add("Item " + (i + 1) + ": " + ex.getMessage());
            }
        }
        if (!requests.isEmpty() && inserted == 0 && updated == 0) {
            throw new QuestionImportException("Không thể import bất kỳ câu hỏi nào.", errors);
        }
        return new ImportResult(requests.size(), inserted, updated, skipped, errors);
    }

    private QuestionRequest fromCsv(CSVRecord r) {
        List<OptionRequest> options = List.of(
                new OptionRequest("A", required(r, "optionA")),
                new OptionRequest("B", required(r, "optionB")),
                new OptionRequest("C", required(r, "optionC")),
                new OptionRequest("D", required(r, "optionD"))
        );
        Set<String> answers = splitSet(required(r, "correctOption"));
        Set<String> categories = splitSet(optional(r, "categoryCodes"));
        if (categories.isEmpty()) categories = splitSet(optional(r, "category"));
        return new QuestionRequest(optional(r, "externalId"), optional(r, "examName"),
                QuestionType.valueOf(Optional.ofNullable(optional(r, "questionType")).orElse("MCQ").toUpperCase(Locale.ROOT)),
                required(r, "questionText"), optional(r, "imageUrl"), options, answers, List.of(),
                Optional.ofNullable(optional(r, "pmaExplanation")).orElse(optional(r, "explanation")),
                optional(r, "aiExplanation"), optional(r, "finalExplanation"),
                parseFinalSource(optional(r, "finalExplanationSource")),
                parseReviewStatus(optional(r, "explanationReviewStatus")), optional(r, "explanationReviewNotes"),
                parseDifficulty(optional(r, "difficulty")), QuestionSource.CSV, optional(r, "reference"),
                optional(r, "tags"), categories);
    }

    private void apply(Question q, QuestionRequest r, boolean manualEdit) {
        validate(r);
        q.setExternalId(blankToNull(r.externalId()));
        q.setExamName(blankToNull(r.examName()));
        q.setQuestionType(r.questionType());
        q.setQuestionText(r.questionText().trim());
        q.setImageUrl(blankToNull(r.imageUrl()));
        q.setPmaExplanation(blankToNull(r.pmaExplanation()));
        q.setExplanationStatus(q.getPmaExplanation() == null ? ExplanationStatus.NOT_PROVIDED
                : (manualEdit || r.source() == null || r.source() == QuestionSource.MANUAL ? ExplanationStatus.MANUAL : ExplanationStatus.IMPORTED));

        boolean shouldUpdateReview = manualEdit || q.getId() == null || r.aiExplanation() != null
                || r.finalExplanation() != null || r.finalExplanationSource() != null
                || r.explanationReviewStatus() != null || r.explanationReviewNotes() != null;
        if (shouldUpdateReview) {
            applyExplanationReview(q, r.aiExplanation(), r.finalExplanation(), r.finalExplanationSource(),
                    r.explanationReviewStatus(), r.explanationReviewNotes());
        }
        q.setDifficulty(r.difficulty());
        q.setSource(r.source() == null ? QuestionSource.MANUAL : r.source());
        q.setReference(blankToNull(r.reference()));
        q.setTags(blankToNull(r.tags()));
        q.setNumberOfAnswers(r.questionType() == QuestionType.MATCHING
                ? safeList(r.matchingPairs()).size() : safeSet(r.correctAnswers()).size());

        Set<String> correct = safeSet(r.correctAnswers()).stream().map(x -> x.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<QuestionOption> options = new ArrayList<>();
        int order = 0;
        for (OptionRequest item : safeList(r.options())) {
            QuestionOption option = new QuestionOption();
            option.setOptionKey(item.key().trim().toUpperCase(Locale.ROOT));
            option.setOptionText(item.text().trim());
            option.setCorrect(correct.contains(option.getOptionKey()));
            option.setDisplayOrder(order++);
            options.add(option);
        }
        q.replaceOptions(options);

        List<MatchingPair> pairs = new ArrayList<>();
        order = 0;
        for (MatchingPairRequest item : safeList(r.matchingPairs())) {
            MatchingPair pair = new MatchingPair();
            pair.setLeftText(item.left().trim());
            pair.setRightText(item.right().trim());
            pair.setDisplayOrder(order++);
            pairs.add(pair);
        }
        q.replaceMatchingPairs(pairs);

        q.getCategories().clear();
        Set<String> categoryCodes = safeSet(r.categoryCodes());
        if (!categoryCodes.isEmpty()) {
            for (String code : categoryCodes) {
                Category category = categoryRepository.findByCodeIgnoreCase(code.trim())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown category code: " + code));
                q.getCategories().add(category);
            }
        } else {
            String combined = q.getQuestionText() + " " + options.stream().map(QuestionOption::getOptionText).reduce("", (a,b) -> a + " " + b);
            q.getCategories().addAll(classifier.classify(combined));
        }
    }


    private void applyExplanationReview(Question q, String aiExplanation, String finalExplanation,
                                        FinalExplanationSource source, ExplanationReviewStatus reviewStatus,
                                        String reviewNotes) {
        q.setAiExplanation(blankToNull(aiExplanation));
        q.setFinalExplanation(blankToNull(finalExplanation));
        q.setExplanationReviewNotes(blankToNull(reviewNotes));

        FinalExplanationSource selectedSource = source;
        if (q.getFinalExplanation() == null) selectedSource = FinalExplanationSource.NONE;
        else if (selectedSource == null || selectedSource == FinalExplanationSource.NONE) selectedSource = FinalExplanationSource.MANUAL;
        q.setFinalExplanationSource(selectedSource);

        ExplanationReviewStatus status = reviewStatus == null ? ExplanationReviewStatus.PENDING : reviewStatus;
        if (q.getFinalExplanation() == null) status = ExplanationReviewStatus.PENDING;
        q.setExplanationReviewStatus(status);
        q.setExplanationReviewedAt(status == ExplanationReviewStatus.REVIEWED ? java.time.Instant.now() : null);
    }

    private void validate(QuestionRequest r) {
        if (r.questionType() == QuestionType.MATCHING) {
            if (safeList(r.matchingPairs()).size() < 2) throw new IllegalArgumentException("Matching questions require at least two pairs");
            return;
        }
        if (safeList(r.options()).size() < 2) throw new IllegalArgumentException("MCQ/MRQ questions require at least two options");
        int answers = safeSet(r.correctAnswers()).size();
        if (r.questionType() == QuestionType.MCQ && answers != 1) throw new IllegalArgumentException("MCQ requires exactly one correct answer");
        if (r.questionType() == QuestionType.MRQ && answers < 2) throw new IllegalArgumentException("MRQ requires two or more correct answers");
    }

    private void validateUpload(MultipartFile file, String expectedExtension) {
        if (file == null || file.isEmpty()) throw new QuestionImportException("File upload đang trống.");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(expectedExtension)) {
            throw new QuestionImportException("Định dạng file không hợp lệ. Backend yêu cầu file " + expectedExtension + ".");
        }
    }

    private Question find(Long id) { return repository.findById(id).orElseThrow(() -> new QuestionNotFoundException(id)); }
    private Difficulty parseDifficulty(String value) { return value == null ? null : Difficulty.valueOf(value.toUpperCase(Locale.ROOT)); }
    private FinalExplanationSource parseFinalSource(String value) { return value == null ? null : FinalExplanationSource.valueOf(value.toUpperCase(Locale.ROOT)); }
    private ExplanationReviewStatus parseReviewStatus(String value) { return value == null ? null : ExplanationReviewStatus.valueOf(value.toUpperCase(Locale.ROOT)); }
    private String required(CSVRecord r, String name) { String value = optional(r, name); if (value == null) throw new IllegalArgumentException(name + " is required"); return value; }
    private String optional(CSVRecord r, String name) { try { return blankToNull(r.get(name)); } catch (IllegalArgumentException ex) { return null; } }
    private Set<String> splitSet(String value) { if (value == null) return new LinkedHashSet<>(); return Arrays.stream(value.split("[,;|]" )).map(String::trim).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private <T> List<T> safeList(List<T> value) { return value == null ? List.of() : value; }
    private <T> Set<T> safeSet(Set<T> value) { return value == null ? Set.of() : value; }
}
