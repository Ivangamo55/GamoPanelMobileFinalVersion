package com.gamopanel.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import com.gamopanel.mobile.data.AdminApi
import com.gamopanel.mobile.ui.connect.AuthScreen
import com.gamopanel.mobile.ui.connect.ConnectScreen
import com.gamopanel.mobile.ui.dashboard.DashboardScreen

/** Raíz de la app: conexión -> login con passkey -> panel. */
@Composable
fun App() {
    var connectedUrl by remember { mutableStateOf<String?>(null) }
    var authenticated by remember { mutableStateOf(false) }
    // UN solo AdminApi por servidor: conserva cookie de sesión y token CSRF entre login y panel.
    val api = remember(connectedUrl) { connectedUrl?.let { u -> AdminApi().also { it.connect(u) } } }
    DisposableEffect(api) { onDispose { api?.close() } }
    MaterialTheme(colorScheme = darkColorScheme()) {
        val current = api
        when {
            current == null -> ConnectScreen(onConnected = { baseUrl, _ -> connectedUrl = baseUrl; authenticated = false })
            !authenticated -> AuthScreen(api = current, onAuthenticated = { authenticated = true }, onBack = { connectedUrl = null })
            else -> DashboardScreen(api = current, onLogout = { authenticated = false })
        }
    }
}
