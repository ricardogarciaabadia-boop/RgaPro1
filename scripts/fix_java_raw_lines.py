from pathlib import Path

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False
fixed_array = '        String[] lines=(raw==null?"":raw.replace("\\r","\\n")).split("\\n");'
fixed_text = "        String text=raw==null?\"\":raw.replace('\\r','\\n');"
for i, line in enumerate(lines):
    stripped = line.strip()
    indent = line[:len(line)-len(line.lstrip())]
    if 'String[] lines=(raw==null?' in line and 'replace' in line and '.split' in line:
        lines[i] = indent + fixed_array.strip()
        changed = True
    elif stripped.startswith('String text=raw==null?') and 'raw.replace' in line:
        lines[i] = indent + fixed_text.strip()
        changed = True
if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
