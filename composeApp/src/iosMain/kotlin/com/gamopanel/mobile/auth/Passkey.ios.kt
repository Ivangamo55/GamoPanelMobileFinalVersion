package com.gamopanel.mobile.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSNotification
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun performPasskey(optionsJson: String, relyingPartyId: String): String = suspendCancellableCoroutine { continuation ->
    val center = NSNotificationCenter.defaultCenter
    var resultObserver: Any? = null
    resultObserver = center.addObserverForName(
        name = "GamoPasskeyResult",
        `object` = null,
        queue = NSOperationQueue.mainQueue
    ) { note: NSNotification? ->
        val info = note?.userInfo as? Map<*, *>
        val assertion = info?.get("assertionJson") as? String
        val error = info?.get("error") as? String
        resultObserver?.let { center.removeObserver(it) }
        if (!continuation.isActive) return@addObserverForName
        if (assertion != null) continuation.resume(assertion)
        else continuation.resumeWithException(IllegalStateException(error ?: "No se pudo completar la passkey."))
    }
    center.postNotificationName(
        "GamoPasskeyRequest",
        `object` = null,
        userInfo = mapOf("optionsJson" to optionsJson, "rpId" to relyingPartyId)
    )
    continuation.invokeOnCancellation { resultObserver?.let { center.removeObserver(it) } }
}
