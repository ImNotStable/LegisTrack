plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.kotlin.plugin.jpa)
    alias(libs.plugins.kotlin.plugin.allopen)
    alias(libs.plugins.kotlin.plugin.noarg)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.spring.boot.aot)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "com.legistrack"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation(libs.bundles.spring.starters)

    // JSON
    implementation(libs.bundles.jackson)

    // Kotlin Support
    implementation(libs.bundles.kotlin)

    // Database
    implementation(libs.bundles.db)
    runtimeOnly(libs.postgresql)

    // Redis
    implementation(libs.bundles.redis)

    // Reactive WebClient without enabling reactive web server
    implementation(libs.bundles.http.client)

    // Development
    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.spring.boot.config.processor)

    // Testing
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)
    testRuntimeOnly(libs.h2)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        jvmArgs("-XX:+EnableDynamicAgentLoading", "-XX:+UseG1GC", "-Xmx2g")
        systemProperty("spring.profiles.active", "test")
        testLogging {
            events("passed", "skipped", "failed")
        }
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        forkEvery = 100
        reports.html.required.set(false)
        reports.junitXml.required.set(false)
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xno-param-assertions",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Werror",
            )
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
            progressiveMode = true
            allWarningsAsErrors = true
            suppressWarnings = false
        }
    }

    bootJar {
        archiveFileName.set("${project.name}.jar")
        launchScript()
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    jar {
        enabled = false
    }

    // Build performance optimizations
    withType<JavaCompile> {
        options.isFork = true
        options.isIncremental = true
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:all",
                "-Xlint:-processing",
            ),
        )
    }

    // AOT processing optimization
    named("processAot") {
        doLast {
            // AOT processing completed
        }
    }

    // Aggregate checks
    named("check") {
        dependsOn("detekt", "ktlintCheck")
    }
}

// Detekt static analysis
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}

// Remove duplicate check dependency declarations

// GraalVM Native Image configuration
graalvmNative {
    binaries {
        named("main") {
            imageName.set("legistrack")
            mainClass.set("com.legistrack.LegisTrackApplicationKt")
            debug.set(false)
            verbose.set(false)
            fallback.set(false)
            sharedLibrary.set(false)
            quickBuild.set(false)
        }
    }
    toolchainDetection.set(false)
}

// AllOpen plugin configuration for Spring
allOpen {
    annotation("org.springframework.stereotype.Component")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.stereotype.Repository")
    annotation("org.springframework.web.bind.annotation.RestController")
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

// NoArg plugin configuration for JPA
noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// Build optimization configurations (keep defaults for dependency resolution to improve stability)

// Dependency management optimization
dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.bom.get()}")
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

// Build performance monitoring (intentionally minimal)
