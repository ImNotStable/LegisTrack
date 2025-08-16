package com.legistrack

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for ensuring modular architecture boundaries.
 *
 * During Phase 0, these tests capture the current monolithic state
 * as a baseline. In later phases, these rules will be enforced
 * to prevent architectural violations.
 */
@AnalyzeClasses(packages = ["com.legistrack"])
class ArchitectureTest {

    // Phase 0: Baseline rules - currently commented out, will be enabled in later phases
    
    // @ArchTest - Enable in Phase 9
    // val noCyclesRule: ArchRule = slices()
    //     .matching("com.legistrack.(*)")
    //     .should().beFreeOfCycles()

    // @ArchTest - Enable in Phase 9  
    // val layeredArchitectureRule: ArchRule = layeredArchitecture()
    //     .consideringOnlyDependenciesInLayers()
    //     .layer("Domain").definedBy("..domain..")
    //     .layer("Port").definedBy("..port..")
    //     .layer("Service").definedBy("..service..")
    //     .layer("Repository").definedBy("..repository..")
    //     .layer("Controller").definedBy("..controller..")
    //     .layer("Config").definedBy("..config..")
    //     .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Repository", "Port")
    //     .whereLayer("Port").mayOnlyBeAccessedByLayers("Service")
    //     .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
    //     .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
    //     .whereLayer("Controller").mayOnlyBeAccessedByLayers()

    // @ArchTest - Enable in Phase 2
    // val noJpaOutsidePersistenceRule: ArchRule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
    //     .that().resideOutsideOfPackage("..persistence..")
    //     .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")

    // @ArchTest - Enable in Phase 2  
    // val noSpringDataJpaOutsidePersistenceRule: ArchRule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
    //     .that().resideOutsideOfPackage("..persistence..")
    //     .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.jpa..")

    /**
     * Baseline test to capture current architecture state.
     * This test documents the current monolithic structure and will pass
     * during Phase 0 to establish a known good state.
     */
    @Test
    fun `baseline - capture current architecture state`() {
        val classes: JavaClasses = ClassFileImporter().importPackages("com.legistrack")
        
        // Document current package structure
        val packages = classes.map { it.packageName }.distinct().sorted()
        println("=== Phase 0 Architecture Baseline ===")
        println("Current packages:")
        packages.forEach { println("  - $it") }
        
        // Document current class count by package
        val classCounts = classes.groupBy { it.packageName }
            .mapValues { it.value.size }
            .toSortedMap()
        
        println("\nClass counts by package:")
        classCounts.forEach { (pkg, count) -> 
            println("  - $pkg: $count classes")
        }
        
        println("\nTotal classes: ${classes.size}")
        println("=== End Baseline ===")
        
        // This test always passes - it's for documentation only
        assert(classes.isNotEmpty()) { "Should have classes to analyze" }
    }
}