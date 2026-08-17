package com.rgapro1.ocaso;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {
    private static final int NAVY=Color.rgb(24,52,92), BLUE=Color.rgb(35,120,225), BG=Color.rgb(247,249,252), TEXT=Color.rgb(28,38,52), MUTED=Color.rgb(100,112,128);
    private static final int ITER=120000, CAMERA_REQ=7001, CAMERA_RESULT=7002;
    private SharedPreferences prefs; private String currentUser; private LinearLayout content;
    private final Executor biometricExecutor=Executors.newSingleThreadExecutor();
    private CancellationSignal biometricCancellation;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s,float z,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);v.setPadding(dp(6),dp(6),dp(6),dp(6));return v;}
    private EditText edit(String h){EditText e=new EditText(this);e.setHint(h);e.setSingleLine(true);return e;}
    private EditText pin(String h){EditText e=edit(h);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);return e;}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);if(primary){b.setTextColor(Color.WHITE);b.setBackgroundColor(BLUE);}return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);try{prefs=getSharedPreferences("rgapro_local",MODE_PRIVATE);migrate();if(hasPin())showLogin();else createUser();}catch(Exception e){safeError();}}
    private void safeError(){LinearLayout l=col();l.setGravity(Gravity.CENTER);l.setPadding(dp(24),dp(24),dp(24),dp(24));l.addView(text("RgaPro",32,NAVY,true));l.addView(text("No se pudo iniciar. Tus datos locales no se han borrado.",16,TEXT,false));Button r=button("Reintentar",true);r.setOnClickListener(v->onCreate(null));l.addView(r);setContentView(l);}
    private boolean hasPin(){return prefs.contains("pin_hash")&&prefs.contains("pin_salt");}
    private JSONObject users(){try{return new JSONObject(prefs.getString("users_json","{}"));}catch(Exception e){return new JSONObject();}}
    private void saveUsers(JSONObject u){prefs.edit().putString("users_json",u.toString()).apply();}
    private void migrate(){try{JSONObject u=users();if(u.length()==0&&hasPin()){JSONObject a=new JSONObject();a.put("pin_hash",prefs.getString("pin_hash",""));a.put("pin_salt",prefs.getString("pin_salt",""));a.put("policies",new JSONArray());u.put("Administrador",a);saveUsers(u);}}catch(Exception ignored){}}
    private byte[] derive(String p,byte[] salt)throws Exception{PBEKeySpec s=new PBEKeySpec(p.toCharArray(),salt,ITER,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(s).getEncoded();}finally{s.clearPassword();}}
    private boolean verify(String user,String p){try{JSONObject o=users().optJSONObject(user);if(o==null)return false;byte[] h=android.util.Base64.decode(o.optString("pin_hash"),android.util.Base64.DEFAULT);byte[] s=android.util.Base64.decode(o.optString("pin_salt"),android.util.Base64.DEFAULT);return MessageDigest.isEqual(h,derive(p,s));}catch(Exception e){return false;}}
    private void createUser(){LinearLayout l=col();l.setGravity(Gravity.CENTER);l.setPadding(dp(24),dp(24),dp(24),dp(24));l.addView(text("RgaPro",34,NAVY,true));l.addView(text("Tu cartera, clara y bajo tu control ✨",17,MUTED,false));EditText n=edit("Nombre de usuario"),p=pin("Clave de 6 dígitos"),p2=pin("Repite la clave");l.addView(n);l.addView(p);l.addView(p2);Button b=button("Crear cartera",true);l.addView(b);b.setOnClickListener(v->{String name=n.getText().toString().trim(),a=p.getText().toString(),c=p2.getText().toString();if(name.isEmpty()||!a.matches("\\d{6}")||!a.equals(c)){Toast.makeText(this,"Nombre y clave de 6 dígitos obligatorios",Toast.LENGTH_LONG).show();return;}try{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);JSONObject o=new JSONObject();o.put("pin_hash",android.util.Base64.encodeToString(derive(a,salt),android.util.Base64.NO_WRAP));o.put("pin_salt",android.util.Base64.encodeToString(salt,android.util.Base64.NO_WRAP));o.put("policies",new JSONArray());JSONObject u=users();u.put(name,o);saveUsers(u);prefs.edit().putString("pin_hash",o.getString("pin_hash")).putString("pin_salt",o.getString("pin_salt")).apply();currentUser=name;home();}catch(Exception e){Toast.makeText(this,"No se pudo crear la cartera",Toast.LENGTH_LONG).show();}});setContentView(l);}
    private void showLogin(){LinearLayout l=col();l.setGravity(Gravity.CENTER);l.setPadding(dp(24),dp(24),dp(24),dp(24));l.addView(text("RgaPro",34,NAVY,true));l.addView(text("🔐 Acceso seguro",20,TEXT,true));Spinner s=new Spinner(this);ArrayList<String> names=new ArrayList<>();Iterator<String> it=users().keys();while(it.hasNext())names.add(it.next());Collections.sort(names);if(names.isEmpty()){createUser();return;}s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));l.addView(s);Button b=button("Entrar con clave",true);b.setOnClickListener(v->login(String.valueOf(s.getSelectedItem())));l.addView(b);Button bio=button("🔐 Entrar con huella / biometría",false);bio.setOnClickListener(v->biometricLogin(String.valueOf(s.getSelectedItem())));l.addView(bio);setContentView(l);}
    private void login(String user){EditText e=pin("Clave de 6 dígitos");AlertDialog d=new AlertDialog.Builder(this).setTitle("Desbloquear · "+user).setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Entrar",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{if(verify(user,e.getText().toString())){currentUser=user;d.dismiss();home();}else e.setError("Clave incorrecta");}));d.show();}
    private void biometricLogin(String user){if(Build.VERSION.SDK_INT<28){Toast.makeText(this,"La biometría requiere Android 9 o superior. Usa tu clave.",Toast.LENGTH_LONG).show();return;}BiometricPrompt.AuthenticationCallback cb=new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){runOnUiThread(()->{currentUser=user;home();});}@Override public void onAuthenticationError(int e,CharSequence m){runOnUiThread(()->Toast.makeText(MainActivity.this,"Biometría no disponible: "+m,Toast.LENGTH_SHORT).show());}};BiometricPrompt prompt=new BiometricPrompt.Builder(this).setTitle("RgaPro").setSubtitle("Acceso seguro a tu cartera").setDescription("Confirma tu identidad con la biometría del dispositivo").setNegativeButton("Usar clave",getMainExecutorCompat(),d->{}).build();biometricCancellation=new CancellationSignal();prompt.authenticate(biometricCancellation,biometricExecutor,cb);}
    private Executor getMainExecutorCompat(){return command->runOnUiThread(command);}
    private JSONArray policies(){try{JSONObject o=users().optJSONObject(currentUser);return o==null?new JSONArray():o.optJSONArray("policies");}catch(Exception e){return new JSONArray();}}
    private void home(){LinearLayout l=col();l.setBackgroundColor(BG);TextView h=text("Hola, "+currentUser+" 👋",25,Color.WHITE,true);h.setBackgroundColor(NAVY);h.setPadding(dp(20),dp(20),dp(20),dp(20));l.addView(h);l.addView(text("¿Qué quieres hacer hoy?",19,TEXT,true));add(l,"👥 Clientes\nBuscar por nombre, DNI, teléfono o póliza",v->clients());add(l,"📁 Pólizas\nSeparadas por tipo de seguro",v->policiesPage());add(l,"📷 Escanear documento\nOCR para leer datos y localizar clientes",v->scanDocument());add(l,"⏰ Vencimientos\n60 · 45 · 30 · 15 días",v->expiries());add(l,"🔐 Seguridad\nUsuarios, biometría y privacidad",v->security());Button out=button("Cerrar sesión",false);out.setOnClickListener(v->{currentUser=null;showLogin();});l.addView(out);setContentView(l);}
    private void add(LinearLayout l,String s,View.OnClickListener c){Button b=button(s,false);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setOnClickListener(c);l.addView(b,new LinearLayout.LayoutParams(-1,dp(78)));}
    private void page(String title){LinearLayout root=col();root.setBackgroundColor(BG);Button back=button("‹  "+title,false);back.setOnClickListener(v->home());root.addView(back);content=col();content.setPadding(dp(16),dp(16),dp(16),dp(16));ScrollView sc=new ScrollView(this);sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void clients(){page("👥 Clientes");EditText q=edit("Buscar nombre, DNI, teléfono, póliza...");content.addView(q);LinearLayout list=col();content.addView(list);Runnable render=()->{list.removeAllViews();JSONArray a=policies();for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&matches(p,q.getText().toString())){Button b=button(p.optString("holder","Sin titular")+"\n"+p.optString("type","Otros")+" · "+p.optString("number",""),false);b.setOnClickListener(v->detail(p));list.addView(b);}}};q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){render.run();}public void afterTextChanged(android.text.Editable e){}});render.run();}
    private boolean matches(JSONObject p,String q){if(q==null||q.trim().isEmpty())return true;String n=q.toLowerCase(Locale.ROOT);for(String k:new String[]{"holder","holderDni","phone","email","address","type","number","expiry","ocrText"})if(p.optString(k).toLowerCase(Locale.ROOT).contains(n))return true;return false;}
    private void detail(JSONObject p){String msg="Titular: "+p.optString("holder")+"\nDNI: "+p.optString("holderDni")+"\nTeléfono: "+p.optString("phone")+"\nTipo: "+p.optString("type")+"\nPóliza: "+p.optString("number")+"\nVencimiento: "+p.optString("expiry");new AlertDialog.Builder(this).setTitle("Ficha de póliza").setMessage(msg).setPositiveButton("Cerrar",null).setNeutralButton("Compartir",(d,w)->share(msg)).show();}
    private void share(String msg){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,msg);startActivity(Intent.createChooser(i,"Elegir aplicación para compartir"));}
    private void policiesPage(){page("📁 Pólizas");for(String type:new String[]{"Todas","Decesos","Vida","Hogar","Auto","Salud","Accidentes","Otros"}){Button b=button(type,false);b.setOnClickListener(v->filter(type));content.addView(b);}}
    private void filter(String type){page("📁 "+type);JSONArray a=policies();for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&(type.equals("Todas")||type.equals(p.optString("type")))){Button b=button(p.optString("holder")+"\n"+p.optString("number"),false);b.setOnClickListener(v->detail(p));content.addView(b);}}}
    private void expiries(){page("⏰ Vencimientos");JSONArray a=policies();for(int days:new int[]{60,45,30,15}){content.addView(text("Próximas a "+days+" días",18,TEXT,true));for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p!=null&&daysToExpiry(p.optString("expiry"))==days)content.addView(button(p.optString("holder")+" · "+p.optString("type"),false));}}}
    private int daysToExpiry(String value){try{Date d=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(value);return (int)Math.round((d.getTime()-System.currentTimeMillis())/86400000.0);}catch(Exception e){return -1;}}
    private void security(){page("🔐 Seguridad");content.addView(text("Acceso por clave de 6 dígitos y biometría del dispositivo cuando está disponible.",16,TEXT,false));content.addView(text("Cada usuario tiene su cartera separada. Compartir datos solo se hace al pulsar Compartir.",16,TEXT,false));}
    private void scanDocument(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQ);return;}launchCamera();}
    private void launchCamera(){try{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,CAMERA_RESULT);else Toast.makeText(this,"No hay cámara disponible",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_LONG).show();}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==CAMERA_REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)launchCamera();else if(r==CAMERA_REQ)Toast.makeText(this,"Necesito permiso de cámara para escanear documentos",Toast.LENGTH_LONG).show();}
    @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r==CAMERA_RESULT&&result==RESULT_OK&&data!=null&&data.getExtras()!=null){Object o=data.getExtras().get("data");if(o instanceof Bitmap)runOcr((Bitmap)o);else Toast.makeText(this,"No se obtuvo la imagen",Toast.LENGTH_LONG).show();}}
    private void runOcr(Bitmap bitmap){Toast.makeText(this,"Leyendo documento…",Toast.LENGTH_SHORT).show();InputImage image=InputImage.fromBitmap(bitmap,0);TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);recognizer.process(image).addOnSuccessListener(result->{String raw=result.getText();showOcrResult(raw);}).addOnFailureListener(e->Toast.makeText(this,"No se pudo leer el documento: "+e.getMessage(),Toast.LENGTH_LONG).show()).addOnCompleteListener(t->recognizer.close());}
    private void showOcrResult(String raw){String clean=raw==null?"":raw.trim();String dni=extractDni(clean),phone=extractPhone(clean),email=extractEmail(clean);String summary="Texto leído:\n\n"+clean+"\n\nDatos detectados:\nDNI/NIE: "+(dni.isEmpty()?"—":dni)+"\nTeléfono: "+(phone.isEmpty()?"—":phone)+"\nEmail: "+(email.isEmpty()?"—":email);new AlertDialog.Builder(this).setTitle("📷 Documento leído").setMessage(summary).setNegativeButton("Cerrar",null).setPositiveButton("Buscar en clientes",(d,w)->clientsWithQuery(!dni.isEmpty()?dni:(!phone.isEmpty()?phone:email))).show();}
    private String extractDni(String s){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\b[XYZ]?\\d{7,8}[A-Z]\\b",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(s.replace(" ",""));return m.find()?m.group().toUpperCase(Locale.ROOT):"";}
    private String extractPhone(String s){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\b(?:\\+34\\s?)?[6789]\\d{8}\\b").matcher(s.replace("-"," "));return m.find()?m.group():"";}
    private String extractEmail(String s){java.util.regex.Matcher m=java.util.regex.Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(s);return m.find()?m.group():"";}
    private void clientsWithQuery(String query){clients();if(content!=null&&content.getChildCount()>0&&content.getChildAt(0) instanceof EditText){EditText q=(EditText)content.getChildAt(0);q.setText(query==null?"":query);q.setSelection(q.length());}}
    @Override protected void onDestroy(){if(biometricCancellation!=null)biometricCancellation.cancel();super.onDestroy();}
}
