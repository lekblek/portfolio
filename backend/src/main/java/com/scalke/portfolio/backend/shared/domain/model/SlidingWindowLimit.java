package com.scalke.portfolio.backend.shared.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limite par source sur une fenêtre glissante : après {@code maxEvents} événements enregistrés dans {@code window},
 * la source attend que le plus ancien sorte de la fenêtre. Sert aux connexions manquées (D-CQ) et aux messages de
 * contact (D-EJ). Une source peut être effacée ({@link #reset}), par exemple après une connexion réussie.
 * <p>
 * En mémoire (une seule instance en V1) et sûr en accès concurrent. Les sources dont tous les événements ont expiré
 * sont oubliées dès que le nombre de sources suivies dépasse {@link #PRUNE_ABOVE}.
 */
public final class SlidingWindowLimit {

    static final int PRUNE_ABOVE = 10_000;

    private final int maxEvents;
    private final Duration window;
    private final ConcurrentMap<String, List<Instant>> events = new ConcurrentHashMap<>();

    public SlidingWindowLimit(int maxEvents, Duration window) {
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be positive");
        }
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxEvents = maxEvents;
        this.window = window;
    }

    /**
     * Temps à attendre avant un nouvel événement depuis {@code source} ; vide s'il est permis.
     */
    public Optional<Duration> retryAfter(String source, Instant now) {
        List<Instant> recent = events.computeIfPresent(Objects.requireNonNull(source), (key, times) -> recent(times, now));
        if (recent == null || recent.size() < maxEvents) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, recent.get(recent.size() - maxEvents).plus(window)));
    }

    public void record(String source, Instant now) {
        events.compute(Objects.requireNonNull(source), (key, times) -> {
            List<Instant> updated = times == null ? new ArrayList<>() : new ArrayList<>(recent(times, now));
            updated.add(now);
            return List.copyOf(updated);
        });
        if (events.size() > PRUNE_ABOVE) {
            events.replaceAll((key, times) -> recent(times, now));
            events.values().removeIf(List::isEmpty);
        }
    }

    public void reset(String source) {
        events.remove(Objects.requireNonNull(source));
    }

    int trackedSources() {
        return events.size();
    }

    /**
     * Événements encore dans la fenêtre à {@code now}.
     */
    private List<Instant> recent(List<Instant> times, Instant now) {
        Instant oldestKept = now.minus(window);
        return times.stream().filter(time -> time.isAfter(oldestKept)).toList();
    }
}
