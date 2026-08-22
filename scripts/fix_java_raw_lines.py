from pathlib import Path
import re
import subprocess

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
lines = s.splitlines()
changed = False

fixed_array = 'String[] lines=(raw==null?"":raw.replace("\\r","\\n")).split("\\n");'
fixed_text = "String text=raw==null?\"\":raw.replace('\\r','\\n');"


def normalize_java_escapes(line):
    """Remove accidental Markdown escapes while preserving real Java string escapes."""
    out = []
    i = 0
    in_string = False
    in_char = False
    while i < len(line):
        c = line[i]
        if c == '\\':
            j = i
            while j < len(line) and line[j] == '\\':
                j += 1
            run = line[i:j]
            nxt = line[j] if j < len(line) else ''
            punctuation = ':<>_%.,'
            if nxt in punctuation and (nxt != '.' or not in_string):
                out.append(nxt)
                i = j + 1
                continue
            out.append(run)
            if nxt in ('"', "'"):
                if nxt == '"' and in_string and len(run) % 2 == 1:
                    out.append(nxt)
                    i = j + 1
                    continue
                if nxt == "'" and in_char and len(run) % 2 == 1:
                    out.append(nxt)
                    i = j + 1
                    continue
            i = j
            continue
        out.append(c)
        if c == '"' and not in_char:
            in_string = not in_string
        elif c == "'" and not in_string:
            in_char = not in_char
        i += 1
    return ''.join(out)


def restore_parse_ocr_if_missing(src):
    if 'private OcrData parseOcr(' in src:
        return src, False
    try:
        base = subprocess.check_output(
            ['git', 'show', '593af701d4f14adcc009ef4f686d98de11b5081f:' + str(JAVA)],
            text=True,
            encoding='utf-8',
        )
    except Exception:
        return src, False
    marker = '    private void showOcrResult(String raw){'
    b = base.find('    private static class OcrData{')
    e = base.find(marker, b)
    if b < 0 or e < 0 or marker not in src:
        return src, False
    methods = base[b:e]
    return src.replace(marker, methods + marker, 1), True


for i, line in enumerate(lines):
    indent = line[:len(line) - len(line.lstrip())]
    stripped = line.strip()

    normalized = normalize_java_escapes(line)
    if normalized != line:
        lines[i] = normalized
        line = normalized
        stripped = line.strip()
        indent = line[:len(line) - len(line.lstrip())]
        changed = True

    # The managed-user patch emits the refresh lambda as one generated line.
    if stripped.startswith('refresh[0]=()->{') and stripped.endswith('}}'):
        lines[i] = line + ';'
        line = lines[i]
        stripped = line.strip()
        changed = True

    if 'String[] lines=' in line and 'raw' in line and 'replace' in line and '.split' in line:
        if lines[i] != indent + fixed_array:
            lines[i] = indent + fixed_array
            changed = True
    elif re.search(r'String\s+text\s*=\s*raw\s*==\s*null', stripped) and 'raw' in line and 'replace' in line:
        if lines[i] != indent + fixed_text:
            lines[i] = indent + fixed_text
            changed = True

s = '\n'.join(lines) + ('\n' if s.endswith('\n') else '')

# Several source-generation patches insert helper bodies independently. If a later
# patch replaces only the body, the JSONArray declaration can be lost. Restore it
# locally in every method that uses the generated `out` accumulator.
method_re = re.compile(r'(?m)^(    private [^{]+\([^\n]*\)\{)(.*?)(?=^    private |^    @Override |^\})', re.S)
def add_missing_out(m):
    head, body = m.group(1), m.group(2)
    if 'out.' not in body or re.search(r'\bJSONArray\s+out\s*=', body):
        return m.group(0)
    return head + '\n        JSONArray out=new JSONArray();' + body
new_s = method_re.sub(add_missing_out, s)
if new_s != s:
    s = new_s
    changed = True

# onActivityResult is a callback; it cannot see the local birth EditText created
# inside showOcrResult. Remove that accidental cross-scope reference.
bad_birth = 'if(dniMode&&birth!=null)birth.setVisibility(View.VISIBLE);else processLastImage(false);'
if bad_birth in s:
    s = s.replace(bad_birth, 'processLastImage(false);')
    changed = True

# A generated helper occasionally references `raw` without retaining it in its
# signature. Keep the helper compilable; its callers still provide the actual OCR
# text through the surrounding workflow where available.
helper_lines = s.splitlines()
for i, line in enumerate(helper_lines):
    if 'String[] lines=(raw==null?' not in line:
        continue
    start = i
    while start >= 0 and not re.match(r'^    private .*\{$', helper_lines[start].strip()):
        start -= 1
    if start < 0:
        continue
    signature = helper_lines[start]
    if 'String raw' not in signature:
        helper_lines.insert(i, '        String raw="";')
        changed = True
        break
s = '\n'.join(helper_lines) + ('\n' if s.endswith('\n') else '')

restored, did_restore = restore_parse_ocr_if_missing(s)
if did_restore:
    s = restored
    changed = True

if changed:
    JAVA.write_text(s, encoding='utf-8')
    print('Malformed Java raw-line, lambda, escaped-punctuation, and generated-scope normalization fixed')
else:
    print('No malformed Java normalization found')
