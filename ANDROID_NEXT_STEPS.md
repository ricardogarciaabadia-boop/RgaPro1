# Android — siguiente integración

La app Android actual muestra el prototipo y usa BiometricPrompt como puerta de entrada.

Para producción:
1. Sustituir WebView/local prototype por una UI Android nativa o WebView empaquetada con recursos locales.
2. Guardar el token y claves locales en Android Keystore/EncryptedSharedPreferences.
3. Conectar al endpoint HTTPS del backend privado.
4. Añadir sincronización incremental.
5. Añadir OCR local y cámara.
6. Guardar documentos cifrados localmente y sincronizarlos cifrados.
7. Implementar notificaciones de renovaciones/citas.
8. Implementar revocación de dispositivos y cierre de sesiones.
