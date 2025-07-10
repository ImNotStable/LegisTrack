pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
}

rootProject.name = "legistrack-backend"

// Build performance optimizations
gradle.startParameter.apply {
    // Enable parallel builds
    isParallelProjectExecutionEnabled = true

    // Configure build cache
    isBuildCacheEnabled = false

    // Optimize console output
    consoleOutput = ConsoleOutput.Plain
    logLevel = LogLevel.LIFECYCLE

    // Set max workers
    maxWorkerCount = Runtime.getRuntime().availableProcessors()
}

// Enable build features for performance
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Enhanced build cache configuration
gradle.settingsEvaluated {
    buildCache {
        local {
            isEnabled = true
            directory = File(settingsDir, "build/cache")
        }
        remote<HttpBuildCache> {
            isEnabled = false // Enable if you have a remote cache server
        }
    }
}
