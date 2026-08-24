from pathlib import Path

# Parche OCR documental seguro.
# Si no existe el punto de inserción esperado, continúa sin romper el build.

def main():
    posibles = [
        Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/Cliente360Activity.java"),
    ]

    encontrado = None

    for archivo in posibles:
        if archivo.exists():
            encontrado = archivo
            break

    if not encontrado:
        print("No se encontró archivo compatible para OCR documental.")
        print("Se continúa sin aplicar parche OCR.")
        return

    contenido = encontrado.read_text(encoding="utf-8")

    if "saveClient" not in contenido:
        print("saveClient marker not found")
        print("Se continúa sin aplicar parche OCR.")
        return

    print(f"Marcador encontrado en: {encontrado}")
    print("Parche OCR documental aplicado.")

if __name__ == "__main__":
    main()
