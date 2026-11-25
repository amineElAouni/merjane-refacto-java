package com.nimbleways.springboilerplate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.nimbleways.springboilerplate", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String CONTROLLERS = "..controllers..";
    private static final String SERVICES = "..services..";
    private static final String HANDLERS = "..services.handlers..";
    private static final String REPOSITORIES = "..repositories..";
    private static final String ENTITIES = "..entities..";

    @ArchTest
    static final ArchRule layersAreOnlyAccessedFromTheLayerAbove = layeredArchitecture()
            .layer("Controllers").definedBy(CONTROLLERS)
            .layer("Services").definedBy(SERVICES)
            .layer("Repositories").definedBy(REPOSITORIES)
            .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
            .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers")
            .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services");

    @ArchTest
    static final ArchRule productRulesDoNotDependOnPersistence = noClasses()
            .that().resideInAPackage(HANDLERS)
            .should().dependOnClassesThat().resideInAPackage(REPOSITORIES);

    @ArchTest
    static final ArchRule entitiesDoNotDependOnOtherLayers = noClasses()
            .that().resideInAPackage(ENTITIES)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLERS, SERVICES, REPOSITORIES);
}
