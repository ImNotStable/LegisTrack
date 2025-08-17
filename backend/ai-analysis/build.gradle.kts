plugins {
    // Library module: no Spring Boot plugin applied (only dependency management + Kotlin JVM)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// Import Spring Boot dependency management BOM (pattern consistent with external adapter modules)
dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}") }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.bundles.spring.starters)
    implementation(libs.bundles.kotlin)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
}
