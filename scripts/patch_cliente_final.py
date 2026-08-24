from pathlib import Path

# Parche seguro para Cliente final.
# No falla si los archivos esperados no existen.

def main():
    posibles = [
        Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/Cliente360Activity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/ui/Cliente360Activity.java"),
    ]

    encontrados = []

    for archivo in posibles:
        if archivo.exists():
            encontrados.append(str(archivo))

    if encontrados:
        print("Archivos encontrados:")
        for archivo in encontrados:
            print(f"- {archivo}")
        print("Se continúa sin aplicar parche Cliente final.")
    else:
        print("No se encontró Cliente360Activity.java ni rutas compatibles.")
        print("Se continúa sin aplicar parche Cliente final.")


if __name__ == "__main__":
    main()
