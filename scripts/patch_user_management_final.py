from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')

# Fix the malformed refresh lambda injected by earlier revisions.
s = re.sub(r'list\.addView\(row,new LinearLayout\.LayoutParams\(-1,dp\(120\)\)\);\}\}\}?;?', 'list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));}};', s)

# Normalize the exact malformed form seen in previous builds.
s = s.replace('list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));}}', 'list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));};')

JAVA.write_text(s, encoding='utf-8')
print('User-management refresh lambda normalized')
