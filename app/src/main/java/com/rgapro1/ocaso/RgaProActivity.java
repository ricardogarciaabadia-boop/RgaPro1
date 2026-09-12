package com.rgapro1.ocaso;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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
import androidx.exifinterface.media.ExifInterface;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RgaProActivity extends Activity {

    private static final int PICK = 8101, CAMERA = 8102;

    private WebView web;
    private TextRecognizer recognizer;
    private File cameraFile;
    private String cameraSide = "document";
    private String frontRaw = "", reverseRaw = "";
    private ValueCallback<Uri[]> pendingFileCallback;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web, new ViewGroup.LayoutParams(-1, -1));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) { applyCleanUi(v); }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
                pendingFileCallback = cb;
                pickDocument();
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "RgaProCamera");
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        web.loadUrl("file:///android_asset/prototype/index.html");
    }

    private void applyCleanUi(WebView v) {
        String js = "(function(){"
                + "var p=document.getElementById('n-policies');if(p)p.remove();"
                + "document.querySelectorAll('.nav button').forEach(function(b){if((b.textContent||'').trim().toLowerCase()==='pólizas')b.remove();});"
                + "var g=document.querySelector('#home .grid');if(g)g.style.display='none';"
                + "['ocrTypeDni','ocrTypeDoc','ocrTypePol'].forEach(function(id){var x=document.getElementById(id);if(x)x.style.display='none';});"
                + "var o=document.querySelector('#ocr .ocrbox');if(o){o.querySelectorAll('button').forEach(function(b){if((b.getAttribute('onclick')||'').indexOf('takeFront')<0)b.style.display='none';});"
                + "var a=o.querySelector('button[onclick=\"takeFront()\"]');if(a){a.textContent='📷  Añadir documento';a.setAttribute('onclick','openOcrInput()');a.className='primary full';}}"
                + "var e=document.querySelector('#ocr .eyebrow');if(e)e.style.display='none';"
                + "var st=document.getElementById('step');if(st)st.style.display='none';"
                + "var expiry=document.querySelectorAll('.field');expiry.forEach(function(f){var t=(f.textContent||'').toUpperCase();if(t.indexOf('FECHA DE CADUCIDAD')>=0||t.indexOf('VALIDACIÓN MRZ')>=0||t.indexOf('VALIDACION MRZ')>=0)f.style.display='none';});"
                + "var style=document.getElementById('rgaPreviewStyle');if(!style){style=document.createElement('style');style.id='rgaPreviewStyle';style.textContent='#rgaPreviewFaces{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:10px}#rgaPreviewFaces .rgaFace{background:#071b3a;border-radius:12px;overflow:hidden}#rgaPreviewFaces img{width:100%;height:190px;object-fit:contain;display:block}#rgaPreviewFaces .rgaLabel{background:#071b3a;color:#fff;text-align:center;padding:6px;font-weight:800;font-size:12px}#rgaPreviewFaces .rgaWait{height:190px;display:flex;align-items:center;justify-content:center;color:#dce8f7;text-align:center;padding:10px;font-size:12px}';document.head.appendChild(style);}"
                + "window.rgaRenderPreview=function(side,preview){if(!preview)return;var box=document.querySelector('#ocr .ocrbox');if(!box)return;var old=document.getElementById('docimg');if(old)old.style.display='none';var wrap=document.getElementById('rgaPreviewFaces');if(!wrap){wrap=document.createElement('div');wrap.id='rgaPreviewFaces';if(old)old.parentNode.insertBefore(wrap,old);else box.insertBefore(wrap,box.firstChild);}"
                + "var s=(side||'document').toLowerCase();if(s==='front'||s==='reverse'){var id=s==='front'?'rgaFrontFace':'rgaReverseFace';var label=s==='front'?'ANVERSO':'REVERSO';var face=document.getElementById(id);if(!face){face=document.createElement('div');face.id=id;face.className='rgaFace';face.innerHTML='<img><div class=\"rgaLabel\"></div>';wrap.appendChild(face);}face.querySelector('img').src='data:image/jpeg;base64,'+preview;face.querySelector('.rgaLabel').textContent=label;if(!document.getElementById('rgaFrontFace')){var ph=document.createElement('div');ph.id='rgaReverseFace';ph.className='rgaFace';ph.innerHTML='<div class=\"rgaWait\">Esperando reverso del DNI/NIE</div><div class=\"rgaLabel\">REVERSO</div>';wrap.appendChild(ph);}}else{wrap.innerHTML='<div class=\"rgaFace\"><img src=\"data:image/jpeg;base64,'+preview+'\"><div class=\"rgaLabel\">DOCUMENTO</div></div>';}}"
                + "window.openOcrInput=function(){if(window.RgaProCamera)RgaProCamera.chooseInput();};"
                + "})();";
        v.evaluateJavascript(js, null);
    }

    private class Bridge {
        @JavascriptInterface public void capture(String side) { runOnUiThread(() -> startCamera(side)); }
        @JavascriptInterface public void pickPdf() { runOnUiThread(RgaProActivity.this::pickDocument); }
        @JavascriptInterface public void chooseInput() { runOnUiThread(RgaProActivity.this::showInputChooser); }
    }

    private void showInputChooser() {
        new AlertDialog.Builder(this).setTitle("Añadir documento")
                .setItems(new String[]{"📷 Cámara", "📁 Archivos / PDF"}, (d, w) -> {
                    if (w == 0) startCamera("front"); else pickDocument();
                }).show();
    }

    private void pickDocument() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK);
    }

    private void startCamera(String side) {
        cameraSide = side == null ? "document" : side;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA);
            return;
        }
        try {
            File dir = new File(getCacheDir(), "rgapro_scan");
            if (!dir.exists()) dir.mkdirs();
            cameraFile = File.createTempFile("rgapro_", ".jpg", dir);
            Uri out = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cameraFile);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, out);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, CAMERA);
        } catch (Exception e) { showError("No se pudo abrir la cámara"); }
    }

    @Override protected void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraSide);
        }
    }

    @Override protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == CAMERA) {
            if (res == RESULT_OK && cameraFile != null) scanCamera(cameraFile, cameraSide);
            return;
        }
        if (r != PICK) return;
        if (res != RESULT_OK || d == null) {
            if (pendingFileCallback != null) { pendingFileCallback.onReceiveValue(null); pendingFileCallback = null; }
            return;
        }
        Uri[] values = null;
        if (d.getClipData() != null) {
            int n = d.getClipData().getItemCount(); values = new Uri[n];
            for (int i = 0; i < n; i++) values[i] = d.getClipData().getItemAt(i).getUri();
        } else if (d.getData() != null) values = new Uri[]{d.getData()};
        if (values == null || values.length == 0) return;
        ValueCallback<Uri[]> cb = pendingFileCallback; pendingFileCallback = null;
        if (cb != null) cb.onReceiveValue(values);
        try { for (Uri u : values) { takePersistablePermission(u); scanUri(u, "document"); } }
        catch (Exception e) { showError("No se pudo abrir el documento seleccionado"); }
    }

    private void takePersistablePermission(Uri u) {
        try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { }
    }

    private void scanCamera(File f, String side) {
        try {
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b == null) { showError("La foto no es válida"); return; }
            b = rotateFromExif(b, f.getAbsolutePath());
            runBitmapOcr(b, side, encodePreview(b));
        } catch (Exception e) { showError("No se pudo procesar la foto"); }
    }

    private Bitmap rotateFromExif(Bitmap b, String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degrees = o == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : o == ExifInterface.ORIENTATION_ROTATE_180 ? 180 : o == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            if (degrees == 0) return b;
            Matrix m = new Matrix(); m.postRotate(degrees);
            Bitmap out = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (out != b) b.recycle();
            return out;
        } catch (Exception ignored) { return b; }
    }

    private void scanUri(Uri u, String side) {
        try {
            String type = getContentResolver().getType(u);
            String low = String.valueOf(u).toLowerCase(Locale.ROOT);
            boolean pdf = "application/pdf".equalsIgnoreCase(type) || "application/x-pdf".equalsIgnoreCase(type) || low.contains(".pdf");
            if (pdf) {
                String preview = encodePdfPreview(u);
                deliverPreview(preview, side, "PDF");
                PdfOcrHelper.process(this, u, new PdfOcrHelper.Callback() {
                    public void onSuccess(String text) { deliver(parse(text), side, preview); }
                    public void onError(Exception e) { deliver(parse(""), side, preview); showError("No se pudo leer el texto del PDF"); }
                });
                return;
            }
            try (InputStream in = getContentResolver().openInputStream(u)) {
                if (in == null) throw new IOException("open");
                Bitmap b = BitmapFactory.decodeStream(in);
                if (b == null) throw new IOException("bitmap");
                runBitmapOcr(b, side, encodePreview(b));
            }
        } catch (Exception e) { showError("No se pudo abrir el documento"); }
    }

    private void runBitmapOcr(Bitmap b, String side, String preview) {
        try {
            deliverPreview(preview, side, "IMAGEN");
            recognizer.process(InputImage.fromBitmap(b, 0)).addOnSuccessListener(x -> {
                deliver(parse(x == null ? "" : x.getText()), side, preview);
                if (!b.isRecycled()) b.recycle();
                if ("front".equals(side)) {
                    String text = x == null ? "" : x.getText();
                    if ("DNI/NIE".equals(classify(text))) askForReverse();
                }
            }).addOnFailureListener(x -> {
                if (!b.isRecycled()) b.recycle();
                showError("No se pudo leer el documento");
            });
        } catch (Exception e) {
            if (!b.isRecycled()) b.recycle();
            showError("No se pudo iniciar el OCR");
        }
    }

    private void askForReverse() {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("DNI/NIE detectado")
                .setMessage("Anverso capturado. Ahora toma una foto del reverso para completar el documento.")
                .setPositiveButton("📷 Capturar reverso", (d, w) -> startCamera("reverse"))
                .setNegativeButton("Ahora no", null)
                .show());
    }

    private String encodePreview(Bitmap b) throws Exception {
        if (b == null) return "";
        Bitmap src = b; int max = 1400;
        if (b.getWidth() > max) {
            int h = Math.max(1, (int)((double)b.getHeight() * max / b.getWidth()));
            src = Bitmap.createScaledBitmap(b, max, h, true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 82, out);
        if (src != b && !src.isRecycled()) src.recycle();
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    private String encodePdfPreview(Uri u) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(u, "r")) {
            if (pfd == null) return "";
            try (android.graphics.pdf.PdfRenderer r = new android.graphics.pdf.PdfRenderer(pfd)) {
                if (r.getPageCount() == 0) return "";
                try (android.graphics.pdf.PdfRenderer.Page p = r.openPage(0)) {
                    int w = Math.min(1400, Math.max(900, p.getWidth() * 2));
                    int h = Math.max(1, (int)((double)p.getHeight() * w / p.getWidth()));
                    Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    p.render(b, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    String out = encodePreview(b); b.recycle(); return out;
                }
            }
        } catch (Exception e) { return ""; }
    }

    private void deliverPreview(String preview, String side, String kind) {
        if (preview == null || preview.isEmpty()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("side", side == null ? "document" : side);
            o.put("preview", preview);
            o.put("previewOnly", true);
            o.put("kind", kind == null ? "DOCUMENTO" : kind);
            String json = JSONObject.quote(o.toString());
            String js = "if(window.setOcrResult)window.setOcrResult(" + json + ");if(window.rgaRenderPreview)window.rgaRenderPreview(" + JSONObject.quote(side == null ? "document" : side) + "," + JSONObject.quote(preview) + ");";
            runOnUiThread(() -> { if (web != null && !isFinishing()) web.evaluateJavascript(js, null); });
        } catch (Exception ignored) { }
    }

    private JSONObject parse(String raw) {
        try {
            DniOcrParser.Result r = DniOcrParser.parse(raw);
            JSONObject o = new JSONObject();
            o.put("documentNumber", r.dni);
            o.put("birthDate", formatDate(r.birthDate));
            o.put("name", r.name);
            o.put("surname", r.surname);
            o.put("raw", raw == null ? "" : raw);
            o.put("policyNumber", findPolicyNumber(raw));
            o.put("policyType", findPolicyType(raw));
            o.put("classification", classify(raw));
            o.put("phone", find(raw, "(?:\\+34\\s*)?[6789]\\d{8}"));
            o.put("email", find(raw, "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));
            o.put("address", findLabeled(raw, "DIRECCIÓN", "DIRECCION", "DOMICILIO", "RIESGO"));
            return o;
        } catch (Exception e) { return new JSONObject(); }
    }

    private String formatDate(String value) {
        if (value == null) return "";
        String v = value.trim().replaceAll("[^0-9]", "");
        if (v.length() == 8) return v.substring(0,2) + "/" + v.substring(2,4) + "/" + v.substring(4);
        return value;
    }

    private void deliver(JSONObject o, String side, String preview) {
        try {
            o.put("side", side == null ? "document" : side);
            o.put("preview", preview == null ? "" : preview);
            o.put("previewOnly", false);
            if ("front".equals(side)) frontRaw = o.optString("raw", "");
            if ("reverse".equals(side)) reverseRaw = o.optString("raw", "");
            o.put("frontRead", !frontRaw.isEmpty());
            o.put("reverseRead", !reverseRaw.isEmpty());
            String json = JSONObject.quote(o.toString());
            String js = "window.setOcrResult(" + json + ");if(window.rgaRenderPreview)window.rgaRenderPreview(" + JSONObject.quote(side == null ? "document" : side) + "," + JSONObject.quote(preview == null ? "" : preview) + ");";
            runOnUiThread(() -> { if (web != null && !isFinishing()) web.evaluateJavascript(js, null); });
        } catch (Exception e) { showError("No se pudo mostrar el resultado OCR"); }
    }

    private String classify(String raw) {
        String u = (raw == null ? "" : raw).toUpperCase(Locale.ROOT);
        boolean dni = u.contains("DNI") || u.contains("NIE") || u.contains("IDESP") || u.matches("(?s).*\\b[XYZ]?[0-9]{7}[A-Z]\\b.*");
        boolean pol = u.contains("PÓLIZA") || u.contains("POLIZA") || u.contains("TOMADOR") || u.contains("ASEGURADO") || u.contains("FECHA DE EFECTO") || u.contains("CONDICIONES PARTICULARES");
        return dni ? "DNI/NIE" : pol ? "Póliza" : "Documento";
    }

    private String findPolicyNumber(String raw) {
        if (raw == null) return "";
        Matcher m = Pattern.compile("(?i)(?:N[º°O]\\s*)?(?:NÚMERO DE P[ÓO]LIZA|NUMERO DE POLIZA|P[ÓO]LIZA|POLIZA)\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})").matcher(raw);
        return m.find() ? m.group(1).trim() : "";
    }

    private String findPolicyType(String raw) {
        String u = (raw == null ? "" : raw).toUpperCase(Locale.ROOT);
        if (u.contains("DECESOS")) return "Decesos";
        if (u.contains("COMUNIDAD")) return "Comunidades";
        if (u.contains("HOGAR")) return "Hogar";
        if (u.contains("AUTO") || u.contains("AUTOMOVIL") || u.contains("AUTOMÓVIL")) return "Auto";
        if (u.contains("VIDA")) return "Vida";
        return "Póliza";
    }

    private String findLabeled(String raw, String... labels) {
        if (raw == null) return "";
        for (String line : raw.split("\\R")) {
            String u = line.toUpperCase(Locale.ROOT);
            for (String label : labels) {
                int p = u.indexOf(label);
                if (p >= 0) {
                    String v = line.substring(Math.min(line.length(), p + label.length())).replaceFirst("^[\\s:.-]+", "").trim();
                    if (!v.isEmpty()) return v;
                }
            }
        }
        return "";
    }

    private String find(String raw, String regex) {
        if (raw == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(raw);
        return m.find() ? m.group() : "";
    }

    private void showError(String m) {
        runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
    }

    @Override protected void onDestroy() {
        if (pendingFileCallback != null) { pendingFileCallback.onReceiveValue(null); pendingFileCallback = null; }
        if (recognizer != null) recognizer.close();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
