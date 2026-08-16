# Control de accesos

Desde el panel de administración de Mi cartera Ocaso podrás:
- Crear códigos de activación individuales.
- Asociar cada código a una persona.
- Limitar el número de dispositivos.
- Establecer caducidad.
- Asignar permisos.
- Activar/desactivar licencias.
- Ver dispositivos autorizados.
- Revocar dispositivos.

Permisos:
clients.read, clients.write, clients.delete, policies.read, policies.write,
documents.scan, documents.read, budgets.read, budgets.write, calendar.read,
calendar.write, export.data, admin.users.

El servidor guarda un hash del código, no el código en texto plano.
Antes de producción se debe añadir un rol de administrador real, MFA obligatorio,
rate limiting, auditoría, rotación de secretos, gestión de sesiones y revisión
de seguridad/RGPD.
