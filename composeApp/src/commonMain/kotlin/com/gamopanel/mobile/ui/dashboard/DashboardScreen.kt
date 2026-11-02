package com.gamopanel.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamopanel.mobile.auth.performPasskey
import com.gamopanel.mobile.data.AdminApi
import com.gamopanel.mobile.data.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private enum class Section(val title: String) {
    Overview("Resumen"), Docker("Contenedores"), Projects("Proyectos"), Images("Imágenes"), Volumes("Volúmenes"), Networks("Redes"), Services("Servicios"), Storage("Discos"), Processes("Procesos"), History("Historial"), Security("Seguridad"), Updates("Actualizaciones"), Notifications("Alertas"), Audit("Auditoría"), Files("Archivos"), Stacks("Stacks"), Terminal("Terminal"), Account("Cuenta")
}

@Composable
fun DashboardScreen(api: AdminApi, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(Section.Overview) }
    var result by remember { mutableStateOf<JsonElement?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var owner by remember { mutableStateOf("Propietario") }

    var codePrompt by remember { mutableStateOf<CompletableDeferred<String?>?>(null) }
    suspend fun elevate() {
        if (api.codeSession) {
            val ask = CompletableDeferred<String?>()
            codePrompt = ask
            val entered = ask.await()
            codePrompt = null
            if (entered.isNullOrBlank()) throw ApiException("Verificación cancelada")
            api.codeStepUp(entered)
            return
        }
        val root = api.stepUpOptions().jsonObject
        val ceremony = root["ceremony"]!!.jsonPrimitive.content
        val options = root["options"]!!
        val host = options.jsonObject["rpId"]?.jsonPrimitive?.contentOrNull
            ?: api.baseUrl.removePrefix("https://").substringBefore('/').substringBefore(':')
        val assertion = performPasskey(options.toString(), host)
        api.elevate(Json.parseToJsonElement(assertion), ceremony)
    }
    suspend fun loadSection(keepNotice: Boolean = false) {
        loading = true; error = null; if (!keepNotice) notice = null
        try {
            val me = api.me().jsonObject
            owner = me["owner"]?.jsonPrimitive?.contentOrNull ?: "Propietario"
            result = when (section) {
                Section.Overview -> api.overview()
                Section.Docker -> api.containers()
                Section.Projects -> api.projects()
                Section.Images -> api.images()
                Section.Volumes -> api.volumes()
                Section.Networks -> api.networks()
                Section.Services -> api.services()
                Section.Storage -> buildJsonObject { put("fs", api.overview().jsonObject["fs"] ?: JsonNull); put("disks", api.disks()) }
                Section.Processes -> api.processes()
                Section.History -> api.history()
                Section.Security -> api.security()
                Section.Updates -> api.updates()
                Section.Notifications -> api.notifications()
                Section.Audit -> api.audit()
                Section.Files -> api.fileRoots()
                Section.Stacks -> api.stacks()
                Section.Terminal -> api.terminalStatus()
                Section.Account -> buildJsonObject { put("passkeys", api.passkeys()); put("sessions", api.sessions()) }
            }
        } catch (e: ApiException) {
            error = when (e.status) { 401 -> "Sesión caducada o revocada. Vuelve a iniciar sesión."; 403, 428 -> e.message ?: "Se requiere una verificación adicional con passkey."; else -> e.message ?: "Error de API" }
        } catch (e: Exception) { error = e.message ?: "No se pudo cargar la información." }
        finally { loading = false }
    }
    codePrompt?.let { ask ->
        var text by remember(ask) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { ask.complete(null) },
            title = { Text("Verificación adicional") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Genera un código nuevo en el servidor: sudo gamopanel-app-code")
                    OutlinedTextField(value = text, onValueChange = { text = it.uppercase().take(14) }, singleLine = true, label = { Text("Código") })
                }
            },
            confirmButton = { TextButton(onClick = { ask.complete(text) }, enabled = text.filter { it.isLetterOrDigit() }.length >= 12) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { ask.complete(null) }) { Text("Cancelar") } }
        )
    }
    LaunchedEffect(section) { loadSection() }

    Column(modifier.fillMaxSize().background(Color(0xFF070B1A)).safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GamoPanel", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Next 0.6.1 · $owner", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
            }
            TextButton(onClick = { scope.launch { loadSection() } }, enabled = !loading) { Text("Actualizar") }
            TextButton(onClick = { scope.launch { runCatching { api.logout() }; onLogout() } }) { Text("Salir") }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Section.entries.forEach { item -> FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.title) }) }
        }
        Spacer(Modifier.height(10.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00D8F5))
        error?.let { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF512532)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text(it, color = Color.White, modifier = Modifier.padding(14.dp)) } }
        notice?.let { Text(it, color = Color(0xFF7CE6C0), modifier = Modifier.padding(8.dp)) }
        result?.let { data ->
            Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Color(0xCC18243A)), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(section.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    when (section) {
                        Section.Docker -> ContainerList(data, onAction = { id, action -> scope.launch { runCatching { elevate(); api.containerAction(id, action); notice = "Acción $action enviada a $id"; loadSection(true) }.onFailure { error = it.message } } })
                        Section.Services -> ServiceList(data, onAction = { name, action -> scope.launch { runCatching { elevate(); api.serviceAction(name, action); notice = "Acción $action enviada a $name"; loadSection(true) }.onFailure { error = it.message } } })
                        Section.Notifications -> NotificationList(data, onReadAll = { scope.launch { runCatching { api.markAllRead(); loadSection() }.onFailure { error = it.message } } })
                        Section.Overview -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StorageBars((data as? JsonObject)?.get("fs")) // barras de uso por disco
                            JsonViewer(data)
                        }
                        Section.Storage -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val obj = data as? JsonObject
                            StorageBars(obj?.get("fs"))
                            JsonViewer(obj?.get("disks") ?: data) // datos SMART
                        }
                        Section.Terminal -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JsonViewer(data)
                            Button(onClick = { scope.launch { runCatching { elevate(); api.createTerminalTicket(); notice = "Ticket de terminal creado. La conexión interactiva requiere el cliente WebSocket." }.onFailure { error = it.message } } }) { Text("Autorizar terminal (passkey)") }
                            Text("La terminal remota requiere un canal WebSocket seguro y ticket de un solo uso. No se ejecutan comandos desde esta vista.", color = Color(0xFFB7C6DA), fontSize = 12.sp)
                        }
                        Section.Account -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JsonViewer(data)
                            OutlinedButton(onClick = { scope.launch { runCatching { elevate(); api.revokeOtherSessions(); notice = "Se solicitó revocar las demás sesiones." }.onFailure { error = it.message } } }) { Text("Revocar otras sesiones") }
                        }
                        else -> JsonViewer(data)
                    }
                }
            }
        }
    }
}

