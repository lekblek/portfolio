package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tout le vocabulaire, par nom (D-CS) : il reste court et sert de liste de choix dans l'administration.
 */
@Service
@RequiredArgsConstructor
public class ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> execute() {
        return categoryRepository.findAll();
    }
}
