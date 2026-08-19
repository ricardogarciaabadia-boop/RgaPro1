package com.rgapro1.ocaso;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import org.json.*;
import java.io.*;

public class Client360Activity extends FragmentActivity {
    private static final int NAVY=0xff0c2343, BLUE=0xff1985e0, BG=0xfff7f9fc, TEXT=0xff1c2736;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView t(String s,int z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(TEXT);v.setTypeface(null,b?1:0);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);String raw=getIntent().getStringExtra("client_json");try{show(new JSONObject(raw==null?"{}":raw));}catch(Exception e){finish();}}
    private void show(JSONObject p){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(10),dp(8),dp(10),dp(8));head.setBackgroundColor(NAVY);
        Button back=btn("↩️  VOLVER");back.setTextColor(-1);back.setTextSize(18);back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));head.addView(t("🔵 CLIENTE 360º",22,true));root.addView(head);
        ScrollView sv=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(12),dp(12),dp(20));
        body.addView(t("👤 "+p.optString("holder",p.optString("name","Cliente")),23,true));
        String id=p.optString("identityNumber",p.optString("holderDni","—"));body.addView(t("Identificación: "+id+"\nTeléfono: "+p.optString("phone","—")+"\nEmail: "+p.optString("email","—")+"\nDirección: "+p.optString("address","—"),16,false));
        addGroup(body,"📦 PRODUCTO / PÓLIZA",p); addGroup(body,"📄 DOCUMENTACIÓN",p); addGroup(body,"🔔 VENCIMIENTO / BAJA",p);
        sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void addGroup(LinearLayout body,String title,JSONObject p){body.addView(t(title,18,true));Button product=btn((p.optString("type","Documento")+"  ·  "+p.optString("number","Sin número")+"\nVencimiento: "+p.optString("expiry",p.optString("validityDate","—"))));product.setOnClickListener(v->showProduct(p));body.addView(product,new LinearLayout.LayoutParams(-1,dp(72)));JSONArray docs=p.optJSONArray("documentPhotos");if(docs!=null){for(int i=0;i<docs.length();i++){String path=docs.optString(i,"");if(path.isEmpty())continue;Button d=btn("📄 "+new File(path).getName());d.setOnClickListener(v->documentMenu(path));body.addView(d,new LinearLayout.LayoutParams(-1,dp(58)));}}}
    private void showProduct(JSONObject p){new AlertDialog.Builder(this).setTitle("Producto / póliza").setMessage("Tipo: "+p.optString("type","—")+"\nNúmero: "+p.optString("number","—")+"\nTitular: "+p.optString("holder","—")+"\nVencimiento: "+p.optString("expiry",p.optString("validityDate","—"))).setPositiveButton("Cerrar",null).show();}
    private void documentMenu(String path){new AlertDialog.Builder(this).setTitle("Documento").setItems(new String[]{"👁️ Abrir / ver","⬇️ Descargar","📤 Compartir"},(d,w)->{if(w==0)open(path);else if(w==1)download(path);else share(path);}).show();}
    private Uri uri(String path){File f=new File(path);if(!f.exists())return null;try{return FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);}catch(Exception e){return Uri.fromFile(f);}}
    private void open(String path){Uri u=uri(path);if(u==null){toast("No se encuentra el documento");return;}Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,mime(path));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(Exception e){toast("No hay aplicación para abrir este documento");}}
    private void share(String path){Uri u=uri(path);if(u==null){toast("No se encuentra el documento");return;}Intent i=new Intent(Intent.ACTION_SEND);i.setType(mime(path));i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Compartir documento"));}
    private void download(String path){File src=new File(path);if(!src.exists()){toast("No se encuentra el documento");return;}try{if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,src.getName());v.put(MediaStore.Downloads.MIME_TYPE,mime(path));v.put(MediaStore.Downloads.IS_PENDING,1);Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(u)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}v.clear();v.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(u,v,null,null);}else{File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!d.exists())d.mkdirs();try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(new File(d,src.getName()))){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}}toast("Documento descargado");}catch(Exception e){toast("No se pudo descargar: "+e.getMessage());}}
    private String mime(String p){String x=p.toLowerCase();if(x.endsWith(".pdf"))return "application/pdf";if(x.endsWith(".png"))return "image/png";return "image/jpeg";}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
