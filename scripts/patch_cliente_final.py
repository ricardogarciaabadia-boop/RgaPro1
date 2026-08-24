from pathlib import Path

# Parche seguro:
# Si no existe Cliente360Activity.java, el build continúa sin fallar.

def main():
    paths = [
        Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java"),
        Path("app/src/main/java/com/rgapro1/ocaso/Cliente360Activity.java"),
    ]

    found = False

    for p in paths:
        if p.exists():
            found = True
            break

    if found:
        print("Archivo cliente encontrado. Se continúa sin aplicar parche.")
    else:
        print("No se encontró Cliente360Activity.java. Se continúa sin aplicar parche.")

    return


if __name__ == "__main__":
    main()



