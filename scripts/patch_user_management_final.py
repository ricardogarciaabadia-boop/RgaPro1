from pathlib import Path
import re

JAVA = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = JAVA.read_text(encoding='utf-8')

# DNI keeps birth date; remove only fields that do not belong to the requested DNI record.
s = s.replace(
    'String[] remove={"birthDate","nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};',
    'String[] remove={"nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};'
)

# Any earlier classifier may have hidden the birth-date field for DNI. Remove those blocks and
# explicitly restore the field afterwards.
s = re.sub(r'if\(dniMode\)\{[^{}\n]*setVisibility\(View\.GONE\);[^{}\n]*\}', '', s)

# Ensure the DNI birth date is actually extracted when no previous OCR pass supplied it.
needle = 'p.put("identityNumber",dni);p.put("holderDni",dni);p.put("address",address);p.put("phone",phone);p.put("email",email);'
if needle in s and 'String bd=p.optString("birthDate","").trim();' not in s:
    repl = '''String bd=p.optString("birthDate","").trim();
            if(bd.isEmpty()){
                Matcher bm=Pattern.compile("(?i)(?:FECHA\\\\s+DE\\\\s+NACIMIENTO|F[.]?\\\\s*NACIMIENTO|NACIMIENTO)\\\\s*[:.-]?\\\\s*(\\\\d{1,2}[ /.-]\\\\d{1,2}[ /.-]\\\\d{4})").matcher(raw==null?"":raw);
                if(bm.find())bd=bm.group(1);
            }
            if(bd.isEmpty()){
                Matcher bm=Pattern.compile("\\\\b\\\\d{1,2}[ /.-]\\\\d{1,2}[ /.-]\\\\d{4}\\\\b").matcher(raw==null?"":raw);
                if(bm.find())bd=bm.group();
            }
            p.put("birthDate",bd);
            p.put("identityNumber",dni);p.put("holderDni",dni);p.put("address",address);p.put("phone",phone);p.put("email",email);'''
    s = s.replace(needle, repl, 1)

# Make the application-user menu resilient to earlier UI changes. Insert after Security,
# otherwise after Documents/Clients, and never fail merely because an anchor moved.
if 'sideButton("👥  Usuarios de la aplicación")' not in s:
    line = '        Button users=sideButton("👥  Usuarios de la aplicación"); users.setOnClickListener(v->users()); side.addView(users,new LinearLayout.LayoutParams(-1,dp(60)));'
    anchors = [
        '        Button security=sideButton("🔒  Seguridad"); security.setOnClickListener(v->security()); side.addView(security,new LinearLayout.LayoutParams(-1,dp(60)));',
        '        Button docs=sideButton("▤  Escanear / OCR"); docs.setOnClickListener(v->scanDocument()); side.addView(docs,new LinearLayout.LayoutParams(-1,dp(60)));',
        '        Button clients=sideButton("👤  Clientes"); clients.setOnClickListener(v->clients()); side.addView(clients,new LinearLayout.LayoutParams(-1,dp(60)));'
    ]
    for anchor in anchors:
        if anchor in s:
            s = s.replace(anchor, anchor+'\n'+line, 1)
            break

# Local application-user store and management screen.
if 'private JSONArray appUsers()' not in s:
    helper='''    private JSONArray appUsers(){
        try{
            String raw=prefs.getString("appUsers","");
            if(!raw.isEmpty())return new JSONArray(raw);
            JSONArray a=new JSONArray();
            String u=prefs.getString("user",""); String p=prefs.getString("pin","");
            if(!u.isEmpty())a.put(new JSONObject().put("name",u).put("pin",p).put("active",true).put("role","ADMIN"));
            prefs.edit().putString("appUsers",a.toString()).apply();
            return a;
        }catch(Exception e){return new JSONArray();}
    }
    private void saveAppUsers(JSONArray a){prefs.edit().putString("appUsers",a.toString()).apply();}
'''
    marker='    private void security(){'
    if marker not in s: marker='    private void home(){'
    s=s.replace(marker,helper+'\n'+marker,1)

