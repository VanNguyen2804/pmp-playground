package com.example.pmp.question;

import com.example.pmp.category.CategoryRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pmp_csv_export_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class QuestionCsvExportIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionCsvExportService exportService;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();

        Question question = new Question();
        question.setExternalId("PMA-001");
        question.setExamName("Exam 1");
        question.setQuestionType(QuestionType.MCQ);
        question.setQuestionText("What should the project manager do first?");
        question.setSource(QuestionSource.PMA);

        QuestionOption optionA = option("A", "Escalate immediately", false, 0);
        QuestionOption optionB = option("B", "Collect information from the team", true, 1);
        question.replaceOptions(List.of(optionA, optionB));

        question.getCategories().add(categoryRepository.findByCodeIgnoreCase("TOPIC_AGILE_HYBRID").orElseThrow());
        question.getCategories().add(categoryRepository.findByCodeIgnoreCase("TOPIC_RESOURCE_TEAM").orElseThrow());
        questionRepository.save(question);
    }

    @Test
    void exportsOneRowPerQuestionWithCategoriesAndCurrentAnswer() throws Exception {
        QuestionCsvExport export = exportService.exportAll();

        assertThat(export.filename()).startsWith("pmp_questions_").endsWith(".csv");
        String csv = new String(export.content(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get()
                .parse(new StringReader(csv.substring(1)))) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(1);
            CSVRecord record = records.getFirst();
            assertThat(record.get("questionText")).isEqualTo("What should the project manager do first?");
            assertThat(record.get("categoryNames")).contains("Agile & Hybrid", "Resource, Team & Leadership Management");
            assertThat(record.get("currentAnswerKeys")).isEqualTo("B");
            assertThat(record.get("currentAnswerText")).isEqualTo("B. Collect information from the team");
        }
    }

    private QuestionOption option(String key, String text, boolean correct, int order) {
        QuestionOption option = new QuestionOption();
        option.setOptionKey(key);
        option.setOptionText(text);
        option.setCorrect(correct);
        option.setDisplayOrder(order);
        return option;
    }
}