@Composable private fun ContainerList(data: JsonElement, onAction: (String, String) -> Unit) {
    val items = (data as? JsonArray).orEmpty()
    if (items.isEmpty()) Text("No hay contenedores.", color = Color.LightGray)
    items.forEach { item ->
        val obj = item as? JsonObject ?: return@forEach
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id.take(12)
        val state = obj["state"]?.jsonPrimitive?.contentOrNull ?: "desconocido"
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF22324A)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text(state, color = if (state == "running") Color(0xFF71E0B2) else Color(0xFFFFC17D), fontSize = 12.sp) }
                Text(obj["image"]?.jsonPrimitive?.contentOrNull ?: "", color = Color(0xFF9FB2CB), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(if (state == "running") "stop" else "start", "restart").forEach { action -> OutlinedButton(onClick = { onAction(id, action) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)) { Text(action, fontSize = 11.sp) } }
                }
            }
        }
    }
}

@Composable private fun ServiceList(data: JsonElement, onAction: (String, String) -> Unit) {
    (data as? JsonArray).orEmpty().forEach { item ->
        val obj = item as? JsonObject ?: return@forEach
        val name = obj["Id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF22324A)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${obj["ActiveState"]?.jsonPrimitive?.contentOrNull ?: "?"} · ${obj["SubState"]?.jsonPrimitive?.contentOrNull ?: "?"}", color = Color(0xFF9FB2CB), fontSize = 12.sp)
                Row { listOf("start", "stop", "restart").forEach { action -> TextButton(onClick = { onAction(name, action) }, enabled = obj["manageable"]?.jsonPrimitive?.booleanOrNull != false) { Text(action) } } }
            }
        }
    }
}

@Composable private fun NotificationList(data: JsonElement, onReadAll: () -> Unit) {
    val items = when (data) { is JsonArray -> data; is JsonObject -> data["items"] as? JsonArray ?: data["notifications"] as? JsonArray ?: JsonArray(emptyList()); else -> JsonArray(emptyList()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onReadAll) { Text("Marcar todas como leídas") } }
    items.forEach { item ->
        val obj = item as? JsonObject ?: return@forEach
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF22324A)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(obj["title"]?.jsonPrimitive?.contentOrNull ?: "Notificación", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(obj["message"]?.jsonPrimitive?.contentOrNull ?: "", color = Color(0xFFB7C6DA), fontSize = 13.sp)
            }
        }
    }
}

@Composable private fun JsonViewer(data: JsonElement) {
    Text(data.toString().replace(",", ",\n").replace("{", "{\n").replace("}", "\n}"), color = Color(0xFFE4ECF8), fontSize = 12.sp, lineHeight = 18.sp)
}

/** Barras de uso por punto de montaje (campo `fs` de /api/system/overview: mount,size,used,use). */
@Composable private fun StorageBars(fs: JsonElement?) {
    (fs as? JsonArray).orEmpty().forEach { item ->
        val o = item as? JsonObject ?: return@forEach
        val mount = o["mount"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        val size = o["size"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val used = o["used"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val reported = o["use"]?.jsonPrimitive?.doubleOrNull
        val pct = (reported ?: (if (size > 0) used / size * 100 else 0.0)).coerceIn(0.0, 100.0)
        val bar = if (pct >= 90) Color(0xFFFF6B6B) else if (pct >= 75) Color(0xFFFFC17D) else Color(0xFF00D8F5)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(mount, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${fmtBytes(used)} / ${fmtBytes(size)} · ${pct.toInt()}%", color = Color(0xFFB7C6DA), fontSize = 12.sp)
            }
            LinearProgressIndicator(progress = { (pct / 100).toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp), color = bar, trackColor = Color.White.copy(alpha = 0.12f))
        }
    }
}

/** Bytes -> texto legible (KB, MB, GB, TB). */
private fun fmtBytes(bytes: Double): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var v = bytes; var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "${(v * 10).toLong() / 10.0} ${units[i]}"
}
