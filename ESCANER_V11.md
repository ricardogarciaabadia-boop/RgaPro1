# RgaPro v11 — Escáner de documentos

Integrado:
- Google ML Kit Document Scanner.
- Detección automática de documento.
- Recorte/edición del documento.
- Hasta 20 páginas por escaneo.
- Salida PDF + JPEG.
- OCR en el dispositivo con ML Kit Text Recognition.
- Cierre del reconocedor para liberar recursos.

Flujo previsto:
Escanear → OCR → extraer campos → buscar coincidencias → mostrar propuesta →
confirmación explícita → guardar en el cliente correcto.

IMPORTANTE:
Esta versión integra el motor de escaneo y OCR, pero el paso de extracción estructurada
(DNI, póliza, teléfono, dirección, etc.), coincidencia de clientes y guardado sincronizado
con el servidor todavía debe conectarse a la capa de datos antes de producción.
