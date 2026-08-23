package com.rgapro1.ocaso;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RgaProActivity extends Activity {
    private static final int PICK=8101,CAMERA=8102;
    private WebView web; private ValueCallback<Uri[]> cb; private TextRecognizer recognizer; private File cameraFile; private String cameraSide="front"; private String frontRaw="",reverseRaw="";
    @Override public void onCreate(android.os.Bundle b){super.onCreate(b);web=new WebView(this);setContentView(web,new ViewGroup.LayoutParams(-1,-1));WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> c,FileChooserParams p){if(cb!=null)cb.onReceiveValue(null);cb=c;pickDocument();return true;}});web.addJavascriptInterface(new Bridge(),"RgaProCamera");recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);web.loadUrl("file:///android_asset/prototype/index_v3.html");}
    private class Bridge{@JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}@JavascriptInterface public void pickPdf(){runOnUiThread(RgaProActivity.this::pickDocument);}}
    private void pickDocument(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});startActivityForResult(i,PICK);}
    private void startCamera(String side){cameraSide="reverse".equals(side)?"reverse":"front";if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA);return;}try{File dir=new File(getCacheDir(),"dni");if(!dir.exists())dir.mkdirs();cameraFile=File.createTempFile("rgapro_dni_",".jpg",dir);Uri out=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraFile);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,out);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_LONG).show();}}
    @Override protected void onActivityResult(int r,int res,Intent d){super.onActivityResult(r,res,d);if(r==CAMERA){if(res==RESULT_OK&&cameraFile!=null)scanCamera(cameraFile,cameraSide);return;}if(r!=PICK)return;Uri u=res==RESULT_OK&&d!=null?d.getData():null;if(cb!=null){cb.onReceiveValue(u==null?null:new Uri[]{u});cb=null;}if(u!=null){String type=getContentResolver().getType(u);if("application/pdf".equals(type)||String.valueOf(u).toLowerCase(Locale.ROOT).contains(".pdf"))scanPdf(u);else scan(u,"gallery");}}
    private void scanCamera(File f,String side){try{Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());runBitmapOcr(b,side,encodePreview(b));}catch(Exception e){err();}}
    private void scan(Uri u,String side){try(InputStream in=getContentResolver().openInputStream(u)){Bitmap b=BitmapFactory.decodeStream(in);runBitmapOcr(b,side,encodePreview(b));}catch(Exception e){err();}}
    private void runBitmapOcr(Bitmap original,String side,String preview){if(original==null){err();return;}Bitmap n=normalize(original,2600),e=enhance(n);recognizer.process(InputImage.fromBitmap(n,0)).addOnSuccessListener(a->recognizer.process(InputImage.fromBitmap(e,0)).addOnSuccessListener(b->{deliver(choose(a==null?"":a.getText(),b==null?"":b.getText()),side,preview);release(n,e,original);}).addOnFailureListener(x->{deliver(a==null?"":a.getText(),side,preview);release(n,e,original);})).addOnFailureListener(x->{recognizer.process(InputImage.fromBitmap(e,0)).addOnSuccessListener(t->{deliver(t==null?"":t.getText(),side,preview);release(n,e,original);}).addOnFailureListener(y->{release(n,e,original);err();});});}
    private String choose(String a,String b){DniOcrParser.Result x=DniOcrParser.parse(a),y=DniOcrParser.parse(b);int sx=x.confidence+(x.surname.isEmpty()?0:10)+(x.birthDate.isEmpty()?0:10)+(x.dni.isEmpty()?0:10);int sy=y.confidence+(y.surname.isEmpty()?0:10)+(y.birthDate.isEmpty()?0:10)+(y.dni.isEmpty()?0:10);return sx>=sy?a+"\n"+b:b+"\n"+a;}
    private void deliver(String raw,String side,String preview){try{if("reverse".equals(side))reverseRaw=raw;else if("front".equals(side))frontRaw=raw;String combined=(frontRaw+"\n"+reverseRaw).trim();JSONObject o=parse(combined.isEmpty()?raw:combined);o.put("side",side);o.put("preview",preview==null?"":preview);o.put("frontRead",!frontRaw.isEmpty());o.put("reverseRead",!reverseRaw.isEmpty());web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"reverse".equals(side)?"Reverso leído: revisa MRZ, DNI y fecha":"Documento leído: revisa los campos",Toast.LENGTH_LONG).show();}catch(Exception e){err();}}
    private void scanPdf(Uri u){PdfOcrHelper.process(this,u,new PdfOcrHelper.Callback(){public void onSuccess(String text){runOnUiThread(()->{try{JSONObject o=parse(text);o.put("side","pdf");o.put("preview","");o.put("frontRead",true);o.put("reverseRead",true);web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(RgaProActivity.this,"PDF completo procesado. Revisa los datos.",Toast.LENGTH_LONG).show();}catch(Exception e){err();}});}public void onError(Exception e){runOnUiThread(()->Toast.makeText(RgaProActivity.this,"No se pudo leer el PDF: "+e.getMessage(),Toast.LENGTH_LONG).show());}});}
    private JSONObject parse(String raw)throws Exception{DniOcrParser.Result r=DniOcrParser.parse(raw);JSONObject o=new JSONObject();o.put("documentNumber",r.dni);o.put("birthDate",r.birthDate);o.put("name",r.name);o.put("surname",r.surname);o.put("mrzStatus",r.mrz.isEmpty()?"No confirmada":"Detectada: revisar checksum");o.put("confidence",r.confidence);o.put("raw",raw==null?"":raw);o.put("policyNumber",findPolicyNumber(raw));o.put("policyType",findPolicyType(raw));o.put("policyExpiry",findDate(raw,"(?:VENCIMIENTO|VENC|FECHA DE VENCIMIENTO|VALIDEZ|CADUCIDAD)"));o.put("effectiveDate",findDate(raw,"(?:FECHA DE EFECTO|EFECTO|INICIO)"));o.put("holder",findLabeled(raw,"TOMADOR","ASEGURADO","CLIENTE","TITULAR"));o.put("capital",findLabeled(raw,"CAPITAL","SUMA ASEGURADA","CAPITALES","CAPITAL ASEGURADO"));o.put("phone",find(raw,"(?:\\+34\\s*)?[6789]\\d{8}"));o.put("email",find(raw,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));o.put("address",findLabeled(raw,"DIRECCIÓN","DIRECCION","DOMICILIO","RIESGO"));o.put("company",findCompany(raw));return o;}
    private String findPolicyNumber(String raw){if(raw==null)return"";Matcher m=Pattern.compile("(?i)(?:N[º°O]\\s*)?P[ÓO]LIZA\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})").matcher(raw);if(m.find())return m.group(1).trim();m=Pattern.compile("(?i)(?:PRODUCTO|P[ÓO]LIZA|SUBP[ÓO]LIZA)[^0-9A-Z]{0,10}([0-9]{5,12})").matcher(raw);return m.find()?m.group(1):"";}
    private String findPolicyType(String raw){String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);if(u.contains("ASISTENCIA FAMILIAR"))return"Asistencia Familiar";if(u.contains("DECESOS"))return"Decesos";if(u.contains("VIDA"))return"Vida";if(u.contains("HOGAR"))return"Hogar";if(u.contains("COMUNIDAD")||u.contains("COMUNIDADES"))return"Comunidades";if(u.contains("RESPONSABILIDAD CIVIL"))return"Responsabilidad Civil";if(u.contains("AUTO")||u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL"))return"Auto";return"Póliza";}
    private String findCompany(String raw){String u=(raw==null?"":raw).toUpperCase(Locale.ROOT);return u.contains("OCASO")?"Ocaso":"";}
    private String findLabeled(String raw,String...labels){if(raw==null)return"";for(String line:raw.split("\\R")){String u=line.toUpperCase(Locale.ROOT);for(String label:labels){int p=u.indexOf(label);if(p>=0){String v=line.substring(Math.min(line.length(),p+label.length())).replaceFirst("^[\\s:.-]+","").trim();if(!v.isEmpty())return v;}}}return"";}
    private String findDate(String raw,String labels){if(raw==null)return"";Matcher m=Pattern.compile("(?i)"+labels+"\\s*[:.-]?\\s*(\\d{2}\\s*[ /.-]\\s*\\d{2}\\s*[ /.-]\\s*\\d{4})").matcher(raw);return m.find()?m.group(1).replaceAll("\\s+","").replace('-','/').replace('.','/') :"";}
    private String find(String raw,String regex){if(raw==null)return"";Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(raw);return m.find()?m.group():"";}
    private Bitmap normalize(Bitmap s,int max){int w=s.getWidth(),h=s.getHeight();if(Math.max(w,h)<=max)return s;float f=max/(float)Math.max(w,h);return Bitmap.createScaledBitmap(s,Math.max(1,Math.round(w*f)),Math.max(1,Math.round(h*f)),true);}
    private Bitmap enhance(Bitmap src){Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(out);android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);android.graphics.ColorMatrix cm=new android.graphics.ColorMatrix(new float[]{1.18f,0,0,0,8,0,1.18f,0,0,8,0,0,1.18f,0,8,0,0,0,1,0});p.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));c.drawBitmap(src,0,0,p);return out;}
    private void release(Bitmap n,Bitmap e,Bitmap o){if(e!=n&&!e.isRecycled())e.recycle();if(n!=o&&!n.isRecycled())n.recycle();}
    private String encodePreview(Bitmap b)throws Exception{if(b==null)return"";Bitmap x=normalize(b,1400);ByteArrayOutputStream out=new ByteArrayOutputStream();x.compress(Bitmap.CompressFormat.JPEG,82,out);if(x!=b&&!x.isRecycled())x.recycle();return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);}
    private void err(){Toast.makeText(this,"No se pudo leer el documento. Haz otra foto con buena luz y encuadre.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
