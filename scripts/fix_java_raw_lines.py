from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

# Canonical Java source forms. These strings are deliberately built with
# normal Python quoting so the generated .java source contains Java escapes,
# not Markdown-style or Python-escaped punctuation.
FIXED_ARRAY = 'String[] lines=(raw==null?"":raw.replace("\\r","\\n")).split("\\n");'
FIXED_TEXT = 'String text=raw==null?"":raw.replace("\\r","\\n");'

for i, original in enumerate(lines):
    line = original
    indent = line[:len(line) - len(line.lstrip())]

    # Remove accidental Markdown punctuation escaping, but only for
    # punctuation that is never an escape in Java source.
    normalized = (line.replace('\\:', ':')
                       .replace('\\<', '<')
                       .replace('\\>', '>')
                       .replace('\\_', '_')
                       .replace('\\%', '%'))
    normalized = re.sub(r'(?<!\\)\\\.', '.', normalized)
    line = normalized

    # Any generated declaration containing these markers is replaced
    # wholesale. This also repairs repeated patch runs that have introduced
    # extra backslashes around quotes or punctuation.
    if 'String[] lines=' in line and 'raw' in line:
        prefix = line[:line.index('String[] lines=')]
        line = prefix + FIXED_ARRAY
    elif 'String text=' in line and 'raw==null' in line and 'replace' in line:
        prefix = line[:line.index('String text=')]
        line = prefix + FIXED_TEXT

    if line != original:
        lines[i] = line
        changed = True

if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line and escaped-punctuation normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
