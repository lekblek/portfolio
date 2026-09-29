package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.series.domain.model.Series;

import java.util.List;
import java.util.Objects;

/**
 * Série vue par l'administration (D-CV), lue à un instant donné : tous ses chapitres, quel que soit le statut de leur
 * article, et si son slug peut encore changer (un de ses articles a déjà été public, D-BK).
 */
public record AdminSeries(Series series, boolean slugLocked, List<Chapter> chapters) {

    public AdminSeries {
        Objects.requireNonNull(series, "series");
        chapters = List.copyOf(chapters);
    }

    /**
     * Chapitre stocké : position (1, 2, …), article, et son statut observable (une planification échue est publiée).
     */
    public record Chapter(int position, Publication publication, PublicationStatus status) {
    }
}
