pluginManagement {
    repositories {
        google()
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

rootProject.name = "Shopar SDK Demo Android"

includeBuild("../vendor/shopar-android-sdk") {
    dependencySubstitution {
        substitute(module("com.mobinova.shopar:agency-android")).using(project(":agency"))
        substitute(module("com.mobinova.shopar:discovery-android")).using(project(":discovery"))
        substitute(module("com.mobinova.shopar:shopar-contracts-android")).using(project(":contracts"))
    }
}

include(":app")
