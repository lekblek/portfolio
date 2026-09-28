package com.scalke.portfolio.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModuleBoundariesTest {
    private static final String BASE = "com.scalke.portfolio.backend";

    private static JavaClasses backendClasses;

    @BeforeAll
    static void importBackendClasses() {
        backendClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
    }

    @Test
    void modules_should_be_free_of_cycles() {
        slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void shared_should_not_depend_on_business_modules() {
        noClasses()
            .that().resideInAPackage(BASE + ".shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                BASE + ".security..",
                BASE + ".profile..",
                BASE + ".project..",
                BASE + ".publication..",
                BASE + ".series..",
                BASE + ".taxonomy..",
                BASE + ".media..",
                BASE + ".search..",
                BASE + ".contact..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    /**
     * D-AN ({@code 04} §8) : un module n'utilise d'un autre module que son modèle ({@code domain.model}) et
     * ses services applicatifs ({@code application}, par exemple {@code TaxonomyQueryService}) ; jamais ses
     * ports, sa persistance ni ses contrôleurs. Chaque module reste propriétaire de ses tables.
     */
    @Test
    void modules_only_use_each_other_through_domain_models_and_application_services() {
        for (String module : List.of("shared", "security", "profile", "project", "publication",
            "series", "taxonomy", "media", "search", "contact")) {
            String root = BASE + "." + module;
            noClasses()
                .that().resideOutsideOfPackage(root + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    root + ".domain.port..",
                    root + ".infrastructure..",
                    root + ".web..")
                .allowEmptyShould(true)
                .check(backendClasses);
        }
    }

    /**
     * Graphe des dépendances autorisées entre modules ({@code 04-architecture-backend.md} §4). Toute
     * dépendance absente de cette table est interdite, même sans cycle (ex. {@code profile → publication}).
     */
    private static final Map<String, Set<String>> ALLOWED_MODULE_DEPENDENCIES = Map.ofEntries(
        Map.entry("shared", Set.of()),
        Map.entry("security", Set.of("shared")),
        Map.entry("taxonomy", Set.of("shared")),
        Map.entry("media", Set.of("shared")),
        Map.entry("contact", Set.of("shared")),
        Map.entry("profile", Set.of("shared", "media")),
        Map.entry("project", Set.of("shared", "media")),
        Map.entry("publication", Set.of("shared", "media", "taxonomy")),
        Map.entry("series", Set.of("shared", "publication", "media")),
        Map.entry("search", Set.of("shared", "publication", "project")));

    @Test
    void modules_only_depend_on_the_modules_allowed_by_the_architecture() {
        ALLOWED_MODULE_DEPENDENCIES.forEach((module, allowed) -> {
            String[] forbidden = ALLOWED_MODULE_DEPENDENCIES.keySet().stream()
                .filter(other -> !other.equals(module) && !allowed.contains(other))
                .map(other -> BASE + "." + other + "..")
                .toArray(String[]::new);
            noClasses()
                .that().resideInAPackage(BASE + "." + module + "..")
                .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                .as(module + " ne dépend que de " + allowed + " (04 §4)")
                .allowEmptyShould(true)
                .check(backendClasses);
        });
    }

    @Test
    void classes_should_reside_in_declared_modules() {
        classes()
            .should().resideInAnyPackage(
                BASE,
                BASE + ".shared..",
                BASE + ".security..",
                BASE + ".profile..",
                BASE + ".project..",
                BASE + ".publication..",
                BASE + ".series..",
                BASE + ".taxonomy..",
                BASE + ".media..",
                BASE + ".search..",
                BASE + ".contact..")
            .check(backendClasses);
    }
}
