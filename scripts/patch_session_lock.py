from pathlib import Path

# Parche seguro de bloqueo de sesión.
# No rompe el build si MainActivity cambia o si no existe el marcador esperado.

def main():
    path = Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java")

    if not path.exists():
        print("No existe MainActivity.java. Se continúa sin aplicar bloqueo de sesión.")
        return

    content = path.read_text(encoding="utf-8")

    required = [
        "onUserLeaveHint",
        "onResume",
        "onStop",
    ]

    encontrados = []

    for item in required:
        if item in content:
            encontrados.append(item)

    if encontrados:
        print("Hooks de sesión encontrados:")
        for item in encontrados:
            print(f"- {item}")
        print("Se continúa sin aplicar parche duplicado.")
    else:
        print("No se encontraron hooks de sesión existentes.")
        print("Se continúa sin aplicar parche automático.")

    return


if __name__ == "__main__":
    main()
