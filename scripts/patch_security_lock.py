from pathlib import Path
p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Register a screen-lock receiver and lock the session when Android is locked.
if 'private BroadcastReceiver securityLockReceiver;' not in s:
    s=s.replace('import android.content.Intent;','import android.content.Intent;\nimport android.content.BroadcastReceiver;\nimport android.content.Context;\nimport android.content.IntentFilter;',1)
    s=s.replace('private final Executor biometricExecutor=Executors.newSingleThreadExecutor();','private final Executor biometricExecutor=Executors.newSingleThreadExecutor();\n    private BroadcastReceiver securityLockReceiver;\n    private boolean securitySessionActive=false;',1)
    old='@Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);prefs=getSharedPreferences("rgapro_local",MODE_PRIVATE);'
    new='''@Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);prefs=getSharedPreferences("rgapro_local",MODE_PRIVATE);securityLockReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(Intent.ACTION_SCREEN_OFF.equals(i.getAction()))lockForSecurity();}};registerReceiver(securityLockReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF));'''
    if old not in s: raise SystemExit('onCreate marker not found')
    s=s.replace(old,new,1)
    # Mark session active only after successful login/create.
    s=s.replace('currentUser=u.getText().toString().trim();home();','currentUser=u.getText().toString().trim();securitySessionActive=true;home();',1)
    s=s.replace('currentUser=prefs.getString("user","");home();','currentUser=prefs.getString("user","");securitySessionActive=true;home();',1)
    # This replacement catches both password and biometric successful paths if present.
    # Add lifecycle security methods before class closing brace.
    methods=r'''
    private void lockForSecurity(){
        securitySessionActive=false;
        currentUser=null;
        try{if(content!=null){content.removeAllViews();content=null;}}catch(Exception ignored){}
        showLogin();
    }
    @Override public void onUserLeaveHint(){super.onUserLeaveHint();if(securitySessionActive)lockForSecurity();}
    @Override public void onBackPressed(){if(securitySessionActive){lockForSecurity();}else super.onBackPressed();}
    @Override protected void onDestroy(){try{if(securityLockReceiver!=null)unregisterReceiver(securityLockReceiver);}catch(Exception ignored){}super.onDestroy();}
'''
    pos=s.rfind('\n}')
    s=s[:pos]+methods+s[pos:]

# Explicit logout must also clear the active session state.
s=s.replace('Button logout=sideButton("Salir"); logout.setOnClickListener(v->showLogin());','Button logout=sideButton("Salir"); logout.setOnClickListener(v->lockForSecurity());',1)
p.write_text(s,encoding='utf-8')
