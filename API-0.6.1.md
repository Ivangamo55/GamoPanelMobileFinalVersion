# Contratos API revisados · GamoPanel Next 0.6.1

Fuente: `apps/server/src/auth.ts`, `apps/server/src/admin.ts`, `apps/server/src/notifications.ts`, `apps/server/src/operations.ts` y `apps/server/src/terminal.ts` del código fuente entregado.

## Autenticación
- `POST /api/auth/options` y `POST /api/auth/verify` para la ceremonia WebAuthn.
- `GET /api/auth/me` para comprobar sesión y obtener CSRF.
- `POST /api/auth/step-up/options` y `POST /api/auth/step-up/verify` para elevación reciente.
- `POST /api/auth/logout` con `x-csrf-token`.
- `GET /api/auth/passkeys` y `GET /api/auth/sessions` para consulta de cuenta.
- `POST /api/auth/sessions/revoke-others` para revocar sesiones adicionales (requiere step-up).

La cookie HttpOnly y el token CSRF se mantienen en el cliente HTTP en memoria; no se guardan en preferencias ni archivos.

## Lecturas administrativas conectadas
- Sistema: `/api/system/overview`, `/api/system/disks`, `/api/system/history`, `/api/system/processes`.
- Docker: `/api/docker/containers`, `/api/docker/projects`, `/api/docker/images`, `/api/docker/volumes`, `/api/docker/networks`.
- Servicios y seguridad: `/api/services`, `/api/security/status`.
- Operación: `/api/updates/status`, `/api/updates/jobs`, `/api/notifications`, `/api/notifications/channels`, `/api/audit`.
- Archivos y stacks: `/api/files/roots`, `/api/stacks`.
- Terminal: `/api/terminal/status`.

## Acciones conectadas
- `POST /api/docker/containers/:id/start|stop|restart`.
- `POST /api/services/:name/start|stop|restart`.
- `POST /api/notifications/read-all`.
- `POST /api/auth/sessions/revoke-others`.
- `POST /api/terminal/ticket` (sólo ticket de un uso; el canal WebSocket interactivo no está implementado en esta app).

Las acciones administrativas se preceden de la ceremonia step-up, según `requireElevated` del servidor. Las operaciones no implementadas se dejan fuera de la interfaz en lugar de exponer controles sin comportamiento.

## Fuera del alcance de esta entrega

El cliente muestra las raíces permitidas de archivos, pero todavía no implementa operaciones de lectura/escritura/subida. La terminal sólo consulta estado y obtiene el ticket; no abre el WebSocket ni proporciona emulación de terminal. La administración de actualizaciones, imágenes, volúmenes, redes, stacks y configuración de alertas permanece en modo consulta.
