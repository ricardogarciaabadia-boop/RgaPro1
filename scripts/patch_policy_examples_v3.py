from pathlib import Path

p = Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s = p.read_text(encoding='utf-8')

# The previous policy example patch emitted Python-string escaping into Java
# (e.g. '\\\\r' inside a Java character literal). Normalize those sequences
# before compiling. This does not alter the proven OCR engine.
s = s.replace("raw.replace('\\\\r','\\\\n')", "raw.replace('\\r','\\n')")
s = s.replace(".replace('\\\\r','\\\\n')", ".replace('\\r','\\n')")
s = s.replace("raw.replace('\\\\r','\\\\n')", "raw.replace('\\r','\\n')")

p.write_text(s, encoding='utf-8')
print('Normalized policy parser Java escape sequences')
