import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// Plugins aplicados a este módulo (versiones vienen de libs.versions.toml).
plugins {
    alias(libs.plugins.kotlinMultiplatform)   // Kotlin Multiplatform
    alias(libs.plugins.androidApplication)    // App Android
    alias(libs.plugins.composeMultiplatform)  // Compose Multiplatform
    alias(libs.plugins.composeCompiler)       // Compilador de Compose
    alias(libs.plugins.kotlinSerialization)   // @Serializable para JSON
}

kotlin {
    // ---------- Target Android (Honor 90) ----------
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // ---------- Targets iOS (iPhone 16 Pro Max / iPad 9) ----------
    // XCFramework agrupa dispositivo + simulador en un solo paquete.
    val xcf = XCFramework("ComposeApp")
    // iosArm64 = iPhone/iPad reales; iosSimulatorArm64 = simulador en Mac Apple Silicon.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"   // Nombre que Swift importa: import ComposeApp
            isStatic = true           // Estático: requerido para enlazar fácil desde Swift
            xcf.add(this)             // Lo añade al XCFramework
        }
    }

    sourceSets {
        // ----- Código compartido por Android e iOS -----
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.haze)
        }
        // ----- Solo Android -----
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose) // setContent / enableEdgeToEdge
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.ktor.client.okhttp)        // Motor HTTP de Android
            implementation(libs.kotlinx.coroutines.android)
        }
        // ----- Solo iOS -----
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)        // Motor HTTP nativo de Apple (NSURLSession)
        }
    }
}

android {
    namespace = "com.gamopanel.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gamopanel.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()       // 31: RenderEffect disponible
        targetSdk = libs.versions.android.targetSdk.get().toInt() // 35 = Android 15 (Honor 90)
        versionCode = 61
        versionName = "0.6.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        // Evita conflictos de licencias duplicadas entre librerías.
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}
