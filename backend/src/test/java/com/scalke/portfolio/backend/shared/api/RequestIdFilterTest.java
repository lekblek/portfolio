package com.scalke.portfolio.backend.shared.api;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DB : identifiant présent dans les journaux pendant la requête, renvoyé au client, retiré ensuite ; un identifiant
 * reçu n'est repris que s'il est sûr.
 */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generates_an_id_visible_in_logs_during_the_request_only() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response,
            (request, res) -> duringRequest.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(duringRequest.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(duringRequest.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void keeps_a_safe_id_set_by_the_reverse_proxy() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "caddy-7f3a.42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("caddy-7f3a.42");
    }

    /**
     * Un en-tête arbitraire (retour à la ligne, texte long) n'entre jamais dans les journaux.
     */
    @Test
    void replaces_an_unsafe_id() throws Exception {
        for (String unsafe : new String[]{"abc\nFAUX journal", "x".repeat(65), "", "id avec espace"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(RequestIdFilter.HEADER, unsafe);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> { });

            assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotEqualTo(unsafe).matches("[0-9a-f-]{36}");
        }
    }
}
