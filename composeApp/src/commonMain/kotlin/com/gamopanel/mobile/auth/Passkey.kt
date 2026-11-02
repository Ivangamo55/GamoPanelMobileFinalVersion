package com.gamopanel.mobile.auth

/** Devuelve el objeto PublicKeyCredential de WebAuthn serializado como JSON. */
expect suspend fun performPasskey(optionsJson: String, relyingPartyId: String): String
