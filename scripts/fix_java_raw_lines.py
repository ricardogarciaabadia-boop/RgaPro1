from pathlib import Path

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False
fixed = '        String[] lines=(raw==null?"":raw.replace("\\r","\\n")).split("\\n");'
for i, line in enumerate(lines):
    if 'String[] lines=(raw==null?' in line and 'replace' in line and '.split' in line:
        lines[i] = fixed
        changed = True
if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
