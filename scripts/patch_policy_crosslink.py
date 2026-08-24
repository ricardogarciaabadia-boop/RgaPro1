from pathlib import Path

MAIN = Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java")


def main():
    if not MAIN.exists():
        print("No existe MainActivity.java. Se continúa sin aplicar crosslink.")
        return

    s = MAIN.read_text(encoding="utf-8")

    # Parche seguro:
    # Este script ya no falla si las estructuras antiguas no existen.
    # Las funciones nuevas se aplican mediante los parches específicos actuales.

    if "policyIds" in s or "insureds" in s:
        print("Crosslink de pólizas ya presente. Se continúa.")
        return

    print("No se encontró estructura compatible para crosslink.")
    print("Se continúa sin aplicar parche automático.")


if __name__ == "__main__":
    main()
