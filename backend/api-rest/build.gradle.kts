plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
    // Spring & JPA plugins not re-applied here to avoid duplicate classpath plugin version conflict
}

group = "com.legistrack"
version = "1.0.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":persistence-jpa"))
    implementation(project(":external-congress-adapter"))
    implementation(project(":external-ollama-adapter"))
    implementation(project(":ingestion"))
    implementation(project(":ai-analysis"))

    implementation(libs.bundles.spring.starters)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.db)
    runtimeOnly(libs.postgresql)
    implementation(libs.bundles.redis)
    implementation(libs.bundles.http.client)

    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.spring.boot.config.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.withType<Test> { useJUnitPlatform() }

// BootJar is enabled here; this is the executable module.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("legistrack-backend.jar")
}

