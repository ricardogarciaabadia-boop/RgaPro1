from pathlib import Path

p = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = p.read_text(encoding='utf-8')

start = '    private boolean rgaProUserLeftApp=false;'
end = '    private int dp(int n)'
if start in s:
    a = s.index(start)
    b = s.index(end, a)
    s = s[:a] + s[b:]

marker = '    private final Executor biometricExecutor=Executors.newSingleThreadExecutor();\n'
if marker not in s:
    raise SystemExit('biometric executor marker not found')

block = '''    private boolean rgaProUserLeftApp=false;
    private boolean rgaProScreenLocked=false;

    @Override public void onUserLeaveHint(){
        super.onUserLeaveHint();
        rgaProUserLeftApp=true;
    }

    @Override protected void onResume(){
        super.onResume();
        boolean wasLocked=rgaProScreenLocked;
        rgaProUserLeftApp=false;
        if(wasLocked){
            rgaProScreenLocked=false;
            currentUser=null;
            showLogin();
        }
    }

    @Override protected void onStop(){
        super.onStop();
        if(rgaProUserLeftApp && currentUser!=null){
            rgaProScreenLocked=true;
            currentUser=null;
        }
    }

'''
s = s.replace(marker, marker + block, 1)
p.write_text(s, encoding='utf-8')
