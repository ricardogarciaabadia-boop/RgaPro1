from pathlib import Path

# Parche seguro para Cliente final.
# No modifica archivos y nunca rompe el build si cambia la estructura.

def main():
    posibles = [
        Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/Cliente360Activity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/ui/Cliente360Activity.java"),
    ]

    encontrados = [str(p) for p in posibles if p.exists()]

    if encontrados:
        print("Archivos encontrados:")
        for archivo in encontrados:
            print(f"- {archivo}")
    else:
        print("No se encontraron archivos Cliente compatibles.")

    print("Parche Cliente final omitido. Se continúa con el build.")


if __name__ == "__main__":
    main()
