# RgaPro — carteras individuales y compartición controlada

## Regla principal
Cada usuario tiene su propia cartera. Por defecto:
- Solo el propietario puede ver y gestionar sus clientes.
- La instalación de la app no concede acceso a ninguna cartera.
- El administrador de licencias tampoco obtiene automáticamente acceso a los clientes.

## Compartir con compañeros
El propietario puede seleccionar un cliente y pulsar:
**Compartir → Compañero autorizado → Permisos → Caducidad → Confirmar**

Permisos recomendados:
- `read`: consultar
- `write`: modificar
- `documents`: ver/añadir documentos
- `calendar`: gestionar citas/eventos
- `export`: exportar datos

El propietario puede revocar el acceso en cualquier momento.

## Ejemplo
Ana tiene 250 clientes.
- Juan no ve ninguno de Ana por defecto.
- Ana comparte el cliente "Empresa X" con Juan.
- Juan solo ve "Empresa X" y únicamente los permisos concedidos.
- Ana revoca el acceso y Juan deja de verlo.

## Seguridad
El control se aplica en el servidor, no solo ocultando botones de la interfaz.
Cada petición debe comprobar:
1. identidad del usuario,
2. propiedad del cliente o concesión vigente,
3. permiso requerido,
4. caducidad,
5. estado de la cuenta/dispositivo.

Antes de producción hay que añadir roles administrativos reales, MFA, auditoría,
rate limiting, pruebas de autorización (IDOR/BOLA), cifrado, backups y revisión RGPD/LOPDGDD.
