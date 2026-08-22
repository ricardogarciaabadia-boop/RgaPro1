from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
changed = False


def normalize_java_escapes(line):
    """Remove accidental Markdown escapes while preserving real Java escapes."""
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
            # Only remove Markdown-style escapes before punctuation.
            # Never alter Java escapes such as \r, \n, \", or \'.
            if nxt in punctuation and (nxt != '.' or not in_string):
                out.append(nxt)
                i = j + 1
                continue
            out.append(run)
            i = j
            continue
        out.append(c)
        if c == '"' and not in_char:
            in_string = not in_string
        elif c == "'" and not in_string:
            in_char = not in_char
        i += 1
    return ''.join(out)


lines = s.splitlines()
for i, line in enumerate(lines):
    normalized = normalize_java_escapes(line)
    if normalized != line:
        lines[i] = normalized
        changed = True

    # Rebuild this generated OCR-parser statement from a literal template.
    # This prevents the normalizer from ever producing invalid Java char literals.
    if 'String[] lines=(raw==null' in lines[i]:
        fixed = r'''        String[] lines=(raw==null?"":raw.replace('\r','\n')).split("\\n");'''
        if lines[i] != fixed:
            lines[i] = fixed
            changed = True

    stripped = lines[i].strip()
    if stripped.startswith('refresh[0]=()->{') and stripped.endswith('}}'):
        lines[i] = lines[i] + ';'
        changed = True

s = '\n'.join(lines) + ('\n' if s.endswith('\n') else '')

# Repair the block parser variant as well. Keep Java escapes literal.
s = re.sub(
    r'(?m)^\s*String\[\] lines=.*split\(".*"\);$',
    r'''        String[] lines=(block==null?"":block.replace('\r','\n')).split("\\n");''',
    s,
    count=1,
) if 'private OcrData parseOcr(String raw)' not in s else s

# These generated helpers need a local JSONArray named out.
exact_out_markers = [
    ('if(block.trim().isEmpty())return out;', '        JSONArray out=new JSONArray();'),
    ('for(String line:block.split("\\\\n|;")){String x=line.trim();', '        JSONArray out=new JSONArray();'),
]
for marker, declaration in exact_out_markers:
    if marker in s:
        pos = s.find(marker)
        method_start = s.rfind('    private ', 0, pos)
        method_end = s.find('{', method_start)
        if method_start >= 0 and method_end >= 0:
            body_prefix = s[method_end:pos]
            if 'JSONArray out=new JSONArray();' not in body_prefix:
                s = s[:method_end + 1] + '\n' + declaration + s[method_end + 1:]
                changed = True

s = s.replace('private void appendSearchJson(StringBuilder out,JSONObject o){JSONArray out=new JSONArray();', 'private void appendSearchJson(StringBuilder out,JSONObject o){')
s = s.replace('private void appendSearchArray(StringBuilder out,JSONArray a){JSONArray out=new JSONArray();', 'private void appendSearchArray(StringBuilder out,JSONArray a){')

bad_birth = 'if(dniMode&&birth!=null)birth.setVisibility(View.VISIBLE);else processLastImage(false);'
if bad_birth in s:
    s = s.replace(bad_birth, 'processLastImage(false);')
    changed = True

if 'private OcrData parseOcr(String raw)' not in s:
    marker = '    private void showOcrResult(String raw){'
    if marker in s:
        parser = '''    private static class OcrData{String holder="",surname="",name="",dni="",address="",phone="",email="";int confidence=0;}\n    private OcrData parseOcr(String raw){\n        OcrData d=new OcrData();\n        String text=raw==null?"":raw.replace('\\r','\\n');\n        String[] ls=text.split("\\n");\n        for(int i=0;i<ls.length;i++){\n            String line=ls[i].trim();\n            String u=line.toUpperCase(Locale.ROOT);\n            String v="";\n            if(u.matches(".*\\\\b(?:DNI|NIE)\\\\b.*")){Matcher m=Pattern.compile("\\\\b(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])\\\\b",Pattern.CASE_INSENSITIVE).matcher(line);if(m.find())d.dni=m.group().toUpperCase(Locale.ROOT);}\n            if(u.startsWith("NOMBRE"))v=line.replaceFirst("(?i)^NOMBRE\\\\s*[:.-]?\\\\s*","").trim();else if(u.startsWith("APELLIDOS"))v=line.replaceFirst("(?i)^APELLIDOS\\\\s*[:.-]?\\\\s*","").trim();else if(u.startsWith("DOMICILIO")||u.startsWith("DIRECCION")||u.startsWith("DIRECCIÓN"))v=line.replaceFirst("(?i)^(?:DOMICILIO|DIRECCION|DIRECCIÓN)\\\\s*[:.-]?\\\\s*","").trim();\n            if(v.isEmpty()&&i+1<ls.length&&(u.equals("NOMBRE")||u.equals("APELLIDOS")||u.equals("DOMICILIO")||u.equals("DIRECCION")||u.equals("DIRECCIÓN")))v=ls[++i].trim();\n            if(u.startsWith("NOMBRE"))d.name=v;else if(u.startsWith("APELLIDOS"))d.surname=v;else if(u.startsWith("DOMICILIO")||u.startsWith("DIRECCION")||u.startsWith("DIRECCIÓN"))d.address=v;\n        }\n        Matcher ph=Pattern.compile("(?<!\\\\d)(?:\\\\+34[\\\\s.-]?)?[6789]\\\\d{2}[\\\\s.-]?\\\\d{3}[\\\\s.-]?\\\\d{3}(?!\\\\d)").matcher(text);if(ph.find())d.phone=ph.group().replaceAll("[\\\\s.-]","");\n        Matcher em=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE).matcher(text);if(em.find())d.email=em.group();\n        d.holder=(d.name+" "+d.surname).trim();\n        d.confidence=(d.dni.isEmpty()?0:60)+(d.name.isEmpty()?0:15)+(d.surname.isEmpty()?0:10)+(d.address.isEmpty()?0:5)+(d.phone.isEmpty()?0:5)+(d.email.isEmpty()?0:5);\n        return d;\n    }\n\n'''
        s = s.replace(marker, parser + marker, 1)
        changed = True

if changed:
    JAVA.write_text(s, encoding='utf-8')
    print('Malformed Java raw-line, lambda termination, escaped-punctuation, and generated-scope normalization fixed')
else:
    print('No malformed Java normalization found')
