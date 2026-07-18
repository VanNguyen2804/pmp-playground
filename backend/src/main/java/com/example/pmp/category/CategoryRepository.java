package com.example.pmp.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCodeIgnoreCase(String code);
    List<Category> findByCodeIn(Collection<String> codes);
    List<Category> findByActiveTrueOrderByTaxonomyAscDisplayOrderAscNameAsc();
    List<Category> findByTaxonomyAndActiveTrueOrderByDisplayOrderAscNameAsc(Taxonomy taxonomy);
}
