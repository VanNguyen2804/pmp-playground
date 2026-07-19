package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pmp_session_report_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class PracticeSessionReportIntegrationTest {

    @Autowired QuestionRepository questionRepository;
    @Autowired QuestionAnswerAttemptRepository attemptRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired QuestionAnswerHistoryService historyService;

    @Test
    void reportsCurrentSessionMistakesAndPmbok8Suggestions() {
        String sessionId = "session-report-integration";
        Category risk = categoryRepository.findByCodeIgnoreCase("TOPIC_RISK").orElseThrow();
        Category ai = categoryRepository.findByCodeIgnoreCase("TOPIC_AI").orElseThrow();

        Question riskQuestion = createQuestion("SESSION-REPORT-RISK", "A risk has occurred. What should the PM do?", risk);
        Question aiQuestion = createQuestion("SESSION-REPORT-AI", "An AI tool created a draft. What should the PM do?", ai);
        Question skippedQuestion = createQuestion("SESSION-REPORT-SKIPPED", "This question is intentionally skipped.", risk);

        historyService.submit(riskQuestion.getId(), new AnswerAttemptRequest(Set.of("A"), Map.of(), sessionId));
        historyService.submit(aiQuestion.getId(), new AnswerAttemptRequest(Set.of("B"), Map.of(), sessionId));

        PracticeSessionReportResponse report = historyService.sessionReport(new PracticeSessionReportRequest(
                sessionId,
                List.of(riskQuestion.getId(), aiQuestion.getId(), skippedQuestion.getId())
        ));

        assertThat(report.summary().totalQuestions()).isEqualTo(3);
        assertThat(report.summary().answeredQuestions()).isEqualTo(2);
        assertThat(report.summary().skippedQuestions()).isEqualTo(1);
        assertThat(report.summary().correctAnswers()).isEqualTo(1);
        assertThat(report.summary().incorrectAnswers()).isEqualTo(1);
        assertThat(report.summary().accuracyPercentage()).isEqualTo(50.0);

        assertThat(report.categoryResults()).anySatisfy(category -> {
            assertThat(category.categoryCode()).isEqualTo("TOPIC_RISK");
            assertThat(category.incorrectAnswers()).isEqualTo(1);
            assertThat(category.wrongQuestionIds()).contains(riskQuestion.getId());
        });
        assertThat(report.suggestions()).anySatisfy(suggestion -> {
            assertThat(suggestion.categoryCode()).isEqualTo("TOPIC_RISK");
            assertThat(suggestion.pmbokReference()).contains("PMBOK 8");
            assertThat(suggestion.focusAreas()).isNotEmpty();
        });
    }

    private Question createQuestion(String externalId, String text, Category category) {
        Question question = new Question();
        question.setExternalId(externalId);
        question.setExamName("Session Report Test - 0001");
        question.setQuestionType(QuestionType.MCQ);
        question.setQuestionText(text);
        question.replaceOptions(List.of(
                option("A", "Incorrect answer", false, 0),
                option("B", "Correct answer", true, 1)
        ));
        question.getCategories().add(category);
        return questionRepository.saveAndFlush(question);
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
