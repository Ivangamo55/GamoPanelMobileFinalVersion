package com.gamopanel.mobile.ui.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gamopanel.mobile.data.ConnectStatus
import com.gamopanel.mobile.data.PanelClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de conexión (sin ViewModel para reducir dependencias).
 * Se crea con `remember` en la UI, así que sobrevive a recomposiciones.
 */
class ConnectState(
    private val client: PanelClient,   // Cliente HTTP que hace la prueba.
    private val scope: CoroutineScope, // Alcance ligado al ciclo de vida de la pantalla.
) {
    /** Texto del campo URL (observable por Compose). */
    var url by mutableStateOf("")
        private set

    /** Resultado actual de la prueba de conexión. */
    var status: ConnectStatus by mutableStateOf(ConnectStatus.Idle)
        private set

    // Trabajo en curso, para cancelarlo si el usuario vuelve a pulsar.
    private var job: Job? = null

    /** Se llama en cada tecla: guarda el texto y descarta resultados viejos. */
    fun onUrlChange(value: String) {
        url = value
        job?.cancel()
        status = ConnectStatus.Idle
    }

    /** Lanza la prueba de conexión contra la URL actual. */
    fun test() {
        job?.cancel()
        job = scope.launch {
            status = ConnectStatus.Testing
            status = client.probe(url)
        }
    }
}
