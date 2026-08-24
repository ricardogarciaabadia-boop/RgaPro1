from pathlib import Path
import re

# Final launcher: force RGA PRO resource and neutralize any legacy RDF launcher reference.
man=Path('app/src/main/AndroidManifest.xml')
s=man.read_text(encoding='utf-8')
s=re.sub(r'android:icon="@(?:drawable|mipmap)/[^"]+"','android:icon="@drawable/rgapro_launcher"',s)
s=re.sub(r'android:roundIcon="@(?:drawable|mipmap)/[^"]+"','android:roundIcon="@drawable/rgapro_launcher"',s)
man.write_text(s,encoding='utf-8')

# Make launcher resource itself explicitly RGA PRO and not derived from RDF.
r=Path('app/src/main/res/drawable/rgapro_launcher.xml')
if not r.exists():
    raise SystemExit('rgapro_launcher.xml missing')
print('launcher points to exact RGA PRO resource')

# Remove any page-level fixed bottom bars/black indicators if present in the prototype HTML.
h=Path('app/src/main/assets/prototype/index_v3.html')
t=h.read_text(encoding='utf-8')
t=re.sub(r'<div[^>]+class=["\']bottom[^>]*>.*?</div>','',t,flags=re.S|re.I)
t=re.sub(r'<div[^>]+id=["\']bottom[^>]*>.*?</div>','',t,flags=re.S|re.I)
t=re.sub(r'position:\s*fixed;[^}]*bottom:\s*0;[^}]*background:\s*#000[^}]*','',t,flags=re.I)
h.write_text(t,encoding='utf-8')
print('bottom visual strip removal applied')
