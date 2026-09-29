package com.scalke.portfolio.backend.profile.web.controller;

import com.scalke.portfolio.backend.media.MediaFixtures;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration du profil de bout en bout (D-CY) : création puis remplacement d'un bloc, collections comprises,
 * validation sur le champ en cause, médias vérifiés, effet sur le profil public.
 */
@Transactional
class AdminProfileIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/profile").contextPath("/api"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/admin/profile").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(profileJson("")))
            .andExpect(status().isForbidden());
    }

    @Test
    void has_no_profile_before_the_first_save() throws Exception {
        mockMvc.perform(get("/api/admin/profile").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /**
     * Le premier enregistrement crée le profil ; chaque collection garde l'ordre saisi, jusque sur le site.
     */
    @Test
    void creates_the_whole_profile() throws Exception {
        long avatar = mediaRepository.create(MediaFixtures.image("Portrait")).id();
        long cv = mediaRepository.create(MediaFixtures.pdf()).id();

        save(profileJson("""
            ,"aboutMarkdown":"# À propos","publicLocation":" Paris ","publicEmail":"contact@example.test",
            "avatarMediaId":%d,"cvMediaId":%d,
            "links":[{"label":"LinkedIn","url":"https://linkedin.example/me"},
                     {"label":"GitHub","url":"https://github.example/me"}],
            "skills":[{"name":"Spring Boot","category":"Backend"},{"name":"Angular","category":"Frontend"}],
            "experiences":[{"organization":"Scalke","title":"Développeur","location":"Paris",
                            "startDate":"2024-01-01","description":"Portfolio"}],
            "educations":[{"institution":"Université","degree":"Master","field":"Informatique","location":"Paris",
                           "startDate":"2019-09-01","endDate":"2021-06-30","description":""}],
            "certifications":[{"name":"Java","issuer":"Oracle","issuedAt":"2025-01-01"}]
            """.formatted(avatar, cv)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Blek"))
            .andExpect(jsonPath("$.publicLocation").value("Paris"))
            .andExpect(jsonPath("$.avatarMediaId").value(avatar))
            .andExpect(jsonPath("$.cvMediaId").value(cv))
            .andExpect(jsonPath("$.links[0].label").value("LinkedIn"))
            .andExpect(jsonPath("$.skills[1].name").value("Angular"))
            .andExpect(jsonPath("$.experiences[0].endDate").value(nullValue()))
            .andExpect(jsonPath("$.educations[0].endDate").value("2021-06-30"))
            .andExpect(jsonPath("$.certifications[0].credentialUrl").value(nullValue()));

        mockMvc.perform(get("/api/admin/profile").contextPath("/api").with(user("admin")))
            .andExpect(jsonPath("$.links[1].label").value("GitHub"));
        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.links[0].label").value("LinkedIn"))
            .andExpect(jsonPath("$.links[1].label").value("GitHub"))
            .andExpect(jsonPath("$.avatar.altText").value("Portrait"))
            .andExpect(jsonPath("$.skillGroups[0].category").value("Backend"));
    }

    /**
     * Les collections sont remplacées : une compétence gardée sous le même nom ne heurte pas {@code skill_unique_name},
     * les lignes retirées disparaissent.
     */
    @Test
    void replaces_the_collections() throws Exception {
        save(profileJson("""
            ,"links":[{"label":"GitHub","url":"https://github.example/me"},{"label":"Blog","url":"https://blog.example"}],
            "skills":[{"name":"Java","category":"Backend"},{"name":"Docker","category":"Outils"}]
            """)).andExpect(status().isOk());

        save(profileJson("""
            ,"links":[{"label":"Blog","url":"https://blog.example"}],
            "skills":[{"name":"Docker","category":"Outils"},{"name":"Java","category":"Langages"}]
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.links.length()").value(1))
            .andExpect(jsonPath("$.skills[0].name").value("Docker"))
            .andExpect(jsonPath("$.skills[1].category").value("Langages"));

        assertThat(count("professional_link")).isEqualTo(1);
        assertThat(count("skill")).isEqualTo(2);
        save("{\"displayName\":\"Blek G.\",\"professionalTitle\":\"Architecte\",\"shortBio\":\"Nouvelle bio\"}")
            .andExpect(jsonPath("$.displayName").value("Blek G."))
            .andExpect(jsonPath("$.professionalTitle").value("Architecte"))
            .andExpect(jsonPath("$.shortBio").value("Nouvelle bio"))
            .andExpect(jsonPath("$.skills.length()").value(0));
        assertThat(count("skill")).isZero();
        assertThat(count("profile")).isEqualTo(1);
    }

    @Test
    void validates_the_profile() throws Exception {
        save("{\"displayName\":\" \",\"professionalTitle\":\"Développeur\",\"shortBio\":\"Bio\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("displayName"));
        save(profileJson(",\"publicEmail\":\"pas-une-adresse\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("publicEmail"));
        save(profileJson(",\"links\":[{\"label\":\"Piège\",\"url\":\"javascript:alert(1)\"}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("links[0].url"));
        save(profileJson("""
            ,"experiences":[{"organization":"Scalke","title":"Dev","location":"Paris","startDate":"2024-01-01",
                             "endDate":"2023-01-01","description":""}]
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("experiences[0].endDate"));
        save(profileJson("""
            ,"certifications":[{"name":"Java","issuer":"Oracle","issuedAt":"2025-01-01","expiresAt":"2024-01-01"}]
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("certifications[0].expiresAt"));
        save(profileJson(",\"skills\":[{\"name\":\"Java\",\"category\":\"A\"},{\"name\":\"JAVA\",\"category\":\"B\"}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("skills"));
        assertThat(count("profile")).isZero();
    }

    /**
     * D06, D-BX : l'avatar est une image, le CV un PDF.
     */
    @Test
    void checks_the_kind_of_each_media() throws Exception {
        long image = mediaRepository.create(MediaFixtures.image(null)).id();
        long pdf = mediaRepository.create(MediaFixtures.pdf()).id();

        save(profileJson(",\"avatarMediaId\":" + pdf))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("avatarMediaId"));
        save(profileJson(",\"cvMediaId\":" + image))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("cvMediaId"));
        save(profileJson(",\"cvMediaId\":999999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("cvMediaId"));
    }

    private ResultActions save(String json) throws Exception {
        return mockMvc.perform(put("/api/admin/profile").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }

    /**
     * Profil minimal ; {@code extra} ajoute des champs (commence par une virgule).
     */
    private static String profileJson(String extra) {
        return """
            {"displayName":"Blek","professionalTitle":"Développeur full-stack","shortBio":"Java et Angular"%s}
            """.formatted(extra);
    }
}
