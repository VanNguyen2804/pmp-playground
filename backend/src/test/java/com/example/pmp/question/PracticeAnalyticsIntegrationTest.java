package com.example.pmp.question;

import com.example.pmp.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pmp_analytics_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class PracticeAnalyticsIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionAnswerAttemptRepository attemptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuestionAnswerHistoryService historyService;

    private Long questionId;

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAll();
        questionRepository.deleteAll();

        Question question = new Question();
        question.setExamName("PMP Full Test 01 - 0009");
        question.setQuestionType(QuestionType.MCQ);
        question.setQuestionText("What should the project manager do first?");
        question.replaceOptions(List.of(
                option("A", "Escalate immediately", false, 0),
                option("B", "Collect information", true, 1)
        ));
        question.getCategories().add(categoryRepository.findByCodeIgnoreCase("TOPIC_RESOURCE_TEAM").orElseThrow());
        questionId = questionRepository.saveAndFlush(question).getId();
    }

    @Test
    void reportsMistakesByCategoryAndTopWrongQuestion() {
        historyService.submit(questionId, new AnswerAttemptRequest(Set.of("A"), Map.of(), "session-1"));
        historyService.submit(questionId, new AnswerAttemptRequest(Set.of("A"), Map.of(), "session-1"));
        historyService.submit(questionId, new AnswerAttemptRequest(Set.of("B"), Map.of(), "session-1"));

        PracticeAnalyticsResponse analytics = historyService.analytics(14, 10, "Asia/Ho_Chi_Minh");

        assertThat(analytics.summary().totalAttempts()).isEqualTo(3);
        assertThat(analytics.summary().incorrectAttempts()).isEqualTo(2);
        assertThat(analytics.summary().accuracyPercentage()).isEqualTo(33.33);
        assertThat(analytics.summary().progressStatus()).isEqualTo("NEW_BASELINE");

        assertThat(analytics.categoryStats()).singleElement().satisfies(category -> {
            assertThat(category.categoryCode()).isEqualTo("TOPIC_RESOURCE_TEAM");
            assertThat(category.incorrectAttempts()).isEqualTo(2);
            assertThat(category.uniqueWrongQuestions()).isEqualTo(1);
        });

        assertThat(analytics.topWrongQuestions()).singleElement().satisfies(question -> {
            assertThat(question.questionId()).isEqualTo(questionId);
            assertThat(question.questionNumber()).isEqualTo("9");
            assertThat(question.incorrectAttempts()).isEqualTo(2);
            assertThat(question.lastAnswerCorrect()).isTrue();
        });
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
