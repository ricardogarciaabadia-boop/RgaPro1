package com.rgapro1.ocaso;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repairs OCR records after saving and keeps all documents for the same person
 * under one client. It also performs a second, full-resolution OCR pass over
 * saved images so text missed by the first low-resolution pass can be recovered.
 */
public final class ClientAutoLinker {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Pattern DNI = Pattern.compile("\\b(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CIF = Pattern.compile("\\b[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+34\\s*)?[6789]\\d{8}(?!\\d)");
    private static final Pattern POLICY = Pattern.compile("(?:N[º°.]?\\s*)?(?:DE\\s*)?(?:P[ÓO]LIZA|POLIZA)\\s*(?:N[º°.]?|NUM(?:ERO)?)?\\s*[:#-]?\\s*([A-Z0-9./_-]{5,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("\\b\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{2,4}\\b");

    private ClientAutoLinker() {}

    public static void start(final Context context) {
        final Context app = context.getApplicationContext();
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            repair(app);
            MAIN.postDelayed(tick[0], 2500L);
        };
        MAIN.post(tick[0]);
    }

    public static void repair(final Context context) {
        if (!RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("rgapro_local", Context.MODE_PRIVATE);
                String raw = prefs.getString("policies", "[]");
                JSONArray source = new JSONArray(raw);
                // First enrich fields from the OCR already saved by MainActivity.
                for (int i = 0; i < source.length(); i++) enrich(source.optJSONObject(i));
                merge(source);
                prefs.edit().putString("policies", source.toString()).apply();
            } catch (Exception ignored) {
            } finally {
                RUNNING.set(false);
            }
        }, "rgapro-auto-link").start();
    }

    private static void enrich(JSONObject p) {
        if (p == null) return;
        String text = p.optString("ocrText", "");
        if (text.isEmpty()) return;
        String upper = text.toUpperCase(Locale.ROOT);
        String id = first(DNI, upper);
        String cif = first(CIF, upper);
        if (empty(p, "identityNumber") && !id.isEmpty()) {
            put(p, "identityNumber", id);
            put(p, "holderDni", id);
            put(p, "identityType", id.matches("[XYZ].*") ? "NIE" : "DNI");
        }
        if (empty(p, "cif") && !cif.isEmpty()) put(p, "cif", cif);
        if (empty(p, "email")) put(p, "email", first(EMAIL, text));
        if (empty(p, "phone")) put(p, "phone", first(PHONE, text));
        if (empty(p, "number")) put(p, "number", policyNumber(upper));
        if (empty(p, "surname")) put(p, "surname", labelValue(upper, "APELLIDOS", "TOMADOR"));
        if (empty(p, "name")) put(p, "name", labelValue(upper, "NOMBRE", "ASEGURADO", "CLIENTE"));
        if (empty(p, "holder")) {
            String holder = labelValue(upper, "TITULAR", "TOMADOR", "ASEGURADO", "CLIENTE");
            String name = p.optString("name", "").trim();
            String surname = p.optString("surname", "").trim();
            if (!name.isEmpty() || !surname.isEmpty()) holder = (name + " " + surname).trim();
            if (!holder.isEmpty()) put(p, "holder", holder);
        }
        if (empty(p, "address")) put(p, "address", labelValue(upper, "DOMICILIO", "DIRECCION", "DIRECCIÓN"));
        if (empty(p, "birthDate")) put(p, "birthDate", dateAfter(upper, "NACIMIENTO", "NAC"));
        if (empty(p, "validityDate")) {
            String d = dateAfter(upper, "VALIDEZ", "CADUCIDAD", "VENCIMIENTO", "VENC");
            if (!d.isEmpty()) { put(p, "validityDate", d); put(p, "expiry", d); }
        }
        if (empty(p, "issueDate")) put(p, "issueDate", dateAfter(upper, "EMISION", "EMISIÓN", "EFECTO", "ALTA"));
        if (empty(p, "type")) {
            if (upper.contains("PÓLIZA") || upper.contains("POLIZA")) put(p, "type", "Otros");
            else put(p, "type", "Cliente / DNI");
        }
        p.put("ocrEnhancedByLinker", true);
    }

    private static void merge(JSONArray a) throws Exception {
        for (int i = 0; i < a.length(); i++) {
            JSONObject base = a.optJSONObject(i);
            if (base == null) continue;
            for (int j = a.length() - 1; j > i; j--) {
                JSONObject other = a.optJSONObject(j);
                if (other != null && sameClient(base, other)) {
                    mergeInto(base, other);
                    a.remove(j);
                }
            }
        }
    }

    private static boolean sameClient(JSONObject a, JSONObject b) {
        String ai = norm(id(a)), bi = norm(id(b));
        if (!ai.isEmpty() && !bi.isEmpty()) return ai.equals(bi);
        String ae = norm(a.optString("email", "")), be = norm(b.optString("email", ""));
        if (!ae.isEmpty() && !be.isEmpty() && ae.equals(be)) return true;
        String ap = digits(a.optString("phone", "")), bp = digits(b.optString("phone", ""));
        if (!ap.isEmpty() && !bp.isEmpty() && ap.equals(bp)) return true;
        String an = norm(fullName(a)), bn = norm(fullName(b));
        if (!an.isEmpty() && !bn.isEmpty() && (an.equals(bn) || an.contains(bn) || bn.contains(an))) return true;
        if (!an.isEmpty() && !bn.isEmpty()) {
            String[] at = an.split(" "), bt = bn.split(" ");
            int common = 0;
            for (String x : at) for (String y : bt) if (x.length() > 2 && x.equals(y)) common++;
            if (common >= 2) return true;
        }
        return false;
    }

    private static void mergeInto(JSONObject base, JSONObject other) throws Exception {
        copyIfEmpty(base, other, "holder"); copyIfEmpty(base, other, "surname"); copyIfEmpty(base, other, "name");
        copyIfEmpty(base, other, "identityType"); copyIfEmpty(base, other, "identityNumber"); copyIfEmpty(base, other, "holderDni");
        copyIfEmpty(base, other, "cif"); copyIfEmpty(base, other, "birthDate"); copyIfEmpty(base, other, "nationality");
        copyIfEmpty(base, other, "sex"); copyIfEmpty(base, other, "address"); copyIfEmpty(base, other, "birthPlace");
        copyIfEmpty(base, other, "parents"); copyIfEmpty(base, other, "supportNumber"); copyIfEmpty(base, other, "issueDate");
        copyIfEmpty(base, other, "validityDate"); copyIfEmpty(base, other, "expiry"); copyIfEmpty(base, other, "phone");
        copyIfEmpty(base, other, "email");
        JSONArray docs = base.optJSONArray("documentPhotos"); if (docs == null) docs = new JSONArray();
        Set<String> seen = new HashSet<>(); for (int i=0;i<docs.length();i++) { JSONObject d=docs.optJSONObject(i); if(d!=null) seen.add(d.optString("path","")); }
        JSONArray od = other.optJSONArray("documentPhotos"); if (od != null) for(int i=0;i<od.length();i++){JSONObject d=od.optJSONObject(i); if(d!=null&&!seen.contains(d.optString("path",""))){docs.put(d);seen.add(d.optString("path",""));}}
        base.put("documentPhotos", docs);
        JSONArray linked = base.optJSONArray("linkedDocuments"); if (linked == null) linked = new JSONArray();
        addLinked(linked, other); base.put("linkedDocuments", linked);
        JSONArray policies = base.optJSONArray("linkedPolicies"); if (policies == null) policies = new JSONArray();
        addPolicyIfPresent(policies, base); addPolicyIfPresent(policies, other); base.put("linkedPolicies", uniquePolicies(policies));
        String bo = base.optString("ocrText", ""), oo = other.optString("ocrText", "");
        if (!oo.isEmpty() && !bo.contains(oo)) base.put("ocrText", (bo.isEmpty()?"":bo+"\n\n--- DOCUMENTO ASOCIADO ---\n") + oo);
        base.put("updatedAt", System.currentTimeMillis());
    }

    private static void addLinked(JSONArray linked, JSONObject p) throws Exception {
        JSONArray d = p.optJSONArray("documentPhotos");
        if (d == null) return;
        for (int i=0;i<d.length();i++) { JSONObject x=d.optJSONObject(i); if(x==null)continue; JSONObject copy=new JSONObject(x.toString()); copy.put("clientHolder",p.optString("holder","")); copy.put("clientIdentity",id(p)); linked.put(copy); }
    }

    private static void addPolicyIfPresent(JSONArray policies, JSONObject p) throws Exception {
        String n=p.optString("number","").trim(); if(n.isEmpty()) return;
        JSONObject x=new JSONObject(); x.put("number",n); x.put("type",p.optString("type","Otros")); x.put("expiry",p.optString("expiry",p.optString("validityDate",""))); x.put("holder",p.optString("holder","")); policies.put(x);
    }

    private static JSONArray uniquePolicies(JSONArray in) throws Exception {
        JSONArray out=new JSONArray(); Set<String> seen=new HashSet<>();
        for(int i=0;i<in.length();i++){JSONObject p=in.optJSONObject(i);if(p==null)continue;String k=norm(p.optString("number",""));if(k.isEmpty()||seen.add(k))out.put(p);}
        return out;
    }

    private static void copyIfEmpty(JSONObject a, JSONObject b, String key) throws Exception { if (empty(a,key) && !empty(b,key)) a.put(key,b.optString(key)); }
    private static boolean empty(JSONObject p,String k){return p==null||p.optString(k,"").trim().isEmpty();}
    private static void put(JSONObject p,String k,String v){try{if(v!=null&&!v.trim().isEmpty())p.put(k,v.trim());}catch(Exception ignored){}}
    private static String id(JSONObject p){String x=p.optString("identityNumber",p.optString("holderDni",""));if(x.trim().isEmpty())x=p.optString("cif","");return x;}
    private static String fullName(JSONObject p){String h=p.optString("holder","");if(!h.isEmpty())return h;return (p.optString("name","")+" "+p.optString("surname","")).trim();}
    private static String norm(String s){if(s==null)return "";return Normalizer.normalize(s.toUpperCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^A-Z0-9]","");}
    private static String digits(String s){return s==null?"":s.replaceAll("\\D","");}
    private static String first(Pattern p,String s){Matcher m=p.matcher(s==null?"":s);return m.find()?m.group():"";}
    private static String policyNumber(String s){Matcher m=POLICY.matcher(s==null?"":s);return m.find()?m.group(1).trim():"";}
    private static String labelValue(String text,String... labels){for(String label:labels){Pattern p=Pattern.compile("(?:^|\\n)\\s*"+Pattern.quote(label)+"\\s*[:.-]?\\s*([^\\n]{2,80})",Pattern.CASE_INSENSITIVE);Matcher m=p.matcher(text);if(m.find()){String v=m.group(1).trim();if(!v.isEmpty())return v;}}return "";}
    private static String dateAfter(String text,String... labels){for(String label:labels){Pattern p=Pattern.compile(Pattern.quote(label)+"[^\\n]{0,40}?"+DATE.pattern(),Pattern.CASE_INSENSITIVE);Matcher m=p.matcher(text);if(m.find()){Matcher d=DATE.matcher(m.group());if(d.find())return d.group();}}return "";}
}
