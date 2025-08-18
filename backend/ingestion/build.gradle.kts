plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
}
// Library module; no executable bootJar
tasks.withType<Jar> { enabled = true }
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}") }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":persistence-jpa"))
    implementation(project(":external-congress-adapter"))
    implementation(project(":external-ollama-adapter"))
    implementation(libs.bundles.spring.starters)
    implementation(libs.bundles.kotlin)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

tasks.test {
    useJUnitPlatform()
}
