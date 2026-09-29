package com.scalke.portfolio.backend.taxonomy.web.controller;

import com.scalke.portfolio.backend.taxonomy.application.usecase.CreateCategoryUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.DeleteCategoryUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.ListCategoriesUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.UpdateCategoryUseCase;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.web.dto.AdminCategoryResponse;
import com.scalke.portfolio.backend.taxonomy.web.dto.SaveCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Administration des categories (D-CS), derrière la session de l'administrateur ({@code 05} §6 : identifiant technique
 * dans l'URI).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    @GetMapping
    List<AdminCategoryResponse> list() {
        return listCategoriesUseCase.execute().stream().map(AdminCategoryResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<AdminCategoryResponse> create(@Valid @RequestBody SaveCategoryRequest body) {
        Category created = createCategoryUseCase.execute(body.toDraft());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
            .body(AdminCategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminCategoryResponse update(@PathVariable Long id, @Valid @RequestBody SaveCategoryRequest body) {
        return AdminCategoryResponse.from(updateCategoryUseCase.execute(id, body.toDraft()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
    }
}
