pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// USING explicit version here because version catalogs are not yet available at settings script compilation
// time inside minimal Docker build context. This is a documented, single-version exception to the "no raw versions"
// rule to restore build operability; catalog still governs all dependency/plugin versions in build scripts.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "legistrack-backend"

// Phase 1: Multi-module structure
include("core-domain")

// Keep settings lean for stability
