# RgaPro — compilación desde el móvil

1. Sube TODO el contenido de este proyecto al repositorio privado RgaPro.
2. Comprueba que `gradlew` está en la raíz y que existe `gradle/wrapper/`.
3. En GitHub abre `Actions`.
4. Selecciona `RgaPro Android`.
5. Pulsa `Run workflow`.
6. Espera a que termine el job.
7. Abre el resultado de la ejecución y descarga el artefacto `RgaPro-debug-apk`.
8. Descarga el APK al teléfono e instálalo.

Si GitHub muestra un error de `gradlew` o `gradle/wrapper`, el proyecto necesita regenerar el Gradle Wrapper antes de poder compilar.

IMPORTANTE:
- Mantén el repositorio privado.
- No subas clientes, pólizas, contraseñas, tokens, claves de cifrado ni keystores.
- Este workflow genera un APK de prueba (debug), no una versión firmada para producción/Google Play.
- Para Play Store se debe generar un AAB firmado con un keystore guardado como secreto de GitHub.
