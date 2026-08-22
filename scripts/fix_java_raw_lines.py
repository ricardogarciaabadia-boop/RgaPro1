from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

BS = chr(92)
FIXED_ARRAY = 'String[] lines=(raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n")).split("' + BS + 'n");'
FIXED_TEXT = 'String normalizedText=raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n");'


def repair_generated_line(line):
    # First remove Markdown-style punctuation escaping. Generated source can
    # contain things such as \\. and \\: inside the normalization declaration;
    # those must be repaired before the declaration regex is applied.
    normalized = line
    for punctuation in '.:<>_%':
        normalized = re.sub(r'\\+' + re.escape(punctuation), punctuation, normalized)

    # Replace only the malformed declaration, preserving any code emitted
    # after it on the same physical Java line.
    if re.search(r'String\s*\[\]\s*lines\s*=.*?raw', normalized):
        repaired, count = re.subn(
            r'String\s*\[\]\s*lines\s*=.*?\.split\s*\(.*?\)\s*;',
            FIXED_ARRAY,
            normalized,
            count=1,
        )
        if count:
            normalized = repaired

    if re.search(r'String\s+text\s*=.*?raw', normalized):
        match = re.search(
            r'(String\s+)text(\s*=\s*raw\s*==\s*null\s*\?\s*""\s*:\s*raw\.replace\([^;]*\);)(.*)$',
            normalized,
        )
        if match:
            suffix = re.sub(r'\btext\b', 'normalizedText', match.group(3))
            normalized = normalized[:match.start()] + FIXED_TEXT + suffix

    # A generated Runnable can contain nested for/if blocks on one line.
    # The lambda assignment itself needs the terminating semicolon.
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
