from pathlib import Path

MAIN=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=MAIN.read_text(encoding='utf-8')

# The previous generated Java used doubled backslashes inside char literals.
# Normalize them to valid Java character literals before compilation.
s=s.replace("raw.replace('\\\\r','\\\\n')", "raw.replace('\\r','\\n')")

MAIN.write_text(s,encoding='utf-8')
print('Fixed Java newline char literals')
