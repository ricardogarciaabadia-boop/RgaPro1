from pathlib import Path
import re

# Base-version cleanup: this step is intentionally limited to the DNI flow.
# The later client/product UI patches are not part of this step because they can
# block the build before compilation and are unrelated to the requested DNI change.
p=Path('app/src/main/java/com/rgapro1/ocaso/DniOcrParser.java')
s=p.read_text(encoding='utf-8')

# The DNI step is already implemented in DniOcrParser.java on this branch.
# This script is deliberately idempotent so the workflow can invoke it safely.
required=[
    'public String holder="", surname="", name="", dni="", birthDate="", address="", phone="", email="";',
    'private static String findDni(String text)',
    'private static String findBirthDate(String text)'
]
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('DNI parser expected markers missing; aborting rather than changing unrelated code')
print('DNI step verified; no unrelated client patches applied')
