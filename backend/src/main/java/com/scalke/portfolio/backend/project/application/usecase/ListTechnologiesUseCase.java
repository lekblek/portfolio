package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tout le vocabulaire des technologies, dans son ordre d'affichage (D-CW) : court, il sert de liste de choix.
 */
@Service
@RequiredArgsConstructor
public class ListTechnologiesUseCase {

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<Technology> execute() {
        return technologyRepository.findAll();
    }
}
