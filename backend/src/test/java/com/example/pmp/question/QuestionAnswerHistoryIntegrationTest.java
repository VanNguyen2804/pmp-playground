package com.example.pmp.question;

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
        "spring.datasource.url=jdbc:h2:mem:pmp_answer_history_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class QuestionAnswerHistoryIntegrationTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionAnswerAttemptRepository attemptRepository;

    @Autowired
    private QuestionAnswerHistoryService historyService;

    private Long questionId;

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAll();
        questionRepository.deleteAll();

        Question question = new Question();
        question.setQuestionType(QuestionType.MCQ);
        question.setQuestionText("What should the project manager do first?");
        question.replaceOptions(List.of(
                option("A", "Escalate immediately", false, 0),
                option("B", "Collect information from the team", true, 1)
        ));
        questionId = questionRepository.saveAndFlush(question).getId();
    }

    @Test
    void storesEveryWrongAndCorrectAttemptForTheSameQuestion() {
        AnswerAttemptResponse wrong = historyService.submit(questionId,
                new AnswerAttemptRequest(Set.of("A"), Map.of(), "session-1"));
        AnswerAttemptResponse correct = historyService.submit(questionId,
                new AnswerAttemptRequest(Set.of("B"), Map.of(), "session-1"));

        assertThat(wrong.correct()).isFalse();
        assertThat(correct.correct()).isTrue();
        assertThat(correct.summary().totalAttempts()).isEqualTo(2);
        assertThat(correct.summary().correctAttempts()).isEqualTo(1);
        assertThat(correct.summary().incorrectAttempts()).isEqualTo(1);
        assertThat(correct.summary().accuracyPercentage()).isEqualTo(50.0);

        var history = historyService.history(questionId, 0, 20);
        assertThat(history.getTotalElements()).isEqualTo(2);
        assertThat(history.getContent()).extracting(AnswerAttemptResponse::correct)
                .containsExactly(true, false);
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
