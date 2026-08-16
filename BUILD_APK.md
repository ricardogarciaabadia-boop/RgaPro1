# Generar el APK

Este entorno no dispone del Android SDK/Gradle, por lo que no puedo producir el binario APK aquí.

En un equipo con Android Studio:
1. Descomprime el proyecto.
2. Abre la carpeta como proyecto Gradle.
3. Configura el JDK recomendado por Android Studio.
4. Sincroniza Gradle.
5. Build > Generate App Bundle(s) / APK(s) > Generate APK(s).
6. Para distribución privada, firma el APK con un keystore que controles.
7. Copia el APK al teléfono e instálalo.

No uses el prototipo con datos reales hasta completar el backend, cifrado, autenticación y pruebas de seguridad.
