package com.scalke.portfolio.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Couches internes d'un module (docs/04-architecture-backend.md §6, ADR 0001).
 * Les motifs s'appliquent à tous les modules métier.
 */
class ModuleLayersTest {

    private static final String BASE = "com.scalke.portfolio.backend";

    private static JavaClasses backendClasses;

    @BeforeAll
    static void importBackendClasses() {
        backendClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
    }

    @Test
    void domain_should_not_depend_on_other_layers_nor_on_frameworks() {
        noClasses()
            .that().resideInAPackage(BASE + ".*.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                BASE + ".*.application..",
                BASE + ".*.infrastructure..",
                BASE + ".*.web..",
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void application_should_not_depend_on_infrastructure_nor_web() {
        noClasses()
            .that().resideInAPackage(BASE + ".*.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                BASE + ".*.infrastructure..",
                BASE + ".*.web..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void web_should_not_depend_on_infrastructure() {
        noClasses()
            .that().resideInAPackage(BASE + ".*.web..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".*.infrastructure..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void infrastructure_should_not_depend_on_web() {
        noClasses()
            .that().resideInAPackage(BASE + ".*.infrastructure..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".*.web..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }
}
