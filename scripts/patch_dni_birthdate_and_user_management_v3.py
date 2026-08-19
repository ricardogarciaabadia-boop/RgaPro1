from pathlib import Path
import re

MAIN = Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java")
s = MAIN.read_text(encoding="utf-8")

# DNI: conservar siempre la fecha de nacimiento.
s = s.replace(
    'if(dniMode){cif.setVisibility(View.GONE);birth.setVisibility(View.GONE);nationality.setVisibility(View.GONE);sex.setVisibility(View.GONE);birthPlace.setVisibility(View.GONE);parents.setVisibility(View.GONE);support.setVisibility(View.GONE);issue.setVisibility(View.GONE);validity.setVisibility(View.GONE);}',
    'if(dniMode){cif.setVisibility(View.GONE);nationality.setVisibility(View.GONE);sex.setVisibility(View.GONE);birthPlace.setVisibility(View.GONE);parents.setVisibility(View.GONE);support.setVisibility(View.GONE);issue.setVisibility(View.GONE);validity.setVisibility(View.GONE);birth.setVisibility(View.VISIBLE);birth.setHint("Fecha de nacimiento (IMPORTANTE)");}'
)
s = s.replace(
    'String[] remove={"birthDate","nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};',
    'String[] remove={"nationality","sex","birthPlace","parents","supportNumber","issueDate","validityDate","expiry","cif","products","insureds","product","ahorroModalidad"};'
)

# Menú de usuarios: primero intenta anclas conocidas; si no existen, usa home().
if 'Button users=sideButton("👥  Usuarios de la aplicación")' not in s:
    line='Button users=sideButton("👥  Usuarios de la aplicación"); users.setOnClickListener(v->users()); side.addView(users,new LinearLayout.LayoutParams(-1,dp(68)));'
    patterns = [
        r'(?m)^(\s*Button\s+logout\s*=\s*sideButton\("Salir"\);.*?side\.addView\(logout,\s*new\s+LinearLayout\.LayoutParams\(-1,\s*dp\(\s*56\s*\)\)\s*;)',
        r'(?m)^(\s*Button\s+security\s*=\s*sideButton\([^;]+\);.*?side\.addView\(security,\s*new\s+LinearLayout\.LayoutParams\(-1,\s*dp\(\s*\d+\s*\)\)\s*;)',
    ]
    m = None
    for pattern in patterns:
        m = re.search(pattern, s, re.S)
        if m:
            indent = re.match(r'^\s*', m.group(1)).group(0)
            s = s[:m.start(1)] + indent + line + '\n' + m.group(1) + s[m.end(1):]
            break
    if not m:
        # Último recurso: localizar home() por balanceo de llaves.
        home_match = re.search(r'\bvoid\s+home\s*\(\s*\)\s*\{', s)
        if not home_match:
            raise SystemExit('home method not found')
        brace = home_match.end() - 1
        depth = 0
        close = -1
        for pos in range(brace, len(s)):
            if s[pos] == '{':
                depth += 1
            elif s[pos] == '}':
                depth -= 1
                if depth == 0:
                    close = pos
                    break
        if close < 0:
            raise SystemExit('home method closing brace not found')
        s = s[:close] + '\n' + line + '\n' + s[close:]

# Crear/normalizar almacén de usuarios.
if 'private JSONArray appUsers()' not in s:
    helper = '''    private JSONArray appUsers(){
        try{String raw=prefs.getString("appUsers","");if(!raw.isEmpty())return new JSONArray(raw);JSONArray a=new JSONArray();String u=prefs.getString("user","");String p=prefs.getString("pin","");if(!u.isEmpty())a.put(new JSONObject().put("name",u).put("pin",p).put("active",true).put("role","ADMIN"));prefs.edit().putString("appUsers",a.toString()).apply();return a;}catch(Exception e){return new JSONArray();}
    }
    private void saveAppUsers(JSONArray a){prefs.edit().putString("appUsers",a.toString()).apply();}
'''
    marker='    private void security(){'
    if marker in s:
        s=s.replace(marker,helper+marker,1)

