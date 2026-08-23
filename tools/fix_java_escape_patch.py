from pathlib import Path
p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')
s=s.replace("raw.replace('\\\\r','\\\\n')", "raw.replace('\\r','\\n')")
p.write_text(s,encoding='utf-8')
print('Java escape patch applied')
