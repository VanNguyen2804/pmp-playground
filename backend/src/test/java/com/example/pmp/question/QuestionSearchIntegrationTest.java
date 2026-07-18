package com.example.pmp.question;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pmp_question_search_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class QuestionSearchIntegrationTest {

    @Autowired
    private QuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        Question question = new Question();
        question.setQuestionText("How should an agile leader handle project risk?");
        question.setAiExplanation("Review the risk collaboratively with the team.");
        question.setTags("agile,risk");
        repository.save(question);
    }

    @Test
    void listWithoutFiltersDoesNotApplyLowerToNullParameters() {
        var result = repository.findAll(
                QuestionSpecifications.filters(null, null, null, null, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void textSearchMatchesQuestionAndExplanationFields() {
        var result = repository.findAll(
                QuestionSpecifications.filters("COLLABORATIVELY", null, null, null, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
