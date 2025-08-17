plugins {
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
}

group = "com.legistrack"
version = "1.0.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}") }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.bundles.http.client)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.kotlin)
    // Spring Boot core & caching support for annotations (@Component, @Cacheable, events)
    implementation(libs.bundles.spring.starters)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
}

tasks.withType<Test> { useJUnitPlatform() }
