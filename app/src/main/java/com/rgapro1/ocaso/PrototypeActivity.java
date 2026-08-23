package com.rgapro1.ocaso;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrototypeActivity extends Activity {
 private static final int PICK=7001; private WebView web; private ValueCallback<Uri[]> cb; private TextRecognizer recognizer;
 @Override public void onCreate(Bundle b){super.onCreate(b); web=new WebView(this); setContentView(web,new ViewGroup.LayoutParams(-1,-1)); WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true); web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> c,FileChooserParams p){if(cb!=null)cb.onReceiveValue(null);cb=c;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK);return true;}}); recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); web.loadUrl("file:///android_asset/prototype/index.html");}
 @Override protected void onActivityResult(int r,int res,Intent d){super.onActivityResult(r,res,d);if(r!=PICK)return;Uri u=res==RESULT_OK&&d!=null?d.getData():null;if(cb!=null){cb.onReceiveValue(u==null?null:new Uri[]{u});cb=null;}if(u!=null)scan(u);}
 private void scan(Uri u){try{InputImage img=InputImage.fromFilePath(this,u);recognizer.process(img).addOnSuccessListener(t->{try{JSONObject o=parse(t);o.put("preview",preview(u));web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"OCR terminado: revisa los campos antes de guardar",Toast.LENGTH_LONG).show();}catch(Exception e){err();}}).addOnFailureListener(e->err());}catch(Exception e){err();}}
 private JSONObject parse(Text t)throws Exception{String raw=t.getText()==null?"":t.getText();String flat=raw.replace('\n',' ').replaceAll("\\s+"," ").trim();String doc=find(flat,"(?i)\\b(?:[XYZ]\\d{7}[A-Z]|\\d{8}[A-Z])\\b");String date=find(flat,"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");String[] lines=raw.split("\\R");String name=findName(lines);String exp=findExpiry(lines);boolean mrz=mrz(lines);JSONObject o=new JSONObject(),c=new JSONObject(),v=new JSONObject();o.put("documentNumber",doc.isEmpty()?"No detectado":doc);o.put("birthDate",date.isEmpty()?"No detectada":date);o.put("expiryDate",exp.isEmpty()?"No detectada":exp);o.put("name",name.isEmpty()?"No detectado":name);o.put("mrzStatus",mrz?"Detectada: revisar checksum":"No confirmada");c.put("Nº documento",doc.isEmpty()?0:98);c.put("Nombre y apellidos",name.isEmpty()?0:92);c.put("Fecha de nacimiento",date.isEmpty()?0:96);c.put("Fecha de caducidad",exp.isEmpty()?0:96);c.put("Validación MRZ",mrz?85:0);v.put("Nº documento",!doc.isEmpty());v.put("Nombre y apellidos",!name.isEmpty());v.put("Fecha de nacimiento",!date.isEmpty());v.put("Fecha de caducidad",!exp.isEmpty());v.put("Validación MRZ",false);o.put("confidence",c);o.put("verified",v);o.put("raw",raw);return o;}
 private String find(String s,String r){Matcher m=Pattern.compile(r).matcher(s);return m.find()?m.group():"";}
 private String findName(String[] ls){for(int i=0;i<ls.length;i++){String u=ls[i].trim().toUpperCase(Locale.ROOT);if(u.contains("NOMBRE")&&i+1<ls.length){String n=ls[i+1].trim();if(n.length()>2&&!n.matches(".*\\d.*"))return n;}}for(String x:ls){if(x.contains("<<")&&x.length()>10)return x.replace('<',' ').replaceAll("\\s+"," ").trim();}return "";}
 private String findExpiry(String[] ls){for(int i=0;i<ls.length;i++){if(ls[i].toUpperCase(Locale.ROOT).contains("VALIDEZ")){String d=find(ls[i],"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");if(!d.isEmpty())return d;if(i+1<ls.length){d=find(ls[i+1],"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");if(!d.isEmpty())return d;}}}return "";}
 private boolean mrz(String[] ls){int n=0;for(String x:ls)if(x.replace(" ","").matches("[A-Z0-9<]{20,}"))n++;return n>=2;}
 private String preview(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();if(b==null)return"";int m=1200;if(Math.max(b.getWidth(),b.getHeight())>m){float f=m/(float)Math.max(b.getWidth(),b.getHeight());b=Bitmap.createScaledBitmap(b,Math.round(b.getWidth()*f),Math.round(b.getHeight()*f),true);}ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,72,o);return Base64.encodeToString(o.toByteArray(),Base64.NO_WRAP);}
 private void err(){Toast.makeText(this,"No se pudo leer la imagen. Haz otra foto con más nitidez.",Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
