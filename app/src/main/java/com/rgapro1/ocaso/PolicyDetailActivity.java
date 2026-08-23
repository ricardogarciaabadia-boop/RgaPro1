package com.rgapro1.ocaso;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.File;

public class PolicyDetailActivity extends FragmentActivity {
    private static final int BG=0xfff7f9fc, TEXT=0xff1c2736, NAVY=0xff0c2343;
    private JSONObject policy;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(TEXT);v.setTypeface(null,bold?1:0);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);try{policy=new JSONObject(getIntent().getStringExtra("policy_json"));render();}catch(Exception e){Toast.makeText(this,"No se pudo abrir la póliza",Toast.LENGTH_LONG).show();finish();}}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setBackgroundColor(NAVY);head.setPadding(dp(10),dp(8),dp(10),dp(8));
        Button back=button("↩️ VOLVER");back.setTextColor(-1);back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        head.addView(text(policy.optString("type","PÓLIZA")+"  ·  Nº "+policy.optString("number","—"),21,true));root.addView(head);
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(12),dp(12),dp(24));
        addCommon(body);addProductSpecific(body);addInsureds(body);addOcr(body);addDocuments(body);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private void addCommon(LinearLayout body){
        body.addView(text("DATOS DE LA PÓLIZA",18,true));
        body.addView(text("Número de póliza: "+policy.optString("number","—"),16,false));
        body.addView(text("Producto: "+policy.optString("type","—"),16,false));
        body.addView(text("Tomador: "+policy.optString("holder","—"),16,false));
        body.addView(text("DNI/NIE: "+policy.optString("holderDni",policy.optString("identityNumber","—")),16,false));
        body.addView(text("Emisión/efecto: "+first(policy,"issueDate","effectDate","startDate","—"),16,false));
        body.addView(text("Vencimiento/renovación: "+first(policy,"expiry","validityDate","renewalDate","—"),16,false));
    }
    private void addProductSpecific(LinearLayout body){
        String type=policy.optString("type","").toUpperCase();
        body.addView(text("DATOS ESPECÍFICOS",18,true));
        if(type.contains("VIDA")){
            body.addView(text("Dirección: "+policy.optString("address","—"),16,false));
            body.addView(text("Teléfono: "+policy.optString("phone","—"),16,false));
            body.addView(text("Email: "+policy.optString("email","—"),16,false));
            addCapitalFields(body,"Capitales asegurados");
        } else if(type.contains("AHOR")||type.contains("AHORRO")) {
            body.addView(text("Cuota/aportación: "+first(policy,"installment","quota","contribution","—"),16,true));
            addCapitalFields(body,"Capital / garantías");
        } else if(type.contains("HOGAR")) {
            addCapitalFields(body,"Capitales / coberturas");
        } else if(type.contains("ACCIDENT")) {
            addCapitalFields(body,"Capitales / garantías");
        }
    }
    private void addCapitalFields(LinearLayout body,String title){
        JSONArray caps=policy.optJSONArray("insuredCapitals");
        if(caps!=null&&caps.length()>0){body.addView(text(title,16,true));for(int i=0;i<caps.length();i++){Object x=caps.opt(i);if(x!=null&&x!=JSONObject.NULL)body.addView(text(String.valueOf(x),15,false));}return;}
        String c=policy.optString("capital","");if(!c.isEmpty())body.addView(text(title+": "+c,16,false));
    }
    private void addInsureds(LinearLayout body){JSONArray a=policy.optJSONArray("insureds");if(a==null||a.length()==0)return;body.addView(text("ASEGURADOS",18,true));for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;StringBuilder s=new StringBuilder();s.append(p.optString("name","Sin nombre"));s.append(p.optBoolean("holder",false)?"\nTitular · Asegurado":"\nAsegurado");s.append("\nNacimiento: ").append(p.optString("birthDate","—"));s.append("\nDNI/NIE: ").append(p.optString("identityNumber","—"));if(!p.optString("capital","").isEmpty())s.append("\nCapital: ").append(p.optString("capital"));if(!p.optString("accidentCapital","").isEmpty())s.append("\nCapital accidente: ").append(p.optString("accidentCapital"));body.addView(text(s.toString(),15,false));}}
    private void addOcr(LinearLayout body){body.addView(text("DATOS EXTRAÍDOS POR OCR",18,true));String raw=policy.optString("ocrText","").trim();if(raw.isEmpty())raw="No hay texto OCR guardado para esta póliza.";Button b=button("👁️ Ver texto OCR completo");b.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("OCR original").setMessage(raw).setPositiveButton("Cerrar",null).show());body.addView(b,new LinearLayout.LayoutParams(-1,dp(56)));}
    private void addDocuments(LinearLayout body){JSONArray docs=policy.optJSONArray("documentPhotos");body.addView(text("COPIA DE LA PÓLIZA",18,true));if(docs==null||docs.length()==0){body.addView(text("No hay una copia escaneada asociada.",15,false));return;}for(int i=0;i<docs.length();i++){Object x=docs.opt(i);String path=x instanceof JSONObject?((JSONObject)x).optString("path",""):String.valueOf(x);if(path.isEmpty())continue;Button b=button("📄 "+new File(path).getName());b.setOnClickListener(v->openDocument(path));body.addView(b,new LinearLayout.LayoutParams(-1,dp(58)));}}
    private void openDocument(String path){File f=new File(path);if(!f.exists()){Toast.makeText(this,"La copia original ya no está disponible",Toast.LENGTH_LONG).show();return;}try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,mime(path));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){Toast.makeText(this,"No hay una aplicación compatible para abrir la póliza",Toast.LENGTH_LONG).show();}}
    private String mime(String p){String x=p.toLowerCase();if(x.endsWith(".pdf"))return "application/pdf";if(x.endsWith(".png"))return "image/png";if(x.endsWith(".webp"))return "image/webp";return "image/jpeg";}
    private String first(JSONObject o,String a,String b,String c,String fallback){String x=o.optString(a,"");if(!x.isEmpty())return x;x=o.optString(b,"");if(!x.isEmpty())return x;x=o.optString(c,"");if(!x.isEmpty())return x;return fallback;}
}
