from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

BS = chr(92)
FIXED_ARRAY = 'String[] lines=(raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n")).split("' + BS + 'n");'
# Keep the generated variable name `text`: later OCR helpers consume it.
FIXED_TEXT = 'String text=raw==null?"":raw.replace("' + BS + 'r","' + BS + 'n");'


def repair_generated_line(line):
    # Remove Markdown-style punctuation escaping first. These escapes are never
    # valid Java punctuation escapes and can prevent the canonical declaration
    # checks below from matching.
    normalized = line
    for punctuation in '.:<>_%':
        normalized = re.sub(r'\\+' + re.escape(punctuation), punctuation, normalized)

    # Replace generated declarations wholesale so their Java escapes remain
    # canonical while preserving code emitted before/after the declaration.
    if 'String[] lines=' in normalized and 'raw' in normalized:
        prefix = normalized[:normalized.index('String[] lines=')]
        suffix = ''
        marker = normalized.find(';', normalized.index('String[] lines='))
        if marker >= 0:
            suffix = normalized[marker + 1:]
        normalized = prefix + FIXED_ARRAY + suffix

    # Do not rename this variable: generated OCR code references `text`.
    if 'String text=' in normalized and 'raw' in normalized and 'replace' in normalized:
        prefix = normalized[:normalized.index('String text=')]
        suffix = ''
        marker = normalized.find(';', normalized.index('String text='))
        if marker >= 0:
            suffix = normalized[marker + 1:]
        normalized = prefix + FIXED_TEXT + suffix

    # A generated Runnable can contain nested for/if blocks on one line. The
    # lambda assignment itself needs the terminating semicolon.
    if (re.search(r'refresh\s*\[\s*0\s*\]\s*=\s*\(\s*\)\s*->\s*\{', normalized)
            and normalized.rstrip().endswith('}}')):
        normalized = normalized.rstrip() + ';'

    # The final pipeline can leave the birth-date view reference in a callback
    # where that local variable is out of scope. Keep the processing behavior
    # without referencing the unavailable local view.
    normalized = normalized.replace(
        'if(dniMode&&birth!=null)birth.setVisibility(View.VISIBLE);else processLastImage(false);',
        'processLastImage(false);'
    )

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
    print('Malformed Java raw-line, escaped-punctuation, generated Runnable, and callback normalization fixed')
else:
    print('No malformed Java raw-line normalization found')
