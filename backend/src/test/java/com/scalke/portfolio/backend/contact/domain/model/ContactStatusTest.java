package com.scalke.portfolio.backend.contact.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContactStatusTest {

    /**
     * Invariant 29 : table exhaustive des seize paires ; on n'avance que dans l'ordre du cycle, jamais en arrière
     * ni sur place.
     */
    @ParameterizedTest(name = "{0} → {1} : {2}")
    @CsvSource({
        "NEW,       NEW,       false",
        "NEW,       READ,      true",
        "NEW,       PROCESSED, true",
        "NEW,       ARCHIVED,  true",
        "READ,      NEW,       false",
        "READ,      READ,      false",
        "READ,      PROCESSED, true",
        "READ,      ARCHIVED,  true",
        "PROCESSED, NEW,       false",
        "PROCESSED, READ,      false",
        "PROCESSED, PROCESSED, false",
        "PROCESSED, ARCHIVED,  true",
        "ARCHIVED,  NEW,       false",
        "ARCHIVED,  READ,      false",
        "ARCHIVED,  PROCESSED, false",
        "ARCHIVED,  ARCHIVED,  false"
    })
    void moves_only_forward_in_the_cycle(ContactStatus from, ContactStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
