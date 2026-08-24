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
        try{ Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath()); runDniOcr(b,side); }
        catch(Exception e){err();}
    }
    private void scanUri(Uri u,String side){
        try(InputStream in=getContentResolver().openInputStream(u)){ Bitmap b=BitmapFactory.decodeStream(in); runDniOcr(b,side); }
        catch(Exception e){err();}
    }

    private void runDniOcr(Bitmap bitmap,String side){
        if(bitmap==null){err();return;}
        recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(a->{
            deliver(DniOcrParser.parse(a==null?"":a.getText()),side);
        }).addOnFailureListener(x->err());
    }

    private void deliver(DniOcrParser.Result r,String side){
        try{
            JSONObject o=new JSONObject();
            o.put("documentNumber",r.dni);
            o.put("birthDate",r.birthDate);
            o.put("name",r.name);
            o.put("surname",r.surname);
            o.put("address",r.address);
            o.put("side",side==null?"document":side);
            o.put("confidence",r.confidence);
            if("front".equals(side)) frontRaw=JSONObject.valueToString(o);
            if("reverse".equals(side)) reverseRaw=JSONObject.valueToString(o);
            JSONObject merged=mergeResults();
            web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(merged.toString())+");",null);
        }catch(Exception e){err();}
    }

    private JSONObject mergeResults(){
        try{
            JSONObject f=frontRaw.isEmpty()?new JSONObject():new JSONObject(frontRaw);
            JSONObject b=reverseRaw.isEmpty()?new JSONObject():new JSONObject(reverseRaw);
            JSONObject o=new JSONObject();
            o.put("side","front".equals(f.optString("side"))?"front":b.optString("side","reverse"));
            o.put("frontRead",!frontRaw.isEmpty());
            o.put("reverseRead",!reverseRaw.isEmpty());
            o.put("name",first(f,b,"name"));
            o.put("surname",first(f,b,"surname"));
            o.put("documentNumber",first(f,b,"documentNumber"));
            o.put("birthDate",first(f,b,"birthDate"));
            o.put("address",first(f,b,"address"));
            o.put("phone","");
            o.put("email","");
            o.put("confidence",Math.max(f.optInt("confidence",0),b.optInt("confidence",0)));
            return o;
        }catch(Exception e){return new JSONObject();}
    }
    private String first(JSONObject a,JSONObject b,String key){String x=a.optString(key,"");return x.isEmpty()?b.optString(key,""):x;}
    private void err(){Toast.makeText(this,"No se pudo leer el DNI. Haz otra captura.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
