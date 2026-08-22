from pathlib import Path

JAVA=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=JAVA.read_text(encoding='utf-8')

if 'Managed users login v1' in s:
    print('Managed users login already applied')
    raise SystemExit(0)

start=s.find('    private void login(){')
if start<0:
    raise SystemExit('login method not found')
brace=s.find('{',start)
depth=0
end=-1
for i in range(brace,len(s)):
    if s[i]=='{': depth+=1
    elif s[i]=='}':
        depth-=1
        if depth==0:
            end=i+1
            break
if end<0:
    raise SystemExit('unbalanced login method')

new=r'''    // Managed users login v1: authenticate against the application user list.
    private void login(){
        LinearLayout l=col();
        EditText u=edit("Usuario"),p=edit("Clave de 6 dígitos");
        p.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        l.addView(u,new LinearLayout.LayoutParams(-1,dp(54)));
        l.addView(p,new LinearLayout.LayoutParams(-1,dp(54)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Entrar en RgaPro").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            String name=u.getText().toString().trim(),pin=p.getText().toString();
            if(name.isEmpty()||!pin.matches("\\d{6}")){p.setError("Usuario y clave de 6 dígitos obligatorios");return;}
            try{
                JSONArray a=appUsers();
                for(int i=0;i<a.length();i++){
                    JSONObject q=a.optJSONObject(i);if(q==null)continue;
                    if(name.equalsIgnoreCase(q.optString("name",""))&&q.optBoolean("active",true)&&pin.equals(q.optString("pin",""))){
                        currentUser=q.optString("name",name);
                        prefs.edit().putString("user",currentUser).putString("pin",pin).apply();
                        d.dismiss();home();return;
                    }
                }
                p.setError("Usuario, clave incorrectos o usuario desactivado");
            }catch(Exception e){p.setError("No se pudo validar el usuario");}
        }));
        d.show();
    }
'''
s=s[:start]+new+s[end:]
JAVA.write_text(s,encoding='utf-8')
print('Managed users login v1 applied')
