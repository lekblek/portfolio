package com.scalke.portfolio.backend.security.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptsTest {

    private static final Instant T0 = Instant.parse("2026-06-15T10:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoginAttempts attempts = new LoginAttempts(5, WINDOW);

    @Test
    void allows_four_failures_and_blocks_the_fifth_until_the_first_leaves_the_window() {
        for (int minute = 0; minute < 4; minute++) {
            attempts.recordFailure("10.0.0.1", T0.plus(Duration.ofMinutes(minute)));
        }
        assertThat(attempts.retryAfter("10.0.0.1", T0.plus(Duration.ofMinutes(4)))).isEmpty();

        attempts.recordFailure("10.0.0.1", T0.plus(Duration.ofMinutes(4)));

        assertThat(attempts.retryAfter("10.0.0.1", T0.plus(Duration.ofMinutes(5)))).contains(Duration.ofMinutes(10));
        assertThat(attempts.retryAfter("10.0.0.1", T0.plus(WINDOW))).isEmpty();
    }

    @Test
    void counts_each_source_separately() {
        for (int i = 0; i < 5; i++) {
            attempts.recordFailure("10.0.0.1", T0);
        }

        assertThat(attempts.retryAfter("10.0.0.1", T0)).isPresent();
        assertThat(attempts.retryAfter("10.0.0.2", T0)).isEmpty();
    }

    @Test
    void forgets_the_failures_of_a_source_after_a_success() {
        for (int i = 0; i < 4; i++) {
            attempts.recordFailure("10.0.0.1", T0);
        }
        attempts.reset("10.0.0.1");
        attempts.recordFailure("10.0.0.1", T0);

        assertThat(attempts.retryAfter("10.0.0.1", T0)).isEmpty();
    }

    /**
     * La mémoire reste bornée : les sources dont tous les échecs ont expiré sont oubliées.
     */
    @Test
    void forgets_expired_sources_when_too_many_are_tracked() {
        for (int i = 0; i <= LoginAttempts.PRUNE_ABOVE; i++) {
            attempts.recordFailure("ancienne-" + i, T0);
        }
        attempts.recordFailure("recente", T0.plus(WINDOW));

        assertThat(attempts.trackedSources()).isEqualTo(1);
    }
}
