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
    # `\\:`, `\\.` and `\\<`). Those backslashes are not valid Java syntax.
    # The application-user refresh lambda is generated as one long line, so
    # normalize escaped punctuation there before javac sees it.
    if 'refresh[0]=()->' in line:
        normalized = re.sub(r'\\([:<>.])', r'\1', line)
        if normalized != line:
            lines[i] = normalized
            line = normalized
            stripped = line.strip()
            indent = line[:len(line) - len(line.lstrip())]
            changed = True

    # Match the whole generated raw-array statement instead of relying on an
    # exact spelling from an earlier patch.
    if 'String[] lines=' in line and 'raw' in line and 'replace' in line and '.split' in line:
        if lines[i] != indent + fixed_array:
            lines[i] = indent + fixed_array
            changed = True
    elif re.search(r'String\s+text\s*=\s*raw\s*==\s*null', stripped) and 'raw' in line and 'replace' in line:
        if lines[i] != indent + fixed_text:
            lines[i] = indent + fixed_text
            changed = True

if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line and escaped-punctuation normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
