package com.gamopanel.mobile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

// ---------------------------------------------------------------
// DTOs: forma del JSON que devuelve el servidor GamoPanel.
// Ambos endpoints son PÚBLICOS (no requieren sesión) en 0.6.x.
// ---------------------------------------------------------------

/** Respuesta de GET /api/health -> {"ok":true}. */
@Serializable
private data class HealthDto(val ok: Boolean = false)

/** Respuesta de GET /api/setup/status -> {"configured":true,"version":"0.6.1"}. */
@Serializable
private data class SetupStatusDto(val configured: Boolean = false, val version: String? = null)

/** Estados posibles de la pantalla de conexión (la UI hace `when` sobre esto). */
sealed interface ConnectStatus {
    /** Aún no se ha probado nada. */
    data object Idle : ConnectStatus

    /** Hay una prueba de conexión en curso. */
    data object Testing : ConnectStatus

    /** Panel alcanzable. [configured]=false significa que falta registrar al propietario. */
    data class Ok(val baseUrl: String, val version: String?, val configured: Boolean) : ConnectStatus

    /** Fallo con mensaje legible para el usuario. */
    data class Error(val message: String) : ConnectStatus
}

/** Cliente HTTP mínimo del panel (solo lo necesario para la pantalla de conexión). */
class PanelClient {

    // Cliente Ktor: el motor se elige solo por plataforma (OkHttp en Android, Darwin en iOS).
    private val http = HttpClient {
        // Convierte el JSON en objetos Kotlin; ignora campos que no conocemos.
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        // Tiempos máximos para no dejar la UI esperando eternamente.
        install(HttpTimeout) {
            connectTimeoutMillis = 6_000
            requestTimeoutMillis = 10_000
        }
    }

    /**
     * Prueba que [rawUrl] sea un GamoPanel alcanzable.
     * Nunca lanza excepciones (salvo cancelación): siempre devuelve un [ConnectStatus].
     */
    suspend fun probe(rawUrl: String): ConnectStatus {
        // 1) Normalizar y validar la URL escrita por el usuario.
        val text = rawUrl.trim().trimEnd('/')
        if (text.isEmpty()) return ConnectStatus.Error("Escribe la dirección de tu panel.")
        // Sin esquema -> asumir https. Con http:// -> rechazar (iOS ATS y cookies Secure lo exigen).
        val base = when {
            text.startsWith("https://", ignoreCase = true) -> text
            text.contains("://") -> return ConnectStatus.Error("Solo se admite HTTPS (https://…).")
            else -> "https://$text"
        }

        return try {
            // 2) /api/health: confirma que el servidor y su base de datos responden.
            val health = http.get("$base/api/health")
            if (!health.status.isSuccess()) {
                return ConnectStatus.Error("El servidor respondió HTTP ${health.status.value}.")
            }
            if (!health.body<HealthDto>().ok) {
                return ConnectStatus.Error("El panel reporta un fallo interno.")
            }
            // 3) /api/setup/status: versión y si el propietario ya fue configurado.
            val setup = http.get("$base/api/setup/status")
            if (!setup.status.isSuccess()) {
                return ConnectStatus.Error("No se pudo leer el estado del panel (HTTP ${setup.status.value}).")
            }
            val info = setup.body<SetupStatusDto>()
            ConnectStatus.Ok(baseUrl = base, version = info.version, configured = info.configured)
        } catch (e: CancellationException) {
            throw e // La cancelación de corrutinas debe propagarse siempre.
        } catch (e: Exception) {
            // DNS, TLS, timeout, o respuesta que no es JSON de GamoPanel (p. ej. página de Cloudflare).
            ConnectStatus.Error("No se pudo conectar: ${e.message ?: e::class.simpleName}")
        }
    }

    /** Libera el motor HTTP (se llama al salir de la pantalla). */
    fun close() = http.close()
}
