package com.example.pmp.question;

import com.example.pmp.category.Category;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionCsvExportService {
    private static final String[] HEADERS = {
            "id",
            "externalId",
            "examName",
            "questionType",
            "questionText",
            "categoryCodes",
            "categoryNames",
            "currentAnswerKeys",
            "currentAnswerText",
            "allOptions",
            "source",
            "difficulty",
            "updatedAt"
    };

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final QuestionRepository repository;

    public QuestionCsvExportService(QuestionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public QuestionCsvExport exportAll() {
        List<Question> questions = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {

            // UTF-8 BOM helps Excel display Vietnamese characters correctly.
            writer.write('\uFEFF');

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader(HEADERS)
                    .setRecordSeparator("\r\n")
                    .get();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (Question question : questions) {
                    printer.printRecord(
                            question.getId(),
                            value(question.getExternalId()),
                            value(question.getExamName()),
                            question.getQuestionType(),
                            value(question.getQuestionText()),
                            categoryCodes(question),
                            categoryNames(question),
                            currentAnswerKeys(question),
                            currentAnswerText(question),
                            allOptions(question),
                            question.getSource(),
                            question.getDifficulty() == null ? "" : question.getDifficulty(),
                            question.getUpdatedAt() == null ? "" : question.getUpdatedAt().toString()
                    );
                }
                printer.flush();
            }

            String timestamp = LocalDateTime.ofInstant(java.time.Instant.now(), ZoneOffset.UTC).format(FILE_TIMESTAMP);
            return new QuestionCsvExport("pmp_questions_" + timestamp + ".csv", output.toByteArray());
        } catch (Exception exception) {
            throw new QuestionExportException("Không thể tạo file CSV câu hỏi.", exception);
        }
    }

    private String categoryCodes(Question question) {
        return sortedCategories(question).stream()
                .map(Category::getCode)
                .collect(Collectors.joining(" | "));
    }

    private String categoryNames(Question question) {
        return sortedCategories(question).stream()
                .map(Category::getName)
                .collect(Collectors.joining(" | "));
    }

    private List<Category> sortedCategories(Question question) {
        return question.getCategories().stream()
                .sorted(Comparator.comparing((Category category) -> category.getTaxonomy().name())
                        .thenComparingInt(Category::getDisplayOrder)
                        .thenComparing(Category::getName))
                .toList();
    }

    private String currentAnswerKeys(Question question) {
        if (question.getQuestionType() == QuestionType.MATCHING) {
            return "";
        }
        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .sorted(Comparator.comparingInt(QuestionOption::getDisplayOrder))
                .map(QuestionOption::getOptionKey)
                .collect(Collectors.joining(" | "));
    }

    private String currentAnswerText(Question question) {
        if (question.getQuestionType() == QuestionType.MATCHING) {
            return question.getMatchingPairs().stream()
                    .sorted(Comparator.comparingInt(MatchingPair::getDisplayOrder))
                    .map(pair -> value(pair.getLeftText()) + " -> " + value(pair.getRightText()))
                    .collect(Collectors.joining(" | "));
        }

        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .sorted(Comparator.comparingInt(QuestionOption::getDisplayOrder))
                .map(option -> option.getOptionKey() + ". " + value(option.getOptionText()))
                .collect(Collectors.joining(" | "));
    }

    private String allOptions(Question question) {
        if (question.getQuestionType() == QuestionType.MATCHING) {
            return "";
        }
        return question.getOptions().stream()
                .sorted(Comparator.comparingInt(QuestionOption::getDisplayOrder))
                .map(option -> option.getOptionKey() + ". " + value(option.getOptionText()))
                .collect(Collectors.joining(" | "));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
