from pathlib import Path

p = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = p.read_text(encoding='utf-8')
old = 'LinearLayout root=col(); root.setBackgroundColor(BG);'
new = 'LinearLayout root=col(); root.setBackgroundResource(R.drawable.bg_rgapro_watermark);'
if old in s:
    s = s.replace(old, new, 1)
    p.write_text(s, encoding='utf-8')
    print('RgaPro watermark background applied')
elif new in s:
    print('RgaPro watermark background already applied')
else:
    raise SystemExit('MainActivity home root line not found')
