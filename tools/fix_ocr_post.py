from pathlib import Path

p = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = p.read_text(encoding='utf-8')
start = s.index('    private OcrData parseOcr(String raw){')
end = s.index('    private String clean(String s){', start)
part = s[start:end]
part = part.replace('\\\\\\\\', '\\\\')
s = s[:start] + part + s[end:]
p.write_text(s, encoding='utf-8')
