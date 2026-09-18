package com.scalke.portfolio.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class ApiConventionsTest {

    private static final String BASE = "com.scalke.portfolio.backend";

    private static JavaClasses backendClasses;

    @BeforeAll
    static void importBackendClasses() {
        backendClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
    }

    @Test
    void controllers_should_reside_in_an_api_package() {
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..web.controller..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void controllers_should_declare_their_audience() {
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().haveSimpleNameStartingWith("Public")
            .orShould().haveSimpleNameStartingWith("Admin")
            .allowEmptyShould(true)
            .check(backendClasses);
    }

    @Test
    void jpa_entities_should_reside_in_a_domain_package() {
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.jpa.entity..")
            .allowEmptyShould(true)
            .check(backendClasses);
    }
}
