package com.scalke.portfolio.backend.taxonomy.web.controller;

import com.scalke.portfolio.backend.taxonomy.application.usecase.CreateTagUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.DeleteTagUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.ListTagsUseCase;
import com.scalke.portfolio.backend.taxonomy.application.usecase.UpdateTagUseCase;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.web.dto.AdminTagResponse;
import com.scalke.portfolio.backend.taxonomy.web.dto.SaveTagRequest;
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
 * Administration des tags (D-CS), derrière la session de l'administrateur ({@code 05} §6 : identifiant technique
 * dans l'URI).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/tags")
public class AdminTagController {

    private final ListTagsUseCase listTagsUseCase;
    private final CreateTagUseCase createTagUseCase;
    private final UpdateTagUseCase updateTagUseCase;
    private final DeleteTagUseCase deleteTagUseCase;

    @GetMapping
    List<AdminTagResponse> list() {
        return listTagsUseCase.execute().stream().map(AdminTagResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<AdminTagResponse> create(@Valid @RequestBody SaveTagRequest body) {
        Tag created = createTagUseCase.execute(body.toDraft());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
            .body(AdminTagResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminTagResponse update(@PathVariable Long id, @Valid @RequestBody SaveTagRequest body) {
        return AdminTagResponse.from(updateTagUseCase.execute(id, body.toDraft()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        deleteTagUseCase.execute(id);
    }
}
