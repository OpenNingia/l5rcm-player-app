pluginManagement {
    // Reproducible builds (F-Droid): AGP 8.7.3 bundles R8 8.7.18, whose dex
    // partitioning can depend on the host CPU-core count, making classes*.dex
    // non-reproducible across build environments. Pin a newer R8 with the fix.
    buildscript {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools:r8:8.8.34")
        }
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "l5rcm-companion-android"
include(":app")
