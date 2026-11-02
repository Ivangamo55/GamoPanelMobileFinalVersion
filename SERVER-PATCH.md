# Parche necesario en el servidor (GamoPanel Next 0.6.1)

Android (Credential Manager) firma `origin` como `android:apk-key-hash:<SHA-256 del certificado en base64url>`.
En `apps/server/src/auth.ts`, dentro de `verifyLogin`, cambia:

    expectedOrigin: config.origin,

por (un hash por cada certificado: debug y release):

    expectedOrigin: [config.origin, 'android:apk-key-hash:HASH_DEBUG', 'android:apk-key-hash:HASH_RELEASE'],

Obtener el hash (base64url sin padding): `keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl dgst -sha256 -binary | basenc --base64url | tr -d =`
iOS no requiere cambio si ORIGIN es `https://panel.gamoserver.space` sin puerto. Además: Associated Domains exige cuenta de Apple Developer de pago.
