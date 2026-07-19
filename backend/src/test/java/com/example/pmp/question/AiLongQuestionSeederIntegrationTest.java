package com.example.pmp.question;

import com.example.pmp.category.Category;
import com.example.pmp.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class AiLongQuestionSeederIntegrationTest {
    @Autowired CategoryRepository categoryRepository;
    @Autowired QuestionRepository questionRepository;

    @Test
    void createsAiAndLongQuestionCategoriesAndCuratedBanks() {
        Category ai = categoryRepository.findByCodeIgnoreCase("TOPIC_AI").orElseThrow();
        Category longQuestion = categoryRepository.findByCodeIgnoreCase("TOPIC_LONG_QUESTION").orElseThrow();

        assertThat(ai.isActive()).isTrue();
        assertThat(longQuestion.isActive()).isTrue();
        assertThat(questionRepository.countByCategories_Id(ai.getId())).isGreaterThanOrEqualTo(36);
        assertThat(questionRepository.countByCategories_Id(longQuestion.getId())).isGreaterThanOrEqualTo(12);

        Question aiQuestion = questionRepository.findByExternalId("AI-PRACTICE-0001").orElseThrow();
        assertThat(aiQuestion.getOptions()).hasSize(4);
        assertThat(aiQuestion.getOptions()).anyMatch(QuestionOption::isCorrect);
        assertThat(aiQuestion.getFinalExplanation()).isNotBlank();

        Question pmbok8AiQuestion = questionRepository.findByExternalId("AI-PMBOK8-0001").orElseThrow();
        assertThat(pmbok8AiQuestion.getReference()).contains("PMBOK Guide 8th Edition");
        assertThat(pmbok8AiQuestion.getFinalExplanation()).isNotBlank();

        Question longQuestionItem = questionRepository.findByExternalId("LONG-CASE-AI-0001").orElseThrow();
        assertThat(longQuestionItem.getQuestionText()).contains("CASE 1");
        assertThat(longQuestionItem.getQuestionText().length()).isGreaterThan(700);
    }
}
