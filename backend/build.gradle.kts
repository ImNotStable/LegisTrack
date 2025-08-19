/*
 * Copyright (c) 2025 LegisTrack
 *
 * Licensed under the MIT License. You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.kotlin.plugin.jpa)
    alias(libs.plugins.kotlin.plugin.allopen)
    alias(libs.plugins.kotlin.plugin.noarg)
    alias(libs.plugins.ktlint)
    // Detekt plugin intentionally disabled: 1.23.8 built with Kotlin 2.0.21 → incompatible with project Kotlin 2.1.0 (build failure on task :detekt)
    // Leave version in catalog; re-enable once a Detekt release compiled against Kotlin 2.1.x is available.
    id("com.diffplug.spotless") version "6.25.0" // tooling plugin explicit (not in version catalog yet)
    id("com.github.ben-manes.versions") version "0.52.0" // dependency update audit
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

// Make root backend project a pure aggregator: exclude its own source directories so
// legacy code (controllers, services, DTOs, etc.) is no longer compiled. Physical
// deletion is handled separately; this guarantees the build ignores any lingering files.
sourceSets {
    named("main") {
        kotlin.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
    named("test") {
        kotlin.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    // Root project has no production sources; keep test dependencies for potential aggregate tests/ArchUnit
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Detekt configuration block removed due to incompatibility (see plugin comment above)

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

    // Disable executable packaging at root (handled in api-rest module)
    withType<org.springframework.boot.gradle.tasks.bundling.BootJar> { enabled = false }
    jar { enabled = false }

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


    // Aggregate checks (detekt excluded until Kotlin 2.1 compatible release ships)
    named("check") {
        dependsOn("ktlintCheck")
    }

    // Show dependency updates (reject non-stable if current is stable)
    register<DependencyUpdatesTask>("dependencyUpdatesFiltered") {
        rejectVersionIf {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) }
            // Escape digit class correctly for Kotlin string
            val regex = "^(?!.*(alpha|beta|rc|cr|m|preview|b\\d)).*".toRegex(RegexOption.IGNORE_CASE)
            val isStable = stableKeyword || candidate.version.matches(regex)
            val currentStable = stableKeyword || currentVersion.matches(regex)
            currentStable && !isStable
        }
        checkForGradleUpdate = true
        outputFormatter = "plain"
    }
}

// Detekt remains disabled: last attempt (1.23.8) failed with message:
// "detekt was compiled with Kotlin 2.0.21 but is currently running with 2.1.0. This is not supported." 
// Action: Monitor Detekt release notes; re-enable when build uses Kotlin 2.1.

// Remove duplicate check dependency declarations

// GraalVM Native Image configuration
// Native image configuration relocated to api-rest module (future Phase 7 common-infra consolidation)

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

// Ktlint configuration (re-enabled)
ktlint {
    filter {
        exclude("**/*.gradle.kts") // keep build scripts out of style checks
    }
}

// Spotless for license headers & formatting (focus on header only to reduce churn)
spotless {
    ratchetFrom("origin/main")
    kotlin {
        target("**/src/**/*.kt")
        licenseHeaderFile(rootProject.file("config/spotless/license-header.txt"))
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        licenseHeaderFile(rootProject.file("config/spotless/license-header.txt"), "(plugins|import|// root)")
    }
    format("misc") {
        target("**/*.md", "**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("check") { dependsOn("spotlessCheck") }

// AOT related tasks removed from root after api-rest extraction
