from pathlib import Path

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')
old = 'list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));}};'
new = 'list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));};'
if old in s:
    s = s.replace(old, new, 1)
    JAVA.write_text(s, encoding='utf-8')
    print('Fixed malformed user-management refresh closure')
else:
    print('Refresh closure already fixed or not present')
