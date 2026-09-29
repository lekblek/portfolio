package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tout le vocabulaire, par nom (D-CS) : il reste court et sert de liste de choix dans l'administration.
 */
@Service
@RequiredArgsConstructor
public class ListTagsUseCase {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<Tag> execute() {
        return tagRepository.findAll();
    }
}
