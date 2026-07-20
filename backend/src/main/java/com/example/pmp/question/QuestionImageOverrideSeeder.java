package com.example.pmp.question;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QuestionImageOverrideSeeder implements ApplicationRunner {
    private final QuestionRepository repository;

    public QuestionImageOverrideSeeder(QuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        overrides().forEach((examName, imageUrl) ->
                repository.findByExamNameIgnoreCase(examName)
                        .ifPresent(question -> {
                            if (!imageUrl.equals(question.getImageUrl())) {
                                question.setImageUrl(imageUrl);
                                repository.save(question);
                            }
                        }));
    }

    private Map<String, String> overrides() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("PMP Full Test 01 - 0162", "/question-images/pmp-full-test-01-0162.png");
        values.put("PMP Full Test 01 - 0175", "/question-images/pmp-full-test-01-0175.svg");
        return values;
    }
}
