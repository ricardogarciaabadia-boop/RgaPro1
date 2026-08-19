from pathlib import Path

p=Path('app/src/main/java/com/rgapro1/ocaso/Client360Activity.java')
s=p.read_text(encoding='utf-8')

if 'private void secureShareClient360()' not in s:
    s=s.replace('    private JSONObject client;\n','    private JSONObject client;\n    private android.content.SharedPreferences prefs;\n    private final java.util.concurrent.Executor biometricExecutor=java.util.concurrent.Executors.newSingleThreadExecutor();\n',1)
    s=s.replace('        String raw=getIntent().getStringExtra("client_json");','        prefs=getSharedPreferences("rgapro_local",MODE_PRIVATE);\n        String raw=getIntent().getStringExtra("client_json");',1)
    marker='        Button edit=btn("✏️ EDITAR");edit.setTextSize(16);edit.setOnClickListener(v->editClient());\n'
    repl=marker+'        Button secure=btn("🔐 COMPARTIR");secure.setTextSize(16);secure.setOnClickListener(v->secureShareClient360());\n'
    if marker not in s: raise SystemExit('Client360 header marker not found')
    s=s.replace(marker,repl,1)
    methods=r'''
    private void secureShareClient360(){
        try{
            JSONArray users=new JSONArray(prefs.getString("authorized_users","[]"));
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.ArrayList<Integer> indexes=new java.util.ArrayList<>();
            for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u!=null&&u.optBoolean("enabled",true)&&!u.optString("publicKey","").isEmpty()){names.add(u.optString("username","Usuario"));indexes.add(i);}}
            if(names.isEmpty()){toast("No hay usuarios autorizados. Registra primero al destinatario.");return;}
            new AlertDialog.Builder(this).setTitle("Compartir cliente de forma segura").setItems(names.toArray(new String[0]),(d,w)->confirmSecureClient360(users.optJSONObject(indexes.get(w)))).setNegativeButton("Cancelar",null).show();
        }catch(Exception e){toast("No se pudo abrir el selector de usuarios");}
    }
    private void confirmSecureClient360(JSONObject user){
        if(user==null)return;
        new AlertDialog.Builder(this).setTitle("Autorizar compartición").setMessage("Destinatario: "+user.optString("username","usuario")+"\n\nSe enviará el cliente completo cifrado. Solo ese dispositivo podrá descifrarlo.").setNegativeButton("Cancelar",null).setPositiveButton("Autorizar con biometría",(d,w)->authenticateSecureClient360(user)).show();
    }
    private void authenticateSecureClient360(JSONObject user){
        int r=androidx.biometric.BiometricManager.from(this).canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if(r!=androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS){toast("Se requiere biometría disponible para autorizar el envío");return;}
        androidx.biometric.BiometricPrompt bp=new androidx.biometric.BiometricPrompt(this,biometricExecutor,new androidx.biometric.BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(androidx.biometric.BiometricPrompt.AuthenticationResult result){runOnUiThread(()->sendSecureClient360(user));}});
        bp.authenticate(new androidx.biometric.BiometricPrompt.PromptInfo.Builder().setTitle("Autorizar envío").setSubtitle("Confirma el uso compartido del cliente").setNegativeButtonText("Cancelar").setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK).build());
    }
    private void sendSecureClient360(JSONObject user){
        try{
            String sender=prefs.getString("user","");
            String pkg=SecureShareManager.encryptForRecipient(this,client,user.getString("publicKey"),sender);
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,pkg);startActivity(Intent.createChooser(i,"Enviar cliente cifrado"));
        }catch(Exception e){toast("No se pudo cifrar el cliente");}
    }
'''
    pos=s.rfind('\n}')
    s=s[:pos]+methods+s[pos:]
    p.write_text(s,encoding='utf-8')
