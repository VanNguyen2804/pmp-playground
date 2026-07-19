package com.example.pmp.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionStudyAnnotationService {
    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_HIGHLIGHTS = 100;
    private static final int MAX_NOTE_LENGTH = 20_000;
    private static final int MAX_HIGHLIGHT_LENGTH = 2_000;

    private final QuestionStudyAnnotationRepository annotationRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public QuestionStudyAnnotationService(QuestionStudyAnnotationRepository annotationRepository,
                                          QuestionRepository questionRepository,
                                          ObjectMapper objectMapper) {
        this.annotationRepository = annotationRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StudyAnnotationResponse get(Long questionId) {
        ensureQuestionExists(questionId);
        return annotationRepository.findByQuestion_Id(questionId)
                .map(this::toResponse)
                .orElseGet(() -> empty(questionId));
    }

    @Transactional(readOnly = true)
    public List<StudyAnnotationResponse> batch(StudyAnnotationBatchRequest request) {
        List<Long> ids = normalizeIds(request == null ? null : request.questionIds());
        if (ids.isEmpty()) return List.of();
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Chỉ được tải tối đa " + MAX_BATCH_SIZE + " ghi chú mỗi lần.");
        }

        Map<Long, Question> questions = questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        for (Long id : ids) {
            if (!questions.containsKey(id)) throw new QuestionNotFoundException(id);
        }

        Map<Long, QuestionStudyAnnotation> annotations = annotationRepository.findAllByQuestion_IdIn(ids).stream()
                .collect(Collectors.toMap(item -> item.getQuestion().getId(), Function.identity()));

        return ids.stream()
                .map(id -> Optional.ofNullable(annotations.get(id)).map(this::toResponse).orElseGet(() -> empty(id)))
                .toList();
    }

    @Transactional
    public StudyAnnotationResponse save(Long questionId, StudyAnnotationRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        String note = normalizeNote(request == null ? null : request.note());
        List<StudyHighlight> highlights = validateHighlights(question, request == null ? null : request.highlights());

        QuestionStudyAnnotation annotation = annotationRepository.findByQuestion_Id(questionId)
                .orElseGet(() -> {
                    QuestionStudyAnnotation created = new QuestionStudyAnnotation();
                    created.setQuestion(question);
                    return created;
                });
        annotation.setNoteText(note);
        annotation.setHighlightsJson(writeHighlights(highlights));
        return toResponse(annotationRepository.save(annotation));
    }

    private List<StudyHighlight> validateHighlights(Question question, List<StudyHighlight> raw) {
        List<StudyHighlight> highlights = raw == null ? List.of() : raw;
        if (highlights.size() > MAX_HIGHLIGHTS) {
            throw new IllegalArgumentException("Mỗi câu chỉ được lưu tối đa " + MAX_HIGHLIGHTS + " highlight.");
        }

        Map<String, String> optionTexts = question.getOptions().stream()
                .collect(Collectors.toMap(QuestionOption::getOptionKey, QuestionOption::getOptionText, (a, b) -> a));
        Set<String> ids = new HashSet<>();
        List<StudyHighlight> normalized = new ArrayList<>();

        for (StudyHighlight item : highlights) {
            if (item == null || item.target() == null || item.color() == null) {
                throw new IllegalArgumentException("Highlight thiếu target hoặc màu.");
            }
            String id = trimRequired(item.id(), "Highlight thiếu id.");
            if (!ids.add(id)) throw new IllegalArgumentException("Highlight id bị trùng: " + id);
            if (item.startOffset() < 0 || item.endOffset() <= item.startOffset()) {
                throw new IllegalArgumentException("Vị trí highlight không hợp lệ.");
            }

            String sourceText;
            String targetKey = blankToNull(item.targetKey());
            if (item.target() == StudyHighlightTarget.QUESTION) {
                targetKey = null;
                sourceText = question.getQuestionText();
            } else {
                if (targetKey == null || !optionTexts.containsKey(targetKey)) {
                    throw new IllegalArgumentException("Không tìm thấy lựa chọn của highlight.");
                }
                sourceText = optionTexts.get(targetKey);
            }

            if (item.endOffset() > sourceText.length()) {
                throw new IllegalArgumentException("Highlight vượt quá độ dài nội dung.");
            }
            String actual = sourceText.substring(item.startOffset(), item.endOffset());
            if (actual.length() > MAX_HIGHLIGHT_LENGTH) {
                throw new IllegalArgumentException("Đoạn highlight quá dài.");
            }
            if (!actual.equals(item.text())) {
                throw new IllegalArgumentException("Nội dung câu hỏi đã thay đổi. Hãy chọn lại đoạn cần highlight.");
            }

            normalized.add(new StudyHighlight(id, item.target(), targetKey,
                    item.startOffset(), item.endOffset(), actual, item.color()));
        }

        validateNoOverlap(normalized);
        return normalized.stream()
                .sorted(Comparator.comparing(StudyHighlight::target)
                        .thenComparing(item -> Optional.ofNullable(item.targetKey()).orElse(""))
                        .thenComparingInt(StudyHighlight::startOffset))
                .toList();
    }

    private void validateNoOverlap(List<StudyHighlight> highlights) {
        Map<String, List<StudyHighlight>> groups = highlights.stream().collect(Collectors.groupingBy(item ->
                item.target().name() + ":" + Optional.ofNullable(item.targetKey()).orElse("")));
        for (List<StudyHighlight> group : groups.values()) {
            List<StudyHighlight> sorted = group.stream()
                    .sorted(Comparator.comparingInt(StudyHighlight::startOffset)).toList();
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).startOffset() < sorted.get(i - 1).endOffset()) {
                    throw new IllegalArgumentException("Các highlight không được chồng lấn nhau.");
                }
            }
        }
    }

    private StudyAnnotationResponse toResponse(QuestionStudyAnnotation annotation) {
        return new StudyAnnotationResponse(annotation.getQuestion().getId(), annotation.getNoteText(),
                readHighlights(annotation.getHighlightsJson()), annotation.getVersion(), annotation.getUpdatedAt());
    }

    private StudyAnnotationResponse empty(Long questionId) {
        return new StudyAnnotationResponse(questionId, null, List.of(), 0, null);
    }

    private List<StudyHighlight> readHighlights(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không đọc được dữ liệu highlight đã lưu.", ex);
        }
    }

    private String writeHighlights(List<StudyHighlight> highlights) {
        try {
            return objectMapper.writeValueAsString(highlights);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Không thể lưu dữ liệu highlight.", ex);
        }
    }

    private void ensureQuestionExists(Long questionId) {
        if (!questionRepository.existsById(questionId)) throw new QuestionNotFoundException(questionId);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
    }

    private String normalizeNote(String note) {
        String value = blankToNull(note);
        if (value != null && value.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá " + MAX_NOTE_LENGTH + " ký tự.");
        }
        return value;
    }

    private String trimRequired(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new IllegalArgumentException(message);
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
