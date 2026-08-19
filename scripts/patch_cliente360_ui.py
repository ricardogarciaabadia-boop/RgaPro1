from pathlib import Path
import re

p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Replace the existing detail dialog with the full Client 360 activity.
pattern=r'    private void detail\(JSONObject p\)\{.*?\n    private void policies\(\)'
replacement='''    private void detail(JSONObject p){
        Intent i=new Intent(this,Client360Activity.class);
        i.putExtra("client_json",p.toString());
        startActivity(i);
    }
    private void policies()'''
s2,n=re.subn(pattern,replacement,s,flags=re.S)
if n!=1: raise SystemExit(f'detail replacement count={n}')
s=s2

# Make OCR the primary action and turn side sections into expandable menus.
old=r'''        Button undo=sideButton("↩️  DESHACER\\nÚltima acción"); undo.setTextSize(16); undo.setTextColor(NAVY); undo.setOnClickListener(v->undoLastAction()); side.addView(undo,new LinearLayout.LayoutParams(-1,dp(76)));
        Button clients=sideButton("👤  Clientes"); clients.setOnClickListener(v->clients()); side.addView(clients,new LinearLayout.LayoutParams(-1,dp(60)));
        Button policies=sideButton("▣  Pólizas"); policies.setOnClickListener(v->policies()); side.addView(policies,new LinearLayout.LayoutParams(-1,dp(60)));
        Button docs=sideButton("▤  Escanear / OCR"); docs.setOnClickListener(v->scanDocument()); side.addView(docs,new LinearLayout.LayoutParams(-1,dp(60)));
        Button expires=sideButton("🔔  Futuras bajas"); expires.setOnClickListener(v->expiries()); side.addView(expires,new LinearLayout.LayoutParams(-1,dp(60)));
        Button security=sideButton("🔒  Seguridad"); security.setOnClickListener(v->security()); side.addView(security,new LinearLayout.LayoutParams(-1,dp(60)));
        Button logout=sideButton("Salir"); logout.setOnClickListener(v->showLogin()); side.addView(logout,new LinearLayout.LayoutParams(-1,dp(56)));'''
new='''        Button ocrMain=sideButton("📷  OCR PRINCIPAL"); ocrMain.setTextSize(17); ocrMain.setTextColor(Color.WHITE); ocrMain.setBackground(bg(BLUE,18)); ocrMain.setOnClickListener(v->scanDocument()); side.addView(ocrMain,new LinearLayout.LayoutParams(-1,dp(76)));
        Button undo=sideButton("↩️  DESHACER\\nÚltima acción"); undo.setTextSize(16); undo.setTextColor(NAVY); undo.setOnClickListener(v->undoLastAction()); side.addView(undo,new LinearLayout.LayoutParams(-1,dp(70)));
        expandableMenu(side,"👤  CLIENTES",new String[]{"Buscar cliente","Nuevo / ficha"},new View.OnClickListener[]{v->clients(),v->clients()});
        expandableMenu(side,"▣  PÓLIZAS",new String[]{"Todas","Por tipo","Próximas bajas"},new View.OnClickListener[]{v->policies(),v->policies(),v->expiries()});
        expandableMenu(side,"📄  DOCUMENTOS",new String[]{"Escanear OCR","JPEG / fotos","PDF"},new View.OnClickListener[]{v->scanDocument(),v->scanDocument(),v->scanDocument()});
        expandableMenu(side,"🔒  SEGURIDAD",new String[]{"Bloqueo y biometría"},new View.OnClickListener[]{v->security()});
        Button logout=sideButton("Salir / bloquear"); logout.setOnClickListener(v->showLogin()); side.addView(logout,new LinearLayout.LayoutParams(-1,dp(56)));'''
if old not in s: raise SystemExit('home menu block not found')
s=s.replace(old,new,1)

# Add expandable menu helper before page().
marker='    private void page(String title,String sub){'
helper='''    private void expandableMenu(LinearLayout parent,String title,String[] options,View.OnClickListener[] listeners){
        LinearLayout box=col();
        Button head=sideButton(title+"  ▾"); head.setTextSize(14); box.addView(head,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout opts=col(); opts.setVisibility(View.GONE); box.addView(opts,new LinearLayout.LayoutParams(-1,-2));
        head.setOnClickListener(v->{boolean open=opts.getVisibility()==View.VISIBLE;opts.setVisibility(open?View.GONE:View.VISIBLE);head.setText(title+(open?"  ▾":"  ▴"));});
        for(int i=0;i<options.length;i++){Button b=sideButton("   • "+options[i]);b.setTextSize(13);b.setOnClickListener(listeners[i]);opts.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);parent.addView(box,lp);
    }
'''
if marker not in s: raise SystemExit('page marker not found')
s=s.replace(marker,helper+marker,1)

# Make page back navigation a large, explicit button.
oldback='Button back=action("‹",false);back.setTextColor(Color.WHITE);back.setBackgroundColor(Color.TRANSPARENT);back.setOnClickListener(v->home());h.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));'
newback='Button back=action("↩️  VOLVER",true);back.setTextSize(18);back.setOnClickListener(v->home());h.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));'
if oldback not in s: raise SystemExit('back button not found')
s=s.replace(oldback,newback,1)

# Lock when the user backgrounds the app; do not lock while an OCR camera/picker is active.
anchor='    @Override public void onCreate(Bundle b){'
lock='''    private boolean lockOnResume=false;
    @Override protected void onUserLeaveHint(){super.onUserLeaveHint();if(!isFinishing()&&scanFile==null&&!dniMode&&!multiMode){prefs.edit().putBoolean("lock_required",true).apply();}}
    @Override protected void onResume(){super.onResume();if(prefs!=null&&prefs.getBoolean("lock_required",false)&&currentUser!=null){prefs.edit().putBoolean("lock_required",false).apply();showLogin();}}
'''
if anchor not in s: raise SystemExit('onCreate anchor not found')
s=s.replace(anchor,lock+anchor,1)
p.write_text(s,encoding='utf-8')

# Register the new activity in the manifest at build time.
m=Path('app/src/main/AndroidManifest.xml')
ms=m.read_text(encoding='utf-8')
if 'Client360Activity' not in ms:
    ms=ms.replace('</application>','    <activity android:name=".Client360Activity" android:exported="false" />\n    </application>')
    m.write_text(ms,encoding='utf-8')
