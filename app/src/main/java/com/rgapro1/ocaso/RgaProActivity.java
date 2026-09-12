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

    @Override public void onCreate(android.os.Bundle b){
        super.onCreate(b);
        web=new WebView(this);
        setContentView(web,new ViewGroup.LayoutParams(-1,-1));
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                super.onPageFinished(view,url);
                // El acceso superior de Pólizas se elimina de la navegación principal;
                // las pólizas siguen disponibles dentro de la ficha de cada cliente.
                view.evaluateJavascript("(function(){var b=document.getElementById('n-policies');if(b)b.style.display='none';})();",null);
            }
        });
        web.addJavascriptInterface(new Bridge(),"RgaProCamera");
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        // La versión completa contiene Inicio, Clientes, ficha 360, pólizas, OCR y alarmas.
        web.loadUrl("file:///android_asset/prototype/index.html");
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
            File dir=new File(getCacheDir(),"rgapro_scan");
            if(!dir.exists())dir.mkdirs();
            cameraFile=File.createTempFile("rgapro_",".jpg",dir);
            Uri out=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraFile);
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT,out);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i,CAMERA);
        }catch(Exception e){showError("No se pudo abrir la cámara");}
    }

    @Override protected void onActivityResult(int r,int res,Intent d){
        super.onActivityResult(r,res,d);
        if(r==CAMERA){
            if(res==RESULT_OK&&cameraFile!=null)scanCamera(cameraFile,cameraSide);
            return;
        }
        if(r!=PICK||res!=RESULT_OK||d==null)return;
        if(d.getClipData()!=null){
            for(int i=0;i<d.getClipData().getItemCount();i++){
                Uri u=d.getClipData().getItemAt(i).getUri();
                scanUri(u,"document");
            }
        } else if(d.getData()!=null) scanUri(d.getData(),"document");
    }

    private void scanCamera(File f,String side){
        try{
            Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());
            if(b==null){showError("La foto no es válida");return;}
            runBitmapOcr(b,side,encodePreview(b));
        }catch(Exception e){showError("No se pudo procesar la foto");}
    }

    private void scanUri(Uri u,String side){
        try{
            String type=getContentResolver().getType(u);
            if("application/pdf".equals(type)||String.valueOf(u).toLowerCase(Locale.ROOT).contains(".pdf")){
                PdfOcrHelper.process(this,u,new PdfOcrHelper.Callback(){
                    public void onSuccess(String text){ deliver(parse(text),side,""); }
                    public void onError(Exception e){ showError("No se pudo leer el PDF"); }
                });
                return;
            }
            try(InputStream in=getContentResolver().openInputStream(u)){
                if(in==null)throw new IOException("No se pudo abrir el archivo");
                Bitmap b=BitmapFactory.decodeStream(in);
                if(b==null)throw new IOException("Imagen no válida");
                runBitmapOcr(b,side,encodePreview(b));
            }
        }catch(Exception e){showError("No se pudo abrir el documento");}
    }

    private void runBitmapOcr(Bitmap original,String side,String preview){
        if(original==null){showError("Imagen no válida");return;}
        try{
            recognizer.process(InputImage.fromBitmap(original,0))
                .addOnSuccessListener(a->{
                    String text=a==null?"":a.getText();
                    deliver(parse(text),side,preview);
                    if(!original.isRecycled())original.recycle();
                })
                .addOnFailureListener(x->{
                    if(!original.isRecycled())original.recycle();
                    showError("No se pudo leer el documento");
                });
        }catch(Exception e){
            if(!original.isRecycled())original.recycle();
            showError("No se pudo iniciar el OCR");
        }
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
            final String js="window.setOcrResult("+JSONObject.quote(o.toString())+");";
            // PdfOcrHelper entrega el resultado desde un hilo de trabajo. WebView y Toast
            // deben tocarse exclusivamente desde el hilo de UI; esto evita el cierre de la APK.
            runOnUiThread(()->{
                if(web==null||isFinishing())return;
                web.evaluateJavascript(js,null);
            });
        }catch(Exception e){showError("No se pudo mostrar el resultado OCR");}
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
    private void showError(String message){runOnUiThread(()->Toast.makeText(this,message,Toast.LENGTH_LONG).show());}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
