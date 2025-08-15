pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id(libs.plugins.foojay.resolver.convention.get().pluginId) version libs.versions.foojay.get()
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "legistrack-backend"

// Keep settings lean for stability
