pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "TypeType-TV"

include(":app")
include(":player")
include(":core:resources")
include(":core:data")
include(":core:playback")
include(":core:ui")
include(":core:testing")
include(":feature:auth")
include(":feature:home")
include(":feature:search")
include(":feature:library")
include(":feature:settings")
include(":feature:collections")
include(":feature:detail")
include(":feature:player")
