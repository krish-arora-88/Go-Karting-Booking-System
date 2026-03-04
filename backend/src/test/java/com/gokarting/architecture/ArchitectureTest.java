package com.gokarting.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architectural fitness functions — enforced in CI as part of the test suite.
 *
 * These rules guarantee the hexagonal architecture stays intact as the codebase grows.
 * If a developer accidentally imports a Spring class into the domain layer,
 * this test fails immediately in the PR pipeline.
 */
@AnalyzeClasses(
        packages = "com.gokarting",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /** The domain layer must have zero dependencies on Spring, JPA, Kafka, or Redis. */
    @ArchTest
    static final ArchRule domainHasNoFrameworkDependencies =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.apache.kafka..",
                            "io.lettuce..",
                            "org.springframework.data.redis.."
                    )
                    .because("Domain is framework-agnostic — it only depends on pure Java");

    /** Inbound adapters (controllers, Kafka consumers) must not directly call outbound adapters. */
    @ArchTest
    static final ArchRule inboundAdaptersDoNotCallOutboundAdapters =
            noClasses()
                    .that().resideInAPackage("..adapter.in..")
                    .should().accessClassesThat()
                    .resideInAPackage("..adapter.out..")
                    .because("Inbound adapters must go through the application layer (use cases)");

    /** Application services must not depend on adapter implementations — only on port interfaces. */
    @ArchTest
    static final ArchRule applicationOnlyDependsOnDomainAndPorts =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..infrastructure.."
                    )
                    .because("Application services orchestrate domain logic and depend only on port interfaces");

    /**
     * Enforce layered dependency direction using ArchUnit's built-in layered architecture check.
     *
     * Note: we intentionally omit .consideringAllDependencies() so that dependencies on
     * third-party frameworks (Spring, JPA, Kafka, etc.) are not flagged — those are
     * expected and tested separately via domainHasNoFrameworkDependencies.
     * This rule only checks cross-layer dependencies within com.gokarting.*.
     */
    @ArchTest
    static final ArchRule layeredArchitectureIsRespected =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain")         .definedBy("..domain..")
                    .layer("Application")    .definedBy("..application..")
                    .layer("Adapters")       .definedBy("..adapter..")
                    .layer("Infrastructure") .definedBy("..infrastructure..")
                    .whereLayer("Domain")         .mayNotAccessAnyLayer()
                    .whereLayer("Application")    .mayOnlyAccessLayers("Domain")
                    .whereLayer("Adapters")       .mayOnlyAccessLayers("Application", "Domain", "Infrastructure")
                    .whereLayer("Infrastructure") .mayOnlyAccessLayers("Domain", "Application");
}
