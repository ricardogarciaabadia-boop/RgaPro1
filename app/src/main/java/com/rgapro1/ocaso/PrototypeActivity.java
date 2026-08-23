package com.rgapro1.ocaso;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrototypeActivity extends Activity {
 private static final int PICK=7001, CAMERA=7002;
 private WebView web; private ValueCallback<Uri[]> cb; private TextRecognizer recognizer;
 private File cameraFile; private String cameraSide="front"; private String frontRaw=""; private String reverseRaw="";

 @Override public void onCreate(android.os.Bundle b){
  super.onCreate(b);
  web=new WebView(this); setContentView(web,new ViewGroup.LayoutParams(-1,-1));
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true);
  web.setWebViewClient(new WebViewClient());
  web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> c,FileChooserParams p){if(cb!=null)cb.onReceiveValue(null);cb=c;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK);return true;}});
  web.addJavascriptInterface(new CameraBridge(),"RgaProCamera");
  recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  web.loadUrl("file:///android_asset/prototype/index.html");
 }

 private class CameraBridge{
  @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}
 }

 private void startCamera(String side){
  cameraSide="reverse".equals(side)?"reverse":"front";
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA);return;}
  try{
   File dir=new File(getCacheDir(),"dni");if(!dir.exists())dir.mkdirs();
   cameraFile=File.createTempFile("rgapro_dni_", ".jpg", dir);
   Uri out=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraFile);
   Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,out);i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,CAMERA);
  }catch(Exception e){Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_LONG).show();}
 }

 @Override protected void onActivityResult(int r,int res,Intent d){
  super.onActivityResult(r,res,d);
  if(r==CAMERA){if(res==RESULT_OK&&cameraFile!=null)scanCamera(cameraFile,cameraSide);return;}
  if(r!=PICK)return;
  Uri u=res==RESULT_OK&&d!=null?d.getData():null;
  if(cb!=null){cb.onReceiveValue(u==null?null:new Uri[]{u});cb=null;}
  if(u!=null)scan(u,"gallery");
 }

 private void scanCamera(File file,String side){
  try{
   InputImage img=InputImage.fromFilePath(this,Uri.fromFile(file));
   recognizer.process(img).addOnSuccessListener(t->{try{String raw=t.getText()==null?"":t.getText();if("reverse".equals(side))reverseRaw=raw;else frontRaw=raw;String combined=(frontRaw+"\n"+reverseRaw).trim();JSONObject o=parse(combined);o.put("side",side);o.put("preview",previewFile(file));o.put("frontRead",!frontRaw.isEmpty());o.put("reverseRead",!reverseRaw.isEmpty());web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"reverse".equals(side)?"Reverso leído: revisa los datos y MRZ":"Anverso leído: ahora toma el REVERSO del DNI/NIE",Toast.LENGTH_LONG).show();}catch(Exception e){err();}}).addOnFailureListener(e->err());
  }catch(Exception e){err();}
 }

 private void scan(Uri u,String side){try{InputImage img=InputImage.fromFilePath(this,u);recognizer.process(img).addOnSuccessListener(t->{try{JSONObject o=parse(t);o.put("side",side);o.put("preview",preview(u));web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"OCR terminado: revisa los campos antes de guardar",Toast.LENGTH_LONG).show();}catch(Exception e){err();}}).addOnFailureListener(e->err());}catch(Exception e){err();}}

 private JSONObject parse(Text t)throws Exception{return parse(t.getText()==null?"":t.getText());}
 private JSONObject parse(String raw)throws Exception{
  String norm=raw.toUpperCase(Locale.ROOT).replace("APELLlDOS","APELLIDOS").replace("N0MBRE","NOMBRE");
  String doc=find(norm,"(?<![0-9])(?:[0-9]\\s*){8}[A-Z](?![A-Z0-9])");
  doc=doc.replaceAll("\\s","");
  if(!isValidDni(doc)){String x=find(norm,"\\b[XYZ][0-9]{7}[A-Z]\\b");if(!x.isEmpty())doc=x;}
  String date=find(norm,"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");
  String[] lines=raw.split("\\R");String name=findName(lines);String exp=findExpiry(lines);boolean mrz=mrz(lines);
  String surname=findSurname(lines);
  if(mrz){Matcher m=Pattern.compile("([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)+)<<([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)").matcher(norm);if(m.find()){surname=m.group(1).replace('<',' ').replaceAll("\\s+"," ").trim();name=m.group(2).replace('<',' ').replaceAll("\\s+"," ").trim();}}
  JSONObject o=new JSONObject(),c=new JSONObject(),v=new JSONObject();o.put("documentNumber",doc.isEmpty()?"No detectado":doc);o.put("birthDate",date.isEmpty()?"No detectada":date);o.put("expiryDate",exp.isEmpty()?"No detectada":exp);o.put("name",name.isEmpty()?"No detectado":(name+((surname.isEmpty())?"":" "+surname)));o.put("surname",surname.isEmpty()?"No detectados":surname);o.put("mrzStatus",mrz?"Detectada: revisar checksum":"No confirmada");c.put("Nº documento",doc.isEmpty()?0:98);c.put("Nombre y apellidos",name.isEmpty()?0:92);c.put("Fecha de nacimiento",date.isEmpty()?0:96);c.put("Fecha de caducidad",exp.isEmpty()?0:96);c.put("Validación MRZ",mrz?85:0);v.put("Nº documento",!doc.isEmpty());v.put("Nombre y apellidos",!name.isEmpty());v.put("Fecha de nacimiento",!date.isEmpty());v.put("Fecha de caducidad",!exp.isEmpty());v.put("Validación MRZ",false);o.put("confidence",c);o.put("verified",v);o.put("raw",raw);return o;
 }
 private String find(String s,String r){Matcher m=Pattern.compile(r).matcher(s);return m.find()?m.group():"";}
 private String findName(String[] ls){for(int i=0;i<ls.length;i++){String u=ls[i].trim().toUpperCase(Locale.ROOT);if(u.startsWith("NOMBRE")){String n=u.substring(6).replaceFirst("^[ :.-]+","").trim();if(!n.isEmpty())return n;if(i+1<ls.length){n=ls[i+1].trim();if(n.length()>2&&!n.matches(".*\\d.*"))return n;}}}return "";}
 private String findSurname(String[] ls){for(int i=0;i<ls.length;i++){String u=ls[i].trim().toUpperCase(Locale.ROOT);if(u.startsWith("APELLIDOS")||u.startsWith("APELLIDO")){StringBuilder b=new StringBuilder(u.substring(u.startsWith("APELLIDOS")?9:8).replaceFirst("^[ :.-]+","").trim());for(int j=i+1;j<Math.min(ls.length,i+4);j++){String x=ls[j].trim().toUpperCase(Locale.ROOT);if(x.isEmpty()||x.startsWith("NOMBRE")||x.startsWith("SEXO")||x.contains("NACIONALIDAD")||x.contains("NACIMIENTO")||x.contains("DOMICILIO")||x.contains("VALIDEZ")||x.contains("CADUCIDAD"))break;if(x.matches("[A-ZÁÉÍÓÚÑ]+(?:[ -][A-ZÁÉÍÓÚÑ]+)*")){if(b.length()>0)b.append(' ');b.append(x);}else break;}return b.toString().trim();}}return "";}
 private String findExpiry(String[] ls){for(int i=0;i<ls.length;i++){if(ls[i].toUpperCase(Locale.ROOT).contains("VALIDEZ")||ls[i].toUpperCase(Locale.ROOT).contains("CADUCIDAD")){String d=find(ls[i],"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");if(!d.isEmpty())return d;if(i+1<ls.length){d=find(ls[i+1],"\\b\\d{2}[ ./-]\\d{2}[ ./-]\\d{4}\\b");if(!d.isEmpty())return d;}}}return "";}
 private boolean mrz(String[] ls){int n=0;for(String x:ls)if(x.replace(" ","").matches("[A-Z0-9<]{20,}"))n++;return n>=2;}
 private boolean isValidDni(String value){if(value==null||!value.matches("\\d{8}[A-Z]"))return false;String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(value.substring(0,8))%23)==value.charAt(8);}catch(Exception e){return false;}}
 private String preview(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();return encodePreview(b);}
 private String previewFile(File f)throws Exception{return encodePreview(BitmapFactory.decodeFile(f.getAbsolutePath()));}
 private String encodePreview(Bitmap b)throws Exception{if(b==null)return"";int m=1200;if(Math.max(b.getWidth(),b.getHeight())>m){float f=m/(float)Math.max(b.getWidth(),b.getHeight());b=Bitmap.createScaledBitmap(b,Math.round(b.getWidth()*f),Math.round(b.getHeight()*f),true);}ByteArrayOutputStream o=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,72,o);return Base64.encodeToString(o.toByteArray(),Base64.NO_WRAP);}
 private void err(){Toast.makeText(this,"No se pudo leer la imagen. Haz otra foto con más nitidez.",Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){if(recognizer!=null)recognizer.close();if(web!=null)web.destroy();super.onDestroy();}
}
