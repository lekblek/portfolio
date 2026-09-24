package com.scalke.portfolio.backend.project.domain.port;

import com.scalke.portfolio.backend.project.domain.model.Technology;

/**
 * Port de persistance du vocabulaire des technologies.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code create} par le
 * seed de développement. La liste publique arrivera avec un écran qui en a besoin (D-AE), la
 * modification avec l'administration (étape 36).
 */
public interface TechnologyRepository {

    Technology create(Technology technology);
}
