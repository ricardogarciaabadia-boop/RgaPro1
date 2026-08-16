# RgaPro v10 — estado técnico

Correcciones aplicadas:
- Nombre de aplicación: RgaPro.
- applicationId/namespace: com.rgapro.app.
- Paquete Java renombrado.
- Biometría migrada a AndroidX Biometric 1.1.0 estable.
- Se permite biometría fuerte o credencial del dispositivo.
- WebView local restringido: sin file/content access.
- Workflow GitHub Actions sin necesidad de Gradle Wrapper; instala Gradle 8.13 y SDK Android 35.
- APK debug se publica como artifact RgaPro-APK.

Limitaciones que siguen siendo deliberadas:
- No es una versión de producción.
- El backend todavía necesita integración completa con la app.
- La autorización por cliente debe aplicarse a todas las operaciones de API antes de usar datos reales.
- OCR, calendario/notificaciones y sincronización completa requieren integración.
- Para Google Play se necesita AAB de release firmado y configuración de Play Console.
- Antes de producción: pruebas de seguridad, RGPD/LOPDGDD, backups cifrados, MFA y auditoría.

No introducir datos reales de clientes en el APK debug.
