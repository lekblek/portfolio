package com.scalke.portfolio.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
