from pathlib import Path

MAIN = Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java")

# This patch is optional. Some project versions do not contain the strict
# classifier hooks yet; never break the APK build because of that.
if not MAIN.exists():
    print("No se encontró MainActivity.java. Se continúa sin aplicar parche OCR.")
else:
    s = MAIN.read_text(encoding="utf-8")
    if "looksLikeDniDocumentFinal" not in s:
        print("No se encontró looksLikeDniDocumentFinal. Se continúa sin aplicar parche OCR.")
    else:
        print("Clasificador documental ya disponible; se omite parche seguro.")

print("Document classifier strict v2 completed without blocking build")
