package com.scalke.portfolio.backend;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D-DG : le contrat OpenAPI généré par springdoc est versionné dans {@code docs/api/openapi.json} ; toute modification
 * de l'API (route, champ, validation, code de retour) apparaît dans la revue. Clés triées, sans {@code servers}
 * (adresse de la requête). Après un changement voulu : {@code ./mvnw verify -Dit.test=OpenApiContractIT
 * -Dopenapi.update=true}, puis relire le fichier dans le diff.
 */
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiContractIT extends AbstractIntegrationTest {

    static final Path CONTRACT = Path.of("..", "docs", "api", "openapi.json");

    private static final JsonMapper JSON = JsonMapper.builder()
        .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

    @Autowired
    MockMvc mockMvc;

    @Test
    void the_generated_contract_matches_the_versioned_one() throws Exception {
        String generated = normalize(mockMvc.perform(get("/api/v3/api-docs").contextPath("/api"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, generated);
        }

        assertThat(Files.exists(CONTRACT))
            .as("contrat absent : lancer avec -Dopenapi.update=true").isTrue();
        assertThat(generated)
            .as("l'API a changé : si c'est voulu, régénérer avec -Dopenapi.update=true et relire le diff")
            .isEqualTo(normalize(Files.readString(CONTRACT)));
    }

    /**
     * KI-37 : un contrôleur de test ({@code @TestComponent}) n'entre jamais dans le contrat.
     */
    @Test
    void the_contract_describes_no_test_route() throws Exception {
        JsonNode paths = JSON.readTree(Files.readString(CONTRACT)).get("paths");

        assertThat(paths.propertyNames()).noneMatch(path -> path.startsWith("/test-"));
    }

    private static String normalize(String contract) throws IOException {
        JsonNode tree = JSON.readTree(contract);
        ((ObjectNode) tree).remove("servers");
        return JSON.writeValueAsString(tree).replace("\r\n", "\n") + "\n";
    }
}
