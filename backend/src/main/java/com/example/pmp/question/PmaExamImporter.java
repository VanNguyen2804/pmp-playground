package com.example.pmp.question;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
public class PmaExamImporter {
    private final QuestionRepository repository;
    private final QuestionClassifier classifier;
    private final ObjectMapper objectMapper;

    public PmaExamImporter(QuestionRepository repository, QuestionClassifier classifier, ObjectMapper objectMapper) {
        this.repository = repository;
        this.classifier = classifier;
        this.objectMapper = objectMapper;
    }

    public boolean supports(JsonNode root) {
        return root.path("exam_attempt").path("exam_content").path("questions").isArray();
    }

    @Transactional
    public ImportResult importRoot(JsonNode root) {
        JsonNode questionNodes = root.path("exam_attempt").path("exam_content").path("questions");
        if (!questionNodes.isArray()) throw new QuestionImportException("JSON không chứa exam_attempt.exam_content.questions.");
        if (questionNodes.isEmpty()) throw new QuestionImportException("exam_content không chứa câu hỏi nào.");

        int total = questionNodes.size(), inserted = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        int index = 0;
        for (JsonNode node : questionNodes) {
            index++;
            try {
                String externalId = text(node, "id");
                Question question = externalId == null ? new Question() : repository.findByExternalId(externalId).orElseGet(Question::new);
                boolean isNew = question.getId() == null;
                apply(question, node);
                repository.save(question);
                if (isNew) inserted++; else updated++;
            } catch (Exception ex) {
                skipped++;
                errors.add("Question " + index + ": " + ex.getMessage());
            }
        }
        if (total > 0 && inserted == 0 && updated == 0) {
            throw new QuestionImportException("Không thể import bất kỳ câu hỏi PMA nào.", errors);
        }
        return new ImportResult(total, inserted, updated, skipped, errors);
    }

    private void apply(Question q, JsonNode node) throws Exception {
        JsonNode data = node.path("data");
        String questionText = text(data, "question");
        if (questionText == null || questionText.isBlank()) throw new IllegalArgumentException("question text is missing");

        q.setExternalId(text(node, "id"));
        q.setExamName(text(node, "name"));
        q.setQuestionType(parseType(text(node, "type")));
        q.setQuestionText(questionText.trim());
        q.setImageUrl(text(node, "image"));
        q.setSource(QuestionSource.PMA);
        q.setDifficulty(null);
        q.setExplanationType(text(data, "explanation_type"));
        q.setExplanationPromptVersion(integer(data, "explanation_prompt_version"));
        q.setNumberOfAnswers(integer(data, "num_answers"));
        q.setRawSourceJson(objectMapper.writeValueAsString(node));

        String explanation = firstText(data, "explanation", "explanation_text", "solution", "rationale");
        if (explanation != null) {
            q.setPmaExplanation(explanation);
            q.setExplanationStatus(ExplanationStatus.IMPORTED);
        } else if (q.getPmaExplanation() == null) {
            // Re-importing a PMA export without explanation text must not erase a manually curated explanation.
            q.setExplanationStatus(ExplanationStatus.NOT_PROVIDED);
        }

        Set<String> correctKeys = answerKeys(data.path("answer"));
        List<QuestionOption> options = new ArrayList<>();
        if (data.path("options").isArray()) {
            int position = 0;
            for (JsonNode optionNode : data.path("options")) {
                String key = optionKey(position);
                QuestionOption option = new QuestionOption();
                option.setOptionKey(key);
                option.setOptionText(optionNode.asText());
                option.setDisplayOrder(position);
                option.setCorrect(correctKeys.contains(key));
                options.add(option);
                position++;
            }
        }
        q.replaceOptions(options);

        List<MatchingPair> pairs = new ArrayList<>();
        if (data.path("pairs").isArray()) {
            int position = 0;
            for (JsonNode pairNode : data.path("pairs")) {
                String left = text(pairNode, "label");
                String right = text(pairNode, "content");
                if (left != null && right != null) {
                    MatchingPair pair = new MatchingPair();
                    pair.setLeftText(left);
                    pair.setRightText(right);
                    pair.setDisplayOrder(position++);
                    pairs.add(pair);
                }
            }
        }
        q.replaceMatchingPairs(pairs);

        String combined = questionText + " " + options.stream().map(QuestionOption::getOptionText).reduce("", (a, b) -> a + " " + b);
        q.getCategories().clear();
        q.getCategories().addAll(classifier.classify(combined));
    }

    private QuestionType parseType(String value) {
        if (value == null) return QuestionType.MCQ;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "mcq" -> QuestionType.MCQ;
            case "mrq" -> QuestionType.MRQ;
            case "matching" -> QuestionType.MATCHING;
            default -> throw new IllegalArgumentException("Unsupported question type: " + value);
        };
    }

    private Set<String> answerKeys(JsonNode node) {
        Set<String> keys = new LinkedHashSet<>();
        if (node.isTextual()) keys.add(node.asText().trim().toUpperCase(Locale.ROOT));
        else if (node.isArray()) node.forEach(n -> keys.add(n.asText().trim().toUpperCase(Locale.ROOT)));
        return keys;
    }

    private String optionKey(int index) {
        if (index < 26) return String.valueOf((char) ('A' + index));
        return "O" + (index + 1);
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isInt() || value.isLong()) return value.asInt();
        try { return Integer.parseInt(value.asText()); } catch (NumberFormatException ex) { return null; }
    }
}
