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
class ChartQuestionSeederIntegrationTest {
    @Autowired CategoryRepository categoryRepository;
    @Autowired QuestionRepository questionRepository;

    @Test
    void createsChartCategoryAndVisualQuestionBank() {
        Category chart = categoryRepository.findByCodeIgnoreCase("TOPIC_CHART").orElseThrow();
        assertThat(chart.isActive()).isTrue();
        assertThat(questionRepository.countByCategories_Id(chart.getId())).isGreaterThanOrEqualTo(20);

        Question question = questionRepository.findByExternalId("CHART-GUIDE-001").orElseThrow();
        assertThat(question.getImageUrl()).startsWith("/chart-guides/");
        assertThat(question.getOptions()).hasSize(4);
        assertThat(question.getOptions()).anyMatch(QuestionOption::isCorrect);
    }
}
