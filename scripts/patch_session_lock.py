from pathlib import Path

p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

if 'private boolean rgaProUserLeftApp' not in s:
    marker='    private final Executor biometricExecutor=Executors.newSingleThreadExecutor();\n'
    insert='''    private boolean rgaProUserLeftApp=false;\n    private boolean rgaProScreenLocked=false;\n\n    @Override public void onUserLeaveHint(){\n        super.onUserLeaveHint();\n        rgaProUserLeftApp=true;\n    }\n\n    @Override protected void onResume(){\n        super.onResume();\n        rgaProUserLeftApp=false;\n        if(rgaProScreenLocked){\n            rgaProScreenLocked=false;\n            currentUser=null;\n            showLogin();\n        }\n    }\n\n    @Override protected void onStop(){\n        super.onStop();\n        if(rgaProUserLeftApp && currentUser!=null){\n            rgaProScreenLocked=true;\n            currentUser=null;\n        }\n    }\n\n'''
    if marker not in s: raise SystemExit('biometric executor marker not found')
    s=s.replace(marker,marker+insert,1)

# Avoid relaunching login immediately from onStop; show it when the activity returns.
p.write_text(s,encoding='utf-8')
