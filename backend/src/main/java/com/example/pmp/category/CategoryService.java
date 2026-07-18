package com.example.pmp.category;

import com.example.pmp.question.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    public CategoryService(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
    }

    public List<CategoryResponse> list(Taxonomy taxonomy) {
        List<Category> categories = taxonomy == null
                ? categoryRepository.findByActiveTrueOrderByTaxonomyAscDisplayOrderAscNameAsc()
                : categoryRepository.findByTaxonomyAndActiveTrueOrderByDisplayOrderAscNameAsc(taxonomy);
        return categories.stream()
                .map(c -> new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                        c.getTaxonomy(), c.getDisplayOrder(), questionRepository.countByCategories_Id(c.getId())))
                .toList();
    }
}
