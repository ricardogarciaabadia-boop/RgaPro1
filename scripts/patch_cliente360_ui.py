#!/usr/bin/env python3

from pathlib import Path
import re
import sys

TARGETS = [
    Path("app/src/main/java"),
]

def find_cliente360():
    for root in TARGETS:
        if root.exists():
            for p in root.rglob("*Cliente360*.java"):
                return p
    return None


def patch_file(path):
    s = path.read_text(encoding="utf-8")

    old = s

    # Evita fallar si ya está aplicado
    if "RgaPro_PATCHED" in s:
        print("Cliente360 ya estaba parcheado")
        return

    marker = "RgaPro_PATCHED"

    replacement = f"""
    // {marker}
    """

    # Buscar método detail de forma flexible
    pattern = r"(private\\s+void\\s+detail\\s*\\([^)]*\\)\\s*\\{{)"

    s2, n = re.subn(
        pattern,
        r"\1" + replacement,
        s,
        count=1,
        flags=re.MULTILINE
    )

    if n == 0:
        raise SystemExit(
            "No se encontró el método detail() para parchear"
        )

    path.write_text(s2, encoding="utf-8")
    print(f"Patched {path}")


def main():
    f = find_cliente360()

    if not f:
        raise SystemExit(
            "No se encontró Cliente360Activity.java"
        )

    patch_file(f)


if __name__ == "__main__":
    main()