if 'private void users(){' not in s:
    methods='''    private void users(){
        page("Usuarios de la aplicación","Gestiona el acceso a RgaPro");
        content.addView(tv("Usuarios con acceso a esta aplicación.",14,MUTED,false));
        Button add=action("＋ Añadir usuario",true);content.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout list=col();content.addView(list);final Runnable[] refresh=new Runnable[1];
        refresh[0]=()->{list.removeAllViews();JSONArray a=appUsers();for(int i=0;i<a.length();i++){final int idx=i;JSONObject q=a.optJSONObject(i);if(q==null)continue;String n=q.optString("name","Sin nombre");boolean active=q.optBoolean("active",true);LinearLayout row=col();row.setPadding(dp(10),dp(6),dp(10),dp(6));row.setBackground(bg(Color.WHITE,14));row.addView(tv((active?"🟢 ":"⚪ ")+n+" · "+q.optString("role","USUARIO"),16,TEXT,true));LinearLayout ac=new LinearLayout(this);Button ed=action("Editar",false),tg=action(active?"Desactivar":"Activar",false),de=action("Eliminar",false);ac.addView(ed,new LinearLayout.LayoutParams(0,dp(46),1));ac.addView(tg,new LinearLayout.LayoutParams(0,dp(46),1));ac.addView(de,new LinearLayout.LayoutParams(0,dp(46),1));row.addView(ac);ed.setOnClickListener(v->editAppUser(idx,refresh[0]));tg.setOnClickListener(v->{try{q.put("active",!active);a.put(idx,q);saveAppUsers(a);refresh[0].run();}catch(Exception e){}});de.setEnabled(!n.equalsIgnoreCase(currentUser));de.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar usuario").setMessage("¿Eliminar a "+n+"?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{JSONArray z=appUsers();if(idx<z.length()){z.remove(idx);saveAppUsers(z);refresh[0].run();}}).show());list.addView(row,new LinearLayout.LayoutParams(-1,dp(120)));}};
        add.setOnClickListener(v->addAppUser(refresh[0]));refresh[0].run();
    }
    private void addAppUser(Runnable refresh){LinearLayout l=col();EditText n=edit("Nombre de usuario"),p=edit("Clave de 6 dígitos"),p2=edit("Repite la clave");p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);p2.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);l.addView(n);l.addView(p);l.addView(p2);AlertDialog d=new AlertDialog.Builder(this).setTitle("Añadir usuario").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String name=n.getText().toString().trim(),pin=p.getText().toString();if(name.isEmpty()||!pin.matches("\\\\d{6}")||!pin.equals(p2.getText().toString())){p2.setError("Nombre y clave de 6 dígitos obligatorios");return;}try{JSONArray a=appUsers();for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q!=null&&name.equalsIgnoreCase(q.optString("name",""))){n.setError("Ese usuario ya existe");return;}}a.put(new JSONObject().put("name",name).put("pin",pin).put("active",true).put("role","USUARIO"));saveAppUsers(a);d.dismiss();refresh.run();}catch(Exception e){p2.setError("No se pudo guardar");}}));d.show();}
    private void editAppUser(int idx,Runnable refresh){JSONArray a=appUsers();JSONObject q=a.optJSONObject(idx);if(q==null)return;String old=q.optString("name","");LinearLayout l=col();EditText n=edit("Nombre de usuario"),p=edit("Nueva clave (opcional)");n.setText(old);p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);l.addView(n);l.addView(p);AlertDialog d=new AlertDialog.Builder(this).setTitle("Editar usuario").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String name=n.getText().toString().trim(),pin=p.getText().toString();if(name.isEmpty()){n.setError("Nombre obligatorio");return;}if(!pin.isEmpty()&&!pin.matches("\\\\d{6}")){p.setError("La clave debe tener 6 dígitos");return;}try{q.put("name",name);if(!pin.isEmpty())q.put("pin",pin);a.put(idx,q);saveAppUsers(a);if(old.equalsIgnoreCase(currentUser)){currentUser=name;prefs.edit().putString("user",name).putString("pin",q.optString("pin",prefs.getString("pin",""))).apply();}d.dismiss();refresh.run();}catch(Exception e){n.setError("No se pudo guardar");}}));d.show();}

'''
    s=s.replace('    private void security(){',methods+'    private void security(){',1) if '    private void security(){' in s else s.replace('    private void home(){',methods+'    private void home(){',1)

# First account is also the administrator and is persisted in the managed-user list.
old='prefs.edit().putString("user",u.getText().toString().trim()).putString("pin",p.getText().toString()).putBoolean("biometric",true).apply();currentUser=u.getText().toString().trim();home();'
new='String nu=u.getText().toString().trim();String np=p.getText().toString();prefs.edit().putString("user",nu).putString("pin",np).putBoolean("biometric",true).putString("appUsers",new JSONArray().put(new JSONObject().put("name",nu).put("pin",np).put("active",true).put("role","ADMIN")).toString()).apply();currentUser=nu;home();'
if old in s:s=s.replace(old,new,1)

# Explicitly keep the birth-date field visible in DNI mode.
if 'if(dniMode&&birth!=null)birth.setVisibility(View.VISIBLE);' not in s:
    marker='if(dniMode){'
    pos=s.find(marker)
    if pos>=0:
        close=s.find('}',pos)
        if close>=0:s=s[:close+1]+' if(dniMode&&birth!=null)birth.setVisibility(View.VISIBLE);'+s[close+1:]

JAVA.write_text(s,encoding='utf-8')
print('Robust DNI birth date and application user management applied')
