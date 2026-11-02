// Build raíz: solo declara los plugins (sin aplicarlos) para que
// todos los módulos usen la MISMA versión definida en libs.versions.toml.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}
