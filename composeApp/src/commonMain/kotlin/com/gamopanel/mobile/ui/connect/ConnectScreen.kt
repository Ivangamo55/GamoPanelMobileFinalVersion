package com.gamopanel.mobile.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamopanel.mobile.data.ConnectStatus
import com.gamopanel.mobile.data.PanelClient
import com.gamopanel.mobile.ui.theme.GlassColors
import com.gamopanel.mobile.ui.theme.GlassShapes
import com.gamopanel.mobile.ui.theme.LiquidBackground
import com.gamopanel.mobile.ui.theme.liquidGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * Pantalla principal: conectar con el servidor GamoPanel.
 *
 * @param onConnected se invoca al pulsar "Continuar" con la URL base validada y
 *                    `setupRequired` = true si el panel aún no tiene propietario.
 */
@Composable
fun ConnectScreen(
    onConnected: (baseUrl: String, setupRequired: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cliente HTTP y estado viven mientras la pantalla esté en composición.
    val client = remember { PanelClient() }
    DisposableEffect(client) { onDispose { client.close() } } // Cierra el motor al salir.
    val scope = rememberCoroutineScope()
    val state = remember { ConnectState(client, scope) }
    // Estado de Haze compartido entre el fondo (fuente) y los paneles (efecto).
    val hazeState = rememberHazeState()

    LiquidBackground(hazeState = hazeState, modifier = modifier) {
        // BoxWithConstraints nos da el ancho real: móvil (<600dp) vs iPad (>=600dp).
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // Respeta recorte de cámara (Honor 90), Dynamic Island (iPhone 16 Pro Max),
                // barras del sistema, esquinas del iPad y teclado (ime).
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val wide = maxWidth >= 600.dp // iPad 9 (810 pt vertical) cae aquí; teléfonos no.
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)           // En tablet el panel no se estira.
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Scroll si el teclado/horizontal lo exige.
                    .padding(horizontal = if (wide) 0.dp else 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ----- Encabezado -----
                Text(
                    text = "GamoPanel",
                    color = Color.White,
                    fontSize = if (wide) 44.sp else 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Conecta con tu servidor",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = if (wide) 18.sp else 16.sp,
                )

                // ----- Tarjeta de cristal con el formulario -----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(hazeState)  // <- aquí se aplica el efecto Liquid Glass
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    UrlField(state)
                    StatusRow(state.status)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Botón secundario: probar la conexión.
                        GlassButton(
                            text = "Probar conexión",
                            hazeState = hazeState,
                            enabled = state.url.isNotBlank() && state.status !is ConnectStatus.Testing,
                            onClick = state::test,
                            modifier = Modifier.weight(1f),
                        )
                        // Botón principal: solo se activa si la prueba fue exitosa.
                        val ok = state.status as? ConnectStatus.Ok
                        GlassButton(
                            text = "Continuar",
                            hazeState = hazeState,
                            enabled = ok != null,
                            accent = true,
                            onClick = { ok?.let { onConnected(it.baseUrl, !it.configured) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ----- Pie informativo -----
                Text(
                    text = "Acceso con passkey · conexión cifrada (HTTPS)",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** Campo de texto para la URL, con teclado de direcciones y acción "Ir". */
@Composable
private fun UrlField(state: ConnectState) {
    OutlinedTextField(
        value = state.url,
        onValueChange = state::onUrlChange,
        label = { Text("URL del panel") },
        placeholder = { Text("https://panel.midominio.com") },
        singleLine = true,
        shape = GlassShapes.Control,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,                   // Teclado con "/" y ".com".
            imeAction = ImeAction.Go,                          // Tecla "Ir" en el teclado.
            capitalization = KeyboardCapitalization.None,      // Sin mayúscula automática.
            autoCorrectEnabled = false,                        // Sin autocorrección en URLs.
        ),
        keyboardActions = KeyboardActions(onGo = { state.test() }), // "Ir" prueba la conexión.
        // Colores para fondo oscuro/vidrio (texto blanco, borde translúcido, foco cian).
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = GlassColors.Accent,
            focusedBorderColor = GlassColors.Accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            focusedLabelColor = GlassColors.Accent,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Línea de estado: punto de color (o spinner) + texto según [ConnectStatus]. */
@Composable
private fun StatusRow(status: ConnectStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Indicador a la izquierda: spinner si está probando, punto de color en los demás casos.
        if (status is ConnectStatus.Testing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = GlassColors.Accent,
            )
        } else {
            val dot = when (status) {
                is ConnectStatus.Ok -> GlassColors.Ok
                is ConnectStatus.Error -> GlassColors.Bad
                else -> Color.White.copy(alpha = 0.35f) // Idle: gris neutro.
            }
            Spacer(Modifier.size(10.dp).background(dot, CircleShape))
        }
        // Texto descriptivo del estado.
        val label = when (status) {
            ConnectStatus.Idle -> "Introduce la dirección de tu GamoPanel."
            ConnectStatus.Testing -> "Probando conexión…"
            is ConnectStatus.Ok -> buildString {
                append("Panel alcanzado")
                status.version?.let { append(" · v$it") }
                if (!status.configured) append(" · requiere configuración inicial")
            }
            is ConnectStatus.Error -> status.message
        }
        Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

/**
 * Botón de cristal. Altura mínima 52 dp: cómodo para dedo y para Apple Pencil
 * (el stylus llega a Compose como un puntero normal, sin código especial).
 */
@Composable
private fun GlassButton(
    text: String,
    hazeState: HazeState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false, // true = tinte cian (acción principal).
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = GlassShapes.Control,
        // Contenedor transparente: el "vidrio" lo dibuja liquidGlass, no el Button.
        // Mantener el modificador clickable nativo de Material sin Haze encima.
        // Haze sigue aplicado a la tarjeta contenedora; no se aplica directamente
        // al botón para evitar interferencias con el gesto táctil en iOS.
        colors = ButtonDefaults.buttonColors(
            containerColor = if (accent && enabled) GlassColors.Accent.copy(alpha = 0.32f)
                else Color.White.copy(alpha = 0.10f),
            contentColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContentColor = Color.White.copy(alpha = 0.4f),
        ),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(text = text, maxLines = 1, fontWeight = FontWeight.SemiBold)
    }
}
