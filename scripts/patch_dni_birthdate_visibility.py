from pathlib import Path

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')

# The DNI form must show Fecha de nacimiento. Earlier classifiers may have hidden it.
s = s.replace('birth.setVisibility(View.GONE);', 'birth.setVisibility(View.VISIBLE);')
s = s.replace('birth.setVisibility(GONE);', 'birth.setVisibility(VISIBLE);')

# Keep birthDate in the persisted DNI record even if an older patch left it in a removal list.
s = s.replace(
    'String[] remove={"birthDate","nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};',
    'String[] remove={"nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};'
)

# If the OCR sanitizer exists, guarantee that a birth date is populated from the labelled DNI field.
needle = 'p.put("documentKind","DNI");'
if needle in s and 'p.put("birthDate",bd);' not in s:
    repl = '''String bd=p.optString("birthDate","").trim();
            if(bd.isEmpty()){
                java.util.regex.Matcher bm=Pattern.compile("(?i)(?:FECHA\\\\s+DE\\\\s+NACIMIENTO|F[.]?\\\\s*NACIMIENTO|NACIMIENTO)\\\\s*[:.-]?\\\\s*(\\\\d{1,2}[ /.-]\\\\d{1,2}[ /.-]\\\\d{4})").matcher(raw==null?"":raw);
                if(bm.find())bd=bm.group(1);
            }
            p.put("birthDate",bd);
            p.put("documentKind","DNI");'''
    s = s.replace(needle, repl, 1)

JAVA.write_text(s, encoding='utf-8')
print('DNI birth date visibility and persistence enforced')
