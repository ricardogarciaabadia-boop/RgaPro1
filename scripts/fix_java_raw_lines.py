from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

BS = chr(92)
# Keep the generated normalization variable unique. Several generated helpers
# already use `text`; using a stable name avoids a collision when generated
# code is assembled into the same Java scope.
FIXED_ARRAY = 'String[] lines=(raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n")).split("' + BS + 'n");'
FIXED_TEXT = 'String normalizedText=raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n");'


def repair_generated_line(line):
    # Replace only the malformed declaration, not the whole Java source line.
    # Some generated methods are emitted as a single very long line; returning
    # FIXED_TEXT/FIXED_ARRAY here used to discard the rest of parseOcr().
    if re.search(r'String\s*\[\]\s*lines\s*=.*?raw', line):
        line, count = re.subn(
            r'String\s*\[\]\s*lines\s*=\s*.*?\.split\("' + BS + r'\\n"\);',
            FIXED_ARRAY,
            line,
            count=1,
        )
        if count:
            return line

    if re.search(r'String\s+text\s*=.*?raw', line):
        match = re.search(
            r'(String\s+)text(\s*=\s*raw\s*==\s*null\s*\?\s*""\s*:\s*raw\.replace\([^;]*\);)(.*)$',
            line,
        )
        if match:
            # The declaration is renamed, and only the code following that
            # declaration is updated. This avoids changing string literals or
            # unrelated identifiers while keeping same-line generated code valid.
            suffix = re.sub(r'\btext\b', 'normalizedText', match.group(3))
            return line[:match.start()] + FIXED_TEXT + suffix

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
