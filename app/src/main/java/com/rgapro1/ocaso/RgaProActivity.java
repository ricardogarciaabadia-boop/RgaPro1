package com.rgapro1.ocaso;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
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
        try{ Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath()); runBitmapOcr(b,side,encodePreview(b)); }
        catch(Exception e){err();}
    }
    private void scanUri(Uri u,String side){
        String type=getContentResolver().getType(u);
        if("application/pdf".equals(type)||String.valueOf(u).toLowerCase(Locale.ROOT).contains(".pdf")){
            PdfOcrHelper.process(this,u,new PdfOcrHelper.Callback(){
                public void onSuccess(String text){ deliver(parse(text),side,""); }
                public void onError(Exception e){err();}
            });
            return;
        }
        try(InputStream in=getContentResolver().openInputStream(u)){ Bitmap b=BitmapFactory.decodeStream(in); runBitmapOcr(b,side,encodePreview(b)); }
        catch(Exception e){err();}
    }

    private void runBitmapOcr(Bitmap original,String side,String preview){
        if(original==null){err();return;}
        recognizer.process(InputImage.fromBitmap(original,0)).addOnSuccessListener(a->{ deliver(parse(a==null?"":a.getText()),side,preview); }).addOnFailureListener(x->err());
    }

    private JSONObject parse(String raw){
        try{
            DniOcrParser.Result r=DniOcrParser.parse(raw);
            JSONObject o=new JSONObject();
            o.put("documentNumber",r.dni); o.put("birthDate",r.birthDate); o.put("name",r.name); o.put("surname",r.surname); o.put("raw",raw==null?"":raw);
            o.put("policyNumber",findPolicyNumber(raw)); o.put("policyType",findPolicyType(raw)); o.put("classification",classify(raw));
            o.put("phone",find(raw,"(?:\\+34\\s*)?[6789]\\d{8}")); o.put("email",find(raw,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}")); o.put("address",findLabeled(raw,"DIRECCIÓN","DIRECCION","DOMICILIO","RIESGO"));
            return o;
        }catch(Exception e){return new JSONObject();}
    }

    private void deliver(JSONObject o,String side,String preview){
        try{
            o.put("side",side==null?"document":side);
            o.put("preview",preview==null?"":preview);
            if("front".equals(side))frontRaw=o.optString("raw","");
            if("reverse".equals(side))reverseRaw=o.optString("raw","");
            o.put("frontRead",!frontRaw.isEmpty()); o.put("reverseRead",!reverseRaw.isEmpty());
            web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);
        }catch(Exception e){err();}
    }

    private String classify(String raw){
        String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);
        boolean dni=u.contains("DNI")||u.contains("NIE")||u.contains("IDESP")||u.matches("(?s).*\\b[XYZ]?[0-9]{7}[A-Z]\\b.*");
        boolean pol=u.contains("PÓLIZA")||u.contains("POLIZA")||u.contains("TOMADOR")||u.contains("ASEGURADO")||u.contains("FECHA DE EFECTO")||u.contains("CONDICIONES PARTICULARES");
        return dni?"DNI/NIE":pol?"Póliza":"Documento";
    }
    private String findPolicyNumber(String raw){if(raw==null)return"";Matcher m=Pattern.compile("(?i)(?:N[º°O]\\s*)?(?:NÚMERO DE P[ÓO]LIZA|NUMERO DE POLIZA|P[ÓO]LIZA|POLIZA)\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})").matcher(raw);return m.find()?m.group(1).trim():"";}
    private String findPolicyType(String raw){String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);if(u.contains("DECESOS"))return"Decesos";if(u.contains("COMUNIDAD")||u.contains("COMUNIDADES"))return"Comunidades";if(u.contains("HOGAR"))return"Hogar";if(u.contains("AUTO")||u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL"))return"Auto";if(u.contains("VIDA"))return"Vida";return"Póliza";}
    private String findLabeled(String raw,String...labels){if(raw==null)return"";for(String line:raw.split("\\R")){String u=line.toUpperCase(Locale.ROOT);for(String label:labels){int p=u.indexOf(label);if(p>=0){String v=line.substring(Math.min(line.length(),p+label.length())).replaceFirst("^[\\s:.-]+","").trim();if(!v.isEmpty())return v;}}}return"";}
    private String find(String raw,String regex){if(raw==null)return"";Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(raw);return m.find()?m.group():"";}
    private String encodePreview(Bitmap b)throws Exception{return"";}
    private void err(){Toast.makeText(this,"No se pudo leer el documento. Haz otra captura.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
