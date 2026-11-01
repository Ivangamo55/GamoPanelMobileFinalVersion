package com.gamopanel.mobile.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var hostActivity: Activity? = null
fun attachPasskeyActivity(activity: Activity) { hostActivity = activity }
fun detachPasskeyActivity(activity: Activity) { if (hostActivity === activity) hostActivity = null }

actual suspend fun performPasskey(optionsJson: String, relyingPartyId: String): String = withContext(Dispatchers.Main) {
    val activity = hostActivity ?: error("No hay una Activity disponible para mostrar la passkey.")
    val manager = CredentialManager.create(activity)
    val option = GetPublicKeyCredentialOption(requestJson = optionsJson)
    val result = manager.getCredential(activity, GetCredentialRequest(listOf(option)))
    val credential = result.credential as? PublicKeyCredential
        ?: error("El proveedor no devolvió una credencial WebAuthn.")
    credential.authenticationResponseJson
}
