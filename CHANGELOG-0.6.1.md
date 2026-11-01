# GamoPanel Mobile 0.6.1

## Implementado en esta revisión
- Ceremonia de inicio de sesión WebAuthn conectada a `/api/auth/options` y `/api/auth/verify`.
- Integración Android con Credential Manager.
- Integración iOS/iPadOS con AuthenticationServices y passkeys de plataforma, comunicada con Compose mediante notificaciones nativas.
- Gestión de cookie HttpOnly y token CSRF en memoria.
- Step-up con passkey para acciones administrativas.
- Consulta de sistema, Docker, servicios, seguridad, actualizaciones, notificaciones, auditoría, archivos (raíces), stacks, terminal (estado) y cuenta.
- Acciones seguras start/stop/restart para contenedores y servicios.
- Proyecto XcodeGen, entitlement Associated Domains, workflow de macOS y bootstrap Gradle.

## Limitaciones explícitas
- No se ejecutó compilación Android/iOS en este entorno. Swift fue parseado sintácticamente; el framework Kotlin/iOS requiere macOS/Xcode.
- Requiere configurar AASA de Apple y Digital Asset Links de Android en el dominio, además de `RP_ID` y `ORIGIN` en el servidor.
- Terminal WebSocket interactiva y operaciones de archivos/actualizaciones avanzadas no están incluidas; se dejan fuera para no exponer acciones incompletas.
