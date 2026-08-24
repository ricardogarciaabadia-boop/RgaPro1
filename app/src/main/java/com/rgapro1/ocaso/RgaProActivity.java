package com.rgapro1.ocaso;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.JSONObject;
import java.io.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RgaProActivity extends Activity {
    private static final int PICK=8101, CAMERA=8102;
    private WebView web;
    private TextRecognizer recognizer;
    private File cameraFile;
    private String cameraSide="document";
    private String frontRaw="", reverseRaw="";
    private JSONObject frontData=new JSONObject(), reverseData=new JSONObject();

    @Override public void onCreate(android.os.Bundle b){
        super.onCreate(b);
        web=new WebView(this);
        setContentView(web,new ViewGroup.LayoutParams(-1,-1));
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(),"RgaProCamera");
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        web.loadUrl("file:///android_asset/prototype/index_v3.html");
    }

    private class Bridge {
        @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}
        @JavascriptInterface public void pickPdf(){runOnUiThread(RgaProActivity.this::pickDocument);}
        @JavascriptInterface public void saveDni(String json){runOnUiThread(()->saveDniResult(json));}
    }

    private void pickDocument(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/jpg","application/pdf"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i,PICK);
    }

    private void startCamera(String side){
        cameraSide=side==null?"document":side;
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA); return;
        }
        try{
            File dir=new File(getCacheDir(),"rgapro_scan"); if(!dir.exists())dir.mkdirs();
            cameraFile=File.createTempFile("rgapro_",".jpg",dir);
            Uri out=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraFile);
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT,out);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i,CAMERA);
        }catch(Exception e){Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_LONG).show();}
    }

    @Override protected void onActivityResult(int r,int res,Intent d){
        super.onActivityResult(r,res,d);
        if(r==CAMERA){ if(res==RESULT_OK&&cameraFile!=null)scanCamera(cameraFile,cameraSide); return; }
        if(r!=PICK||res!=RESULT_OK||d==null)return;
        if(d.getClipData()!=null){
            for(int i=0;i<d.getClipData().getItemCount();i++) scanUri(d.getClipData().getItemAt(i).getUri(),"document");
        } else if(d.getData()!=null) scanUri(d.getData(),"document");
    }

    private void scanCamera(File f,String side){
        try{ Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath()); runBitmapOcr(b,side); }
        catch(Exception e){err();}
    }
    private void scanUri(Uri u,String side){
        String type=getContentResolver().getType(u);
        if("application/pdf".equals(type)||String.valueOf(u).toLowerCase(Locale.ROOT).contains(".pdf")){
            PdfOcrHelper.process(this,u,new PdfOcrHelper.Callback(){
                public void onSuccess(String text){ deliver(parse(text),side); }
                public void onError(Exception e){err();}
            });
            return;
        }
        try(InputStream in=getContentResolver().openInputStream(u)){ Bitmap b=BitmapFactory.decodeStream(in); runBitmapOcr(b,side); }
        catch(Exception e){err();}
    }

    private void runBitmapOcr(Bitmap original,String side){
        if(original==null){err();return;}
        recognizer.process(InputImage.fromBitmap(original,0)).addOnSuccessListener(a->{ deliver(parse(a==null?"":a.getText()),side); original.recycle(); }).addOnFailureListener(x->{ original.recycle(); err(); });
    }

    private JSONObject parse(String raw){
        try{
            DniOcrParser.Result r=DniOcrParser.parse(raw);
            JSONObject o=new JSONObject();
            o.put("documentNumber",r.dni); o.put("birthDate",r.birthDate); o.put("name",r.name); o.put("surname",r.surname);
            o.put("address",r.address); o.put("phone",r.phone); o.put("email",r.email); o.put("confidence",r.confidence);
            o.put("raw",raw==null?"":raw);
            o.put("policyNumber",findPolicyNumber(raw)); o.put("policyType",findPolicyType(raw)); o.put("classification",classify(raw));
            return o;
        }catch(Exception e){return new JSONObject();}
    }

    private void deliver(JSONObject o,String side){
        try{
            o.put("side",side==null?"document":side);
            if("front".equals(side)){frontRaw=o.optString("raw","");frontData=o;}
            if("reverse".equals(side)){reverseRaw=o.optString("raw","");reverseData=o;}
            o.put("frontRead",!frontRaw.isEmpty()); o.put("reverseRead",!reverseRaw.isEmpty());
            o.put("combined",mergeDni(frontData,reverseData));
            web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);
        }catch(Exception e){err();}
    }

    private JSONObject mergeDni(JSONObject a,JSONObject b){
        try{
            JSONObject m=new JSONObject();
            String[] keys={"name","surname","documentNumber","birthDate","address","phone","email","confidence"};
            for(String k:keys){String av=a.optString(k,"");String bv=b.optString(k,"");m.put(k,!av.isEmpty()?av:bv);}
            m.put("side","combined");
            return m;
        }catch(Exception e){return new JSONObject();}
    }

    private void saveDniResult(String json){
        try{
            JSONObject o=new JSONObject(json==null?"{}":json);
            JSONObject merged=o.optJSONObject("combined");
            if(merged==null)merged=o;
            getSharedPreferences("rgapro_dni",MODE_PRIVATE).edit().putString("last",merged.toString()).apply();
            Toast.makeText(this,"DNI guardado. Se relacionará con el cliente correspondiente cuando coincidan sus datos.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"No se pudo guardar el DNI",Toast.LENGTH_LONG).show();}
    }

    private String classify(String raw){
        String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);
        boolean dni=u.contains("DNI")||u.contains("NIE")||u.contains("IDESP")||u.matches("(?s).*\\b[XYZ]?[0-9]{7}[A-Z]\\b.*");
        boolean pol=u.contains("PÓLIZA")||u.contains("POLIZA")||u.contains("TOMADOR")||u.contains("ASEGURADO")||u.contains("FECHA DE EFECTO")||u.contains("CONDICIONES PARTICULARES");
        return dni?"DNI/NIE":pol?"Póliza":"Documento";
    }
    private String findPolicyNumber(String raw){if(raw==null)return"";Matcher m=Pattern.compile("(?i)(?:N[º°O]\\s*)?(?:NÚMERO DE P[ÓO]LIZA|NUMERO DE POLIZA|P[ÓO]LIZA|POLIZA)\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})").matcher(raw);return m.find()?m.group(1).trim():"";}
    private String findPolicyType(String raw){String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);if(u.contains("DECESOS"))return"Decesos";if(u.contains("COMUNIDAD")||u.contains("COMUNIDADES"))return"Comunidades";if(u.contains("HOGAR"))return"Hogar";if(u.contains("AUTO")||u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL"))return"Auto";if(u.contains("VIDA"))return"Vida";return"Póliza";}
    private void err(){Toast.makeText(this,"No se pudo leer el documento. Haz otra captura.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
