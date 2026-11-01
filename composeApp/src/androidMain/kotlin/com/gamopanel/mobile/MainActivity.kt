package com.gamopanel.mobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gamopanel.mobile.auth.attachPasskeyActivity
import com.gamopanel.mobile.auth.detachPasskeyActivity

/** Actividad única de Android: aloja toda la UI de Compose. */
class MainActivity : ComponentActivity() {
    override fun onDestroy() {
        detachPasskeyActivity(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge: la app dibuja detrás de barras y recorte de cámara (Honor 90).
        // Barras transparentes con iconos claros (fondo oscuro). Va ANTES de super.onCreate.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        attachPasskeyActivity(this)
        // Carga la UI compartida.
        setContent { App() }
    }
}
