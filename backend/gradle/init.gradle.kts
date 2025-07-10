// Gradle initialization script for maximum performance
allprojects {
    // Repository optimization
    repositories {
        mavenCentral {
            content {
                excludeGroupByRegex("com\\.github\\..*")
            }
        }
    }

    // Task optimization
    tasks.withType<Test>().configureEach {
        // Optimize test execution
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        forkEvery = 100
        
        // Disable unnecessary reports for speed
        reports.html.required.set(false)
        reports.junitXml.required.set(false)
        
        // JVM optimization for tests
        jvmArgs(
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=100",
            "-XX:+UseStringDeduplication",
            "-Xmx2g",
            "-Xms1g"
        )
    }

    // Compilation optimization
    tasks.withType<JavaCompile>().configureEach {
        options.apply {
            isFork = true
            isIncremental = true
            forkOptions.apply {
                memoryMaximumSize = "2g"
                memoryInitialSize = "1g"
                jvmArgs = listOf(
                    "-XX:+UseG1GC",
                    "-XX:+UseStringDeduplication"
                )
            }
        }
    }

    // Kotlin compilation optimization
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xno-param-assertions",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions"
            )
        }
    }
}

// Global JVM optimization
gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        if (task is org.gradle.api.tasks.compile.JavaCompile) {
            task.options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        }
    }
}
