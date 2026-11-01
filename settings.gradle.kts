// Nombre del proyecto raíz (aparece en Android Studio).
rootProject.name = "GamoPanelMobile"

// Dónde Gradle busca los PLUGINS (AGP, Kotlin, Compose).
pluginManagement {
    repositories {
        // Google solo para grupos de Android/Google (acelera la resolución).
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Dónde Gradle busca las DEPENDENCIAS de la app (Ktor, Haze, Compose...).
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Único módulo: la app compartida (Android + iOS).
include(":composeApp")
