package com.example.pmp.category;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) Taxonomy taxonomy) {
        return service.list(taxonomy);
    }
}
