from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

fixed_array = 'String[] lines=(raw==null?"":raw.replace("\\r","\\n")).split("\\n");'
fixed_text = "String text=raw==null?\"\":raw.replace('\\r','\\n');"

for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()

    # Some earlier patch scripts have escaped Java punctuation (for example
    # `\\:` and `\\.`). Match the whole generated statement instead of relying
    # on an exact spelling, then replace it with valid Java source.
    if 'String[] lines=' in line and 'raw' in line and 'replace' in line and '.split' in line:
        lines[i] = indent + fixed_array
        changed = True
    elif re.search(r'String\s+text\s*=\s*raw\s*==\s*null', stripped) and 'raw' in line and 'replace' in line:
        lines[i] = indent + fixed_text
        changed = True

if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
