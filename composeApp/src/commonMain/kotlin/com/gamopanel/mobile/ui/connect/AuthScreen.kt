package com.gamopanel.mobile.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun AuthScreen(api: AdminApi, onAuthenticated: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Color(0xFF070B1A)).safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("GamoPanel", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("Acceso seguro con passkey", color = Color(0xFF8EA4C4), modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))
        Card(Modifier.fillMaxWidth().widthIn(max = 480.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Autenticación del propietario", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Usa Face ID, Touch ID, una passkey guardada en iCloud o una llave compatible. La passkey se verifica en este dispositivo; la app nunca recibe tu contraseña.", color = Color(0xFFB8C7DB))
                Button(onClick = {
                    scope.launch {
                        busy = true; error = null
                        try {
                            val challenge = api.authOptions("any")
                            val root = challenge.jsonObject
                            if (root["setupRequired"]?.jsonPrimitive?.booleanOrNull == true) error = "El servidor aún no tiene propietario configurado. Completa primero el alta inicial desde GamoPanel web."
                            else {
                                val ceremony = root["ceremony"]!!.jsonPrimitive.content
                                val options = root["options"]!!
                                val host = options.jsonObject["rpId"]?.jsonPrimitive?.contentOrNull
                                    ?: api.baseUrl.removePrefix("https://").substringBefore('/').substringBefore(':')
                                val assertion = performPasskey(options.toString(), host)
                                api.verifyAuth(ceremony, Json.parseToJsonElement(assertion))
                                api.me()
                                onAuthenticated()
                            }
                        } catch (e: Exception) { error = e.message ?: "No se pudo autenticar." }
                        finally { busy = false }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Esperando passkey…" else "Continuar con passkey") }
                error?.let { Text(it, color = Color(0xFFFF9A9A), fontSize = 13.sp) }
                Text("Servidor: ${api.baseUrl}", color = Color(0xFF7890B2), fontSize = 11.sp)
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Cambiar servidor") }
    }
}
