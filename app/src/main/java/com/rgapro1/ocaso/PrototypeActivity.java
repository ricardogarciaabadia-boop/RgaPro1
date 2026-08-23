package com.rgapro1.ocaso;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrototypeActivity extends Activity {
    private static final int PICK=7001, CAMERA=7002;
    private WebView web;
    private ValueCallback<Uri[]> cb;
    private TextRecognizer recognizer;
    private File cameraFile;
    private String cameraSide="front";
    private String frontRaw="", reverseRaw="";

    @Override public void onCreate(android.os.Bundle b){
        super.onCreate(b);
        web=new WebView(this); setContentView(web,new ViewGroup.LayoutParams(-1,-1));
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> c,FileChooserParams p){if(cb!=null)cb.onReceiveValue(null);cb=c;pickDocument();return true;}
        });
        web.addJavascriptInterface(new CameraBridge(),"RgaProCamera");
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        web.loadUrl("file:///android_asset/prototype/index.html");
    }
    private class CameraBridge{
        @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}
        @JavascriptInterface public void pickPdf(){runOnUiThread(PrototypeActivity.this::pickDocument);}
    }
    private void pickDocument(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});startActivityForResult(i,PICK);}
    private void startCamera(String side){
        cameraSide="reverse".equals(side)?"reverse":"front";
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA);return;}
        try{File dir=new File(getCacheDir(),"dni");if(!dir.exists())dir.mkdirs();cameraFile=File.createTempFile("rgapro_dni_",".jpg",dir);Uri out=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraFile);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,out);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int r,int res,Intent d){
        super.onActivityResult(r,res,d);if(r==CAMERA){if(res==RESULT_OK&&cameraFile!=null)scanCamera(cameraFile,cameraSide);return;}if(r!=PICK)return;Uri u=res==RESULT_OK&&d!=null?d.getData():null;if(cb!=null){cb.onReceiveValue(u==null?null:new Uri[]{u});cb=null;}if(u!=null){String type=getContentResolver().getType(u);if("application/pdf".equals(type)||String.valueOf(u).toLowerCase(Locale.ROOT).contains(".pdf"))scanPdf(u);else scan(u,"gallery");}}
    private void scanCamera(File file,String side){try{Bitmap b=BitmapFactory.decodeFile(file.getAbsolutePath());runBitmapOcr(b,side,previewFile(file));}catch(Exception e){err();}}
    private void scan(Uri u,String side){try{InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();runBitmapOcr(b,side,encodePreview(b));}catch(Exception e){err();}}
    private void runBitmapOcr(Bitmap original,String side,String preview){
        if(original==null){err();return;}Bitmap normalized=normalizeBitmap(original,2600),enhanced=enhance(normalized);InputImage a=InputImage.fromBitmap(normalized,0),b=InputImage.fromBitmap(enhanced,0);
        recognizer.process(a).addOnSuccessListener(t1->recognizer.process(b).addOnSuccessListener(t2->{String raw1=t1==null?"":t1.getText(),raw2=t2==null?"":t2.getText();deliverOcr(chooseText(raw1,raw2),side,preview);release(normalized,enhanced,original);}).addOnFailureListener(e->{deliverOcr(t1==null?"":t1.getText(),side,preview);release(normalized,enhanced,original);})).addOnFailureListener(e->{recognizer.process(b).addOnSuccessListener(t->{deliverOcr(t==null?"":t.getText(),side,preview);release(normalized,enhanced,original);}).addOnFailureListener(x->{release(normalized,enhanced,original);err();});});
    }
    private String chooseText(String a,String b){DniOcrParser.Result ra=DniOcrParser.parse(a),rb=DniOcrParser.parse(b);int sa=ra.confidence+(ra.surname.isEmpty()?0:8)+(ra.birthDate.isEmpty()?0:8)+(ra.dni.isEmpty()?0:10),sb=rb.confidence+(rb.surname.isEmpty()?0:8)+(rb.birthDate.isEmpty()?0:8)+(rb.dni.isEmpty()?0:10);return sa>=sb?a+"\n"+b:b+"\n"+a;}
    private void deliverOcr(String raw,String side,String preview){try{if("reverse".equals(side))reverseRaw=raw;else if("front".equals(side))frontRaw=raw;String combined=(frontRaw+"\n"+reverseRaw).trim();JSONObject o=parse(combined.isEmpty()?raw:combined);o.put("side",side);o.put("preview",preview==null?"":preview);o.put("frontRead",!frontRaw.isEmpty());o.put("reverseRead",!reverseRaw.isEmpty());web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"reverse".equals(side)?"Reverso leído: revisa MRZ, DNI y fecha":"Anverso leído: revisa nombre, apellidos, DNI y fecha",Toast.LENGTH_LONG).show();}catch(Exception e){err();}}
    private void scanPdf(Uri u){PdfOcrHelper.process(this,u,new PdfOcrHelper.Callback(){@Override public void onSuccess(String text){runOnUiThread(()->{try{JSONObject o=parse(text);o.put("side","pdf");o.put("preview","");o.put("frontRead",true);o.put("reverseRead",true);web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(PrototypeActivity.this,"PDF procesado. Revisa y edita los datos.",Toast.LENGTH_LONG).show();}catch(Exception e){err();}});}@Override public void onError(Exception e){runOnUiThread(()->Toast.makeText(PrototypeActivity.this,"No se pudo leer el PDF: "+e.getMessage(),Toast.LENGTH_LONG).show());}});}
    private JSONObject parse(String raw)throws Exception{DniOcrParser.Result r=DniOcrParser.parse(raw);JSONObject o=new JSONObject();o.put("documentNumber",r.dni.isEmpty()?"No detectado":r.dni);o.put("birthDate",r.birthDate.isEmpty()?"No detectada":r.birthDate);o.put("expiryDate",r.validityDate.isEmpty()?"No detectada":r.validityDate);o.put("name",r.name.isEmpty()?"No detectado":r.name);o.put("surname",r.surname.isEmpty()?"No detectados":r.surname);o.put("mrzStatus",r.mrz.isEmpty()?"No confirmada":"Detectada: revisar checksum");o.put("confidence",r.confidence);o.put("raw",raw==null?"":raw);o.put("policyNumber",findPolicyNumber(raw));o.put("policyType",findPolicyType(raw));o.put("policyExpiry",r.validityDate);o.put("phone",find(raw,"(?:\\+34\\s*)?[6789]\\d{8}"));o.put("email",find(raw,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));o.put("address",findAddress(raw));return o;}
    private String findPolicyNumber(String raw){if(raw==null)return "";Matcher m=Pattern.compile("(?i)(?:POLIZA|PÓLIZA|N[º°O]\\s*POLIZA)\\s*[:#-]?\\s*([A-Z0-9./-]{5,})").matcher(raw);return m.find()?m.group(1).trim():"";}
    private String findPolicyType(String raw){if(raw==null)return "";String u=raw.toUpperCase(Locale.ROOT);if(u.contains("DECESOS"))return "Decesos";if(u.contains("VIDA"))return "Vida";if(u.contains("HOGAR"))return "Hogar";if(u.contains("AUTO")||u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL"))return "Auto";return "";}
    private String findAddress(String raw){if(raw==null)return "";String[] ls=raw.split("\\R");for(int i=0;i<ls.length;i++){String u=ls[i].toUpperCase(Locale.ROOT);if(u.contains("DOMICILIO")||u.contains("DIRECCION")||u.contains("DIRECCIÓN")){String v=ls[i].replaceFirst("(?i).*?(DOMICILIO|DIRECCION|DIRECCIÓN)\\s*[:.-]?\\s*","");if(!v.trim().isEmpty())return v.trim();if(i+1<ls.length)return ls[i+1].trim();}}return "";}
    private String find(String raw,String regex){if(raw==null)return "";Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(raw);return m.find()?m.group():"";}
    private Bitmap normalizeBitmap(Bitmap src,int max){int w=src.getWidth(),h=src.getHeight();if(Math.max(w,h)<=max)return src;float f=max/(float)Math.max(w,h);return Bitmap.createScaledBitmap(src,Math.max(1,Math.round(w*f)),Math.max(1,Math.round(h*f)),true);}
    private Bitmap enhance(Bitmap src){Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);ColorMatrix cm=new ColorMatrix(new float[]{1.18f,0,0,0,8,0,1.18f,0,0,8,0,0,1.18f,0,8,0,0,0,1,0});p.setColorFilter(new ColorMatrixColorFilter(cm));c.drawBitmap(src,0,0,p);return out;}
    private void release(Bitmap normalized,Bitmap enhanced,Bitmap original){if(enhanced!=normalized&&!enhanced.isRecycled())enhanced.recycle();if(normalized!=original&&!normalized.isRecycled())normalized.recycle();}
    private String previewFile(File f)throws Exception{return encodePreview(BitmapFactory.decodeFile(f.getAbsolutePath()));}
    private String encodePreview(Bitmap b)throws Exception{if(b==null)return "";Bitmap x=normalizeBitmap(b,1400);ByteArrayOutputStream o=new ByteArrayOutputStream();x.compress(Bitmap.CompressFormat.JPEG,82,o);if(x!=b&&!x.isRecycled())x.recycle();return Base64.encodeToString(o.toByteArray(),Base64.NO_WRAP);}
    private void err(){Toast.makeText(this,"No se pudo leer el documento. Haz otra foto con buena luz y encuadre.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
