from pathlib import Path
import re

p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

# The base activity may already contain lifecycle callbacks added by an earlier
# patch. Remove only our previously generated callbacks, then add one canonical
# implementation so repeated workflow patches remain idempotent.
for name, pattern in [
    ('onUserLeaveHint', r'\n\s*@Override\s+(?:protected\s+)?void\s+onUserLeaveHint\(\)\s*\{.*?\n\s*\}\s*'),
    ('onResume', r'\n\s*@Override\s+protected\s+void\s+onResume\(\)\s*\{.*?\n\s*\}\s*'),
    ('onStop', r'\n\s*@Override\s+protected\s+void\s+onStop\(\)\s*\{.*?\n\s*\}\s*'),
]:
    matches=list(re.finditer(pattern,s,re.S))
    if len(matches)>1 or (name in ('onUserLeaveHint','onResume','onStop') and matches):
        # Keep no generated copy; a single canonical block is inserted below.
        s=re.sub(pattern,'\n',s,count=0,flags=re.S)

# Remove stale marker fields from prior generated copies.
s=re.sub(r'\n\s*private boolean rgaProUserLeftApp=false;\s*\n\s*private boolean rgaProScreenLocked=false;\s*','\n',s)

marker='    private final Executor biometricExecutor=Executors.newSingleThreadExecutor();\n'
insert='''    private boolean rgaProUserLeftApp=false;\n    private boolean rgaProScreenLocked=false;\n\n    @Override public void onUserLeaveHint(){\n        super.onUserLeaveHint();\n        rgaProUserLeftApp=true;\n    }\n\n    @Override protected void onResume(){\n        super.onResume();\n        rgaProUserLeftApp=false;\n        if(rgaProScreenLocked){\n            rgaProScreenLocked=false;\n            currentUser=null;\n            showLogin();\n        }\n    }\n\n    @Override protected void onStop(){\n        super.onStop();\n        if(rgaProUserLeftApp && currentUser!=null){\n            rgaProScreenLocked=true;\n            currentUser=null;\n        }\n    }\n\n'''
if marker not in s: raise SystemExit('biometric executor marker not found')
s=s.replace(marker,marker+insert,1)
p.write_text(s,encoding='utf-8')
