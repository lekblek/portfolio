package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.project.application.usecase.CreateTechnologyUseCase;
import com.scalke.portfolio.backend.project.application.usecase.DeleteTechnologyUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ListTechnologiesUseCase;
import com.scalke.portfolio.backend.project.application.usecase.UpdateTechnologyUseCase;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.web.dto.AdminTechnologyResponse;
import com.scalke.portfolio.backend.project.web.dto.SaveTechnologyRequest;
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
 * Administration du vocabulaire des technologies (D-CW), derrière la session de l'administrateur, sur le modèle des
 * tags (D-CS).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/technologies")
public class AdminTechnologyController {

    private final ListTechnologiesUseCase listTechnologiesUseCase;
    private final CreateTechnologyUseCase createTechnologyUseCase;
    private final UpdateTechnologyUseCase updateTechnologyUseCase;
    private final DeleteTechnologyUseCase deleteTechnologyUseCase;

    @GetMapping
    List<AdminTechnologyResponse> list() {
        return listTechnologiesUseCase.execute().stream().map(AdminTechnologyResponse::from).toList();
    }

    @PostMapping
    ResponseEntity<AdminTechnologyResponse> create(@Valid @RequestBody SaveTechnologyRequest body) {
        Technology created = createTechnologyUseCase.execute(body.toDraft());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
            .body(AdminTechnologyResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminTechnologyResponse update(@PathVariable Long id, @Valid @RequestBody SaveTechnologyRequest body) {
        return AdminTechnologyResponse.from(updateTechnologyUseCase.execute(id, body.toDraft()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        deleteTechnologyUseCase.execute(id);
    }
}
