from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

BS = chr(92)
FIXED_ARRAY = 'String[] lines=(raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n")).split("' + BS + 'n");'
FIXED_TEXT = 'String text=raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n");'


def repair_generated_line(line):
    indent = line[:len(line) - len(line.lstrip())]
    if re.search(r'String\s*\[\]\s*lines\s*=.*raw', line):
        return indent + FIXED_ARRAY
    if re.search(r'String\s+text\s*=.*raw', line):
        return indent + FIXED_TEXT

    normalized = line
    for punctuation in '.:<>_%':
        normalized = re.sub(r'\\+' + re.escape(punctuation), punctuation, normalized)

    # A generated Runnable can contain nested for/if blocks on one line.
    # `}}` closes those blocks; do not remove a brace. The lambda assignment
    # itself simply needs the terminating semicolon: `}};`.
    if (re.search(r'refresh\s*\[\s*0\s*\]\s*=\s*\(\s*\)\s*->\s*\{', normalized)
            and normalized.rstrip().endswith('}}')):
        normalized = normalized.rstrip() + ';'

    return normalized


out = []
for original in lines:
    repaired = repair_generated_line(original)
    if repaired != original:
        changed = True
    out.append(repaired)

result = '\n'.join(out) + ('\n' if s.endswith('\n') else '')
if result != s:
    JAVA.write_text(result, encoding='utf-8')
    changed = True

remaining = []
for number, line in enumerate(out, 1):
    if re.search(r'\\+[.:<>_%]', line):
        remaining.append(number)
if remaining:
    raise SystemExit(
        'Malformed Java punctuation escapes remain in MainActivity.java at lines: '
        + ', '.join(map(str, remaining))
    )

if changed:
    print('Malformed Java raw-line, escaped-punctuation, and generated Runnable normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