# Sustituir login sin depender de formato/indentación.
start=s.find('private void login(){')
if start>=0:
    next_methods=['private void biometricLogin(){','private void home(){','void biometricLogin(){','void home(){']
    end=-1
    for marker in next_methods:
        p=s.find(marker,start+1)
        if p>=0 and (end<0 or p<end): end=p
    if end>=0:
        login='''private void login(){LinearLayout l=col();EditText u=edit("Usuario"),p=edit("Clave de 6 dígitos");p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);l.addView(u,new LinearLayout.LayoutParams(-1,dp(52)));l.addView(p,new LinearLayout.LayoutParams(-1,dp(52)));AlertDialog d=new AlertDialog.Builder(this).setTitle("Entrar en RgaPro").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String un=u.getText().toString().trim(),pw=p.getText().toString();JSONArray a=appUsers();for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q!=null&&q.optBoolean("active",true)&&un.equalsIgnoreCase(q.optString("name",""))&&pw.equals(q.optString("pin",""))){currentUser=q.optString("name",un);prefs.edit().putString("user",currentUser).putString("pin",pw).apply();d.dismiss();home();return;}}p.setError("Usuario o clave incorrectos, o usuario desactivado");}));d.show();}
    '''
        s=s[:start]+login+s[end:]

# Gestión de usuarios.
if 'private void users(){' not in s:
    marker='    private void security(){'
    users='''    private void users(){page("Usuarios de la aplicación","Gestiona el acceso local a RgaPro");Button add=action("＋ Añadir usuario",true);content.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));LinearLayout list=col();content.addView(list);Runnable refresh=()->{list.removeAllViews();JSONArray a=appUsers();for(int i=0;i<a.length();i++){final int idx=i;JSONObject q=a.optJSONObject(i);if(q==null)continue;String n=q.optString("name","Sin nombre");boolean active=q.optBoolean("active",true);Button b=action((active?"🟢 ":"⚪ ")+n+(active?" · Activo":" · Desactivado"),false);b.setOnClickListener(v->editAppUser(idx,refresh));list.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));}};add.setOnClickListener(v->addAppUser(refresh));refresh.run();}
    private void addAppUser(Runnable refresh){LinearLayout l=col();EditText n=edit("Nombre de usuario"),p=edit("Clave de 6 dígitos");p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);l.addView(n);l.addView(p);AlertDialog d=new AlertDialog.Builder(this).setTitle("Añadir usuario").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String name=n.getText().toString().trim(),pin=p.getText().toString();if(name.isEmpty()||!pin.matches("\\\\d{6}")){p.setError("Clave de 6 dígitos obligatoria");return;}try{JSONArray a=appUsers();a.put(new JSONObject().put("name",name).put("pin",pin).put("active",true).put("role","USUARIO"));saveAppUsers(a);d.dismiss();refresh.run();}catch(Exception e){p.setError("No se pudo guardar");}}));d.show();}
    private void editAppUser(int idx,Runnable refresh){JSONArray a=appUsers();JSONObject q=a.optJSONObject(idx);if(q==null)return;LinearLayout l=col();EditText n=edit("Nombre");n.setText(q.optString("name",""));EditText p=edit("Nueva clave (opcional)");p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);l.addView(n);l.addView(p);AlertDialog d=new AlertDialog.Builder(this).setTitle("Editar usuario").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{q.put("name",n.getText().toString().trim());if(!p.getText().toString().isEmpty())q.put("pin",p.getText().toString());a.put(idx,q);saveAppUsers(a);d.dismiss();refresh.run();}catch(Exception e){n.setError("No se pudo guardar");}}));d.show();}

'''
    if marker in s:
        s=s.replace(marker,users+marker,1)

MAIN.write_text(s,encoding='utf-8')
print('Robust DNI/user-management patch applied')
