from pathlib import Path
import re

p = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = p.read_text(encoding='utf-8')

# The secure-user patch used to install its own lifecycle/security hooks and this
# patch then installed a second copy. Normalize MainActivity first so the build
# always contains exactly one implementation of each lifecycle callback.
def remove_java_method(source: str, name: str) -> str:
    pattern = re.compile(r'(?m)^\s*@Override\s+(?:public|protected)\s+void\s+' + re.escape(name) + r'\s*\([^)]*\)\s*\{')
    while True:
        m = pattern.search(source)
        if not m:
            return source
        start, brace = m.start(), m.end() - 1
        depth = 0
        in_string = False
        in_char = False
        escape = False
        line_comment = False
        block_comment = False
        i = brace
        while i < len(source):
            ch = source[i]
            nxt = source[i + 1] if i + 1 < len(source) else ''
            if line_comment:
                if ch == '\n': line_comment = False
            elif block_comment:
                if ch == '*' and nxt == '/': block_comment = False; i += 1
            elif in_string:
                if escape: escape = False
                elif ch == '\\': escape = True
                elif ch == '"': in_string = False
            elif in_char:
                if escape: escape = False
                elif ch == '\\': escape = True
                elif ch == "'": in_char = False
            else:
                if ch == '/' and nxt == '/': line_comment = True; i += 1
                elif ch == '/' and nxt == '*': block_comment = True; i += 1
                elif ch == '"': in_string = True
                elif ch == "'": in_char = True
                elif ch == '{': depth += 1
                elif ch == '}':
                    depth -= 1
                    if depth == 0:
                        end = i + 1
                        while end < len(source) and source[end] in ' \t': end += 1
                        if end < len(source) and source[end] == '\n': end += 1
                        source = source[:start] + source[end:]
                        break
            i += 1
        else:
            raise SystemExit('Could not close lifecycle method: ' + name)

for method in ('onUserLeaveHint', 'onResume', 'onStop', 'onBackPressed', 'onDestroy'):
    s = remove_java_method(s, method)

# Remove the old secure-sharing session receiver/flags. Secure sharing remains in
# patch_secure_users_sharing.py; session locking has one owner here.
s = re.sub(r'^\s*private BroadcastReceiver securityLockReceiver;\s*\n', '', s, flags=re.M)
s = re.sub(r'^\s*private boolean securitySessionActive=false;\s*\n', '', s, flags=re.M)
s = s.replace('import android.content.BroadcastReceiver;\n', '')
s = s.replace('import android.content.Context;\n', '')
s = s.replace('import android.content.IntentFilter;\n', '')
s = s.replace('securityLockReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(Intent.ACTION_SCREEN_OFF.equals(i.getAction()))lockForSecurity();}};registerReceiver(securityLockReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF));', '')
s = s.replace('securitySessionActive=true;', '')
s = s.replace('Button logout=sideButton("Salir"); logout.setOnClickListener(v->lockForSecurity());', 'Button logout=sideButton("Salir"); logout.setOnClickListener(v->lockForSecurity());')

marker = '    private final Executor biometricExecutor=Executors.newSingleThreadExecutor();\n'
if marker not in s:
    raise SystemExit('biometric executor marker not found')

block = '''    private boolean rgaProUserLeftApp=false;\n\n    private void lockForSecurity(){\n        rgaProUserLeftApp=false;\n        currentUser=null;\n        showLogin();\n    }\n\n    @Override public void onUserLeaveHint(){\n        super.onUserLeaveHint();\n        rgaProUserLeftApp=true;\n    }\n\n    @Override protected void onStop(){\n        super.onStop();\n        if(rgaProUserLeftApp && currentUser!=null){\n            lockForSecurity();\n        }\n    }\n\n    @Override protected void onResume(){\n        super.onResume();\n        rgaProUserLeftApp=false;\n    }\n\n    @Override public void onBackPressed(){\n        if(currentUser!=null){\n            lockForSecurity();\n        }else{\n            super.onBackPressed();\n        }\n    }\n\n'''
s = s.replace(marker, marker + block, 1)
p.write_text(s, encoding='utf-8')
