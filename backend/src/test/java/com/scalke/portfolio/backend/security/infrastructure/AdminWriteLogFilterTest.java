package com.scalke.portfolio.backend.security.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DF : chaque écriture d'administration laisse une ligne (méthode, chemin, statut, administrateur) ; les lectures,
 * les routes publiques et la session (journalisée à part) n'en laissent pas.
 */
@ExtendWith(OutputCaptureExtension.class)
class AdminWriteLogFilterTest {

    private final AdminWriteLogFilter filter = new AdminWriteLogFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logs_an_administration_write_with_its_outcome(CapturedOutput output) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));

        perform("DELETE", "/admin/media/42", 409);

        assertThat(output).contains("Administration : DELETE /api/admin/media/42 → 409 (admin)");
    }

    @Test
    void ignores_reads_public_routes_and_the_session(CapturedOutput output) throws Exception {
        perform("GET", "/admin/projects", 200);
        perform("POST", "/public/contact-messages", 201);
        perform("POST", "/admin/session", 200);
        perform("DELETE", "/admin/session", 204);

        assertThat(output).doesNotContain("Administration :");
    }

    private void perform(String method, String path, int status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setContextPath("/api");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            ((MockHttpServletResponse) res).setStatus(status));
    }
}
