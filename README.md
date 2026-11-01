# GamoPanel Mobile 0.6.1

Cliente multiplataforma para GamoPanel Next **0.6.1**, con Compose Multiplatform y una capa nativa de passkeys.

## Funciones incluidas

- Conexión HTTPS a `https://panel.gamoserver.space` (editable desde la pantalla de conexión).
- Login WebAuthn/passkey: Credential Manager en Android y AuthenticationServices en iOS/iPadOS.
- Sesión basada en cookie HttpOnly mantenida por Ktor en memoria; el token CSRF se conserva sólo en memoria y se envía en operaciones que lo exigen.
- Elevación de sesión con una segunda verificación passkey antes de operaciones privilegiadas.
- Vistas: resumen, contenedores, proyectos, imágenes, volúmenes, redes, servicios, discos, procesos, historial, seguridad, actualizaciones, alertas, auditoría, raíces de archivos, stacks, terminal (estado/ticket) y cuenta (passkeys/sesiones).
- Acciones para iniciar, detener y reiniciar contenedores y servicios; se exige step-up con passkey antes de ejecutarlas.
- Adaptación de navegación y panel de contenido para pantallas compactas y grandes, incluyendo iPhone y iPad.

## Contratos verificados en el código fuente del servidor 0.6.1

El cliente usa las rutas de `apps/server/src/auth.ts`, `admin.ts`, `notifications.ts` y `terminal.ts`. Las rutas de administración son protegidas por sesión; las acciones marcadas `requireElevated` sólo se ejecutan después de `/api/auth/step-up/options` y `/api/auth/step-up/verify`. No se implementan endpoints inventados.

La vista de terminal solicita un ticket de un solo uso después del step-up, pero no incluye aún una consola interactiva WebSocket. La vista Archivos muestra las raíces permitidas; no expone lectura/escritura ni subida de archivos en esta entrega. Los datos de vistas generales se muestran como JSON íntegro para conservar campos no conocidos por el cliente.

## Requisito de Android: Digital Asset Links

Para que Credential Manager pueda usar passkeys del RP desde la app Android, publica `/.well-known/assetlinks.json` en `panel.gamoserver.space`. Usa la plantilla `assetlinks.json.template`, sustituyendo la huella SHA-256 por la del certificado con el que firmarás la app (incluye las huellas de debug y release si vas a probar ambas). Mantén el `package_name` como `com.gamopanel.mobile` o actualiza el valor si cambias el applicationId.

## Requisito crítico de iOS: Associated Domains

Las passkeys de Apple requieren que el dominio RP publique un archivo `apple-app-site-association` válido y que el identificador de aplicación coincida con el equipo de firma. El proyecto añade el entitlement `webcredentials:panel.gamoserver.space` y usa el bundle ID `space.gamoserver.gamopanel` como valor inicial.

Antes de distribuir la app:

1. En Xcode, asigna tu Team de Apple y confirma el Bundle Identifier definitivo.
2. Publica `/.well-known/apple-app-site-association` en `panel.gamoserver.space`, con `webcredentials.apps` conteniendo `TEAMID.space.gamoserver.gamopanel` (sustituye `TEAMID` por tu Apple Team ID; hay una plantilla en `iosApp/apple-app-site-association.template`).
3. Confirma que el archivo responde por HTTPS, sin redirección, con `Content-Type: application/json`.
4. Configura en el servidor `RP_ID=panel.gamoserver.space` y `ORIGIN=https://panel.gamoserver.space` (el código 0.6.1 usa `localhost` como valor predeterminado de desarrollo). Reinicia el servicio.
5. Verifica que el `rpID` del servidor sea exactamente `panel.gamoserver.space` y que el origin WebAuthn configurado coincida con el origen web usado para enrolar las passkeys.

Si el RP ID/origin del servidor no coincide, Apple rechazará la assertion aunque Face ID funcione.

## Compilar Android

Abre la carpeta `GamoPanelMobile` en Android Studio con JDK 17 y Gradle 8.14.3 o superior. Ejecuta `:composeApp:assembleDebug`. Credential Manager requiere Google Play Services actualizado en el dispositivo o un proveedor de credenciales compatible.

## Compilar iOS/iPadOS

Se requiere macOS, Xcode 16 o superior y XcodeGen:

```bash
gradle :composeApp:assembleComposeAppReleaseXCFramework
cp -R composeApp/build/XCFrameworks/release/ComposeApp.xcframework iosApp/ComposeApp.xcframework
cd iosApp
xcodegen generate
open GamoPanel.xcodeproj
```

Selecciona tu Team de firma y ejecuta en iPhone/iPad. El workflow `.github/workflows/ios-framework.yml` prepara el XCFramework y compila un destino de simulador en GitHub Actions.

## Validación pendiente antes de producción

Este entorno de edición no dispone de macOS/Xcode ni de los dispositivos físicos; por ello no se declara una compilación iOS exitosa ni una prueba real de Face ID. La primera validación debe incluir login, cookie de sesión, logout, step-up, acción controlada de contenedor, expiración/revocación de sesión y prueba de passkeys con el AASA publicado. También deben probarse los layouts en iPhone 16 Pro Max y iPad 9.ª generación.
