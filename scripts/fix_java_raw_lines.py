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

    # Normalize Markdown-style punctuation escapes accidentally emitted into Java.
    normalized = (line.replace('\\:', ':')
                       .replace('\\<', '<')
                       .replace('\\>', '>')
                       .replace('\\_', '_')
                       .replace('\\%', '%'))
    # Remove only a single backslash before a dot; preserve legitimate \\. regex escapes.
    normalized = re.sub(r'(?<!\\)\\\.', '.', normalized)
    if normalized != line:
        lines[i] = normalized
        line = normalized
        changed = True

    # Replace the complete generated line when it contains the known newline split form.
    if 'String[] lines=' in line and 'raw' in line and 'replace' in line and '.split' in line:
        prefix = line[:line.index('String[] lines=')]
        replacement = prefix + fixed_array
        if line != replacement:
            lines[i] = replacement
            changed = True
        continue

    # This declaration is sometimes appended after another statement on the same line
    # (for example: "JSONArray out=new JSONArray();String text=..."). Replace only
    # the malformed String-text declaration and preserve the preceding statements.
    marker = 'String text='
    if marker in line and 'raw==null' in line and 'replace' in line:
        prefix = line[:line.index(marker)]
        replacement = prefix + fixed_text
        if line != replacement:
            lines[i] = replacement
            changed = True

if changed:
    JAVA.write_text('\n'.join(lines) + ('\n' if s.endswith('\n') else ''), encoding='utf-8')
    print('Malformed Java raw-line and escaped-punctuation normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
