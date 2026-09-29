package com.scalke.portfolio.backend.security.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limite des connexions manquées par source (D-CQ) : après {@code maxFailures} échecs dans une fenêtre glissante de
 * {@code window}, la source attend que le plus ancien sorte de la fenêtre. Une connexion réussie efface le compte.
 * Aucun verrouillage du compte : un attaquant pourrait sinon bloquer l'administrateur.
 * <p>
 * En mémoire (une seule instance en V1) et sûr en accès concurrent. Les sources dont tous les échecs ont expiré sont
 * oubliées dès que le nombre de sources suivies dépasse {@link #PRUNE_ABOVE}.
 */
public final class LoginAttempts {

    static final int PRUNE_ABOVE = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final ConcurrentMap<String, List<Instant>> failures = new ConcurrentHashMap<>();

    public LoginAttempts(int maxFailures, Duration window) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxFailures = maxFailures;
        this.window = window;
    }

    /**
     * Temps à attendre avant une nouvelle tentative depuis {@code source} ; vide si elle est permise.
     */
    public Optional<Duration> retryAfter(String source, Instant now) {
        List<Instant> recent = failures.computeIfPresent(Objects.requireNonNull(source), (key, times) -> recent(times, now));
        if (recent == null || recent.size() < maxFailures) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, recent.get(recent.size() - maxFailures).plus(window)));
    }

    public void recordFailure(String source, Instant now) {
        failures.compute(Objects.requireNonNull(source), (key, times) -> {
            List<Instant> updated = times == null ? new ArrayList<>() : new ArrayList<>(recent(times, now));
            updated.add(now);
            return List.copyOf(updated);
        });
        if (failures.size() > PRUNE_ABOVE) {
            failures.replaceAll((key, times) -> recent(times, now));
            failures.values().removeIf(List::isEmpty);
        }
    }

    public void reset(String source) {
        failures.remove(Objects.requireNonNull(source));
    }

    int trackedSources() {
        return failures.size();
    }

    /**
     * Échecs encore dans la fenêtre à {@code now}.
     */
    private List<Instant> recent(List<Instant> times, Instant now) {
        Instant oldestKept = now.minus(window);
        return times.stream().filter(time -> time.isAfter(oldestKept)).toList();
    }
}
