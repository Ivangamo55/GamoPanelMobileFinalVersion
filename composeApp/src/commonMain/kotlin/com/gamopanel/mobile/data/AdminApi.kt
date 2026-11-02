package com.gamopanel.mobile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*

/** Cliente de la API real de GamoPanel Next 0.6.1. Cookies sólo en memoria. */
class AdminApi {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val http = HttpClient {
        install(ContentNegotiation) { json(this@AdminApi.json) }
        install(HttpCookies) { storage = AcceptAllCookiesStorage() }
        install(HttpTimeout) { connectTimeoutMillis = 8_000; requestTimeoutMillis = 25_000; socketTimeoutMillis = 25_000 }
    }
    var baseUrl: String = "https://panel.gamoserver.space"; private set
    private var csrf: String? = null
    /** true si la sesión se abrió con código de un solo uso (sin passkey): el step-up también usa código. */
    var codeSession: Boolean = false; private set
    fun connect(url: String) { baseUrl = url.trim().trimEnd('/') }

    suspend fun health() = request("GET", "/api/health")
    suspend fun setupStatus() = request("GET", "/api/setup/status")
    suspend fun authOptions(mode: String = "any") = request("POST", "/api/auth/options", buildJsonObject { put("mode", mode) })
    suspend fun verifyAuth(ceremony: String, assertion: JsonElement) = request("POST", "/api/auth/verify", buildJsonObject { put("ceremony", ceremony); put("response", assertion) }).also { updateCsrf(it); codeSession = false }
    suspend fun codeLogin(code: String) = request("POST", "/api/auth/code-login", buildJsonObject { put("code", code) }).also { updateCsrf(it); codeSession = true }
    suspend fun codeStepUp(code: String) = request("POST", "/api/auth/code-step-up", buildJsonObject { put("code", code) }, csrfRequired = true)
    suspend fun me() = request("GET", "/api/auth/me").also { updateCsrf(it) }
    suspend fun stepUpOptions() = request("POST", "/api/auth/step-up/options")
    suspend fun verifyStepUp(ceremony: String, assertion: JsonElement) = request("POST", "/api/auth/step-up/verify", buildJsonObject { put("ceremony", ceremony); put("response", assertion) }, csrfRequired = true) // el servidor exige x-csrf-token aquí
    suspend fun elevate(assertion: JsonElement, ceremony: String) = verifyStepUp(ceremony, assertion)
    suspend fun logout() { request("POST", "/api/auth/logout", csrfRequired = true); csrf = null; codeSession = false }

    suspend fun overview() = get("/api/system/overview")
    suspend fun disks() = get("/api/system/disks")
    suspend fun history() = get("/api/system/history")
    suspend fun processes() = get("/api/system/processes")
    suspend fun containers() = get("/api/docker/containers")
    suspend fun projects() = get("/api/docker/projects")
    suspend fun images() = get("/api/docker/images")
    suspend fun volumes() = get("/api/docker/volumes")
    suspend fun networks() = get("/api/docker/networks")
    suspend fun dockerTop() = get("/api/docker/top")
    suspend fun reclaimable() = get("/api/docker/reclaimable")
    suspend fun services() = get("/api/services")
    suspend fun security() = get("/api/security/status")
    suspend fun updates() = get("/api/updates/status")
    suspend fun updateJobs() = get("/api/updates/jobs")
    suspend fun notifications() = get("/api/notifications")
    suspend fun notificationChannels() = get("/api/notifications/channels")
    suspend fun audit() = get("/api/audit")
    suspend fun stacks() = get("/api/stacks")
    suspend fun fileRoots() = get("/api/files/roots")
    suspend fun terminalStatus() = get("/api/terminal/status")
    suspend fun passkeys() = get("/api/auth/passkeys")
    suspend fun sessions() = get("/api/auth/sessions")

    suspend fun containerAction(id: String, action: String) = request("POST", "/api/docker/containers/${id}/$action", csrfRequired = true)
    suspend fun serviceAction(name: String, action: String) = request("POST", "/api/services/${name}/$action", csrfRequired = true)
    suspend fun markAllRead() = request("POST", "/api/notifications/read-all", csrfRequired = true)
    suspend fun markRead(id: String) = request("POST", "/api/notifications/${id}/read", csrfRequired = true)
    suspend fun dismissNotification(id: String) = request("DELETE", "/api/notifications/${id}", csrfRequired = true)
    suspend fun revokeOtherSessions() = request("POST", "/api/auth/sessions/revoke-others", csrfRequired = true)
    suspend fun createTerminalTicket() = request("POST", "/api/terminal/ticket", csrfRequired = true)

    private suspend fun get(path: String) = request("GET", path)
    private suspend fun request(method: String, path: String, body: JsonElement? = null, csrfRequired: Boolean = false): JsonElement {
        val response = http.request("$baseUrl$path") {
            this.method = io.ktor.http.HttpMethod.parse(method)
            header("Accept", "application/json"); header("Cache-Control", "no-store")
            if (csrfRequired) header("x-csrf-token", csrf ?: throw ApiException("La sesión no tiene token CSRF. Vuelve a iniciar sesión."))
            if (body != null) { contentType(ContentType.Application.Json); setBody(body) }
        }
        val text = response.body<String>()
        val parsed = runCatching { json.parseToJsonElement(text) }.getOrElse { JsonPrimitive(text) }
        if (!response.status.isSuccess()) {
            val message = (parsed as? JsonObject)?.get("error")?.jsonPrimitive?.contentOrNull
            throw ApiException(message ?: "HTTP ${response.status.value}", response.status.value)
        }
        return parsed
    }
    private fun updateCsrf(element: JsonElement) { csrf = (element as? JsonObject)?.get("csrf")?.jsonPrimitive?.contentOrNull }
    fun close() = http.close()
}
class ApiException(message: String, val status: Int? = null) : Exception(message)
