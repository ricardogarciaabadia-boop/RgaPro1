from pathlib import Path
import re

# Add robust extraction for the uploaded Ocaso Vida Individual example.
p=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s=p.read_text(encoding='utf-8')
marker='    private JSONObject parse(String raw)throws Exception{'
if marker in s and 'parseVidaMauricioV2' not in s:
    helper=r'''    private JSONObject parseVidaMauricioV2(String raw)throws Exception{
        String t=raw==null?"":raw;
        JSONObject o=new JSONObject();
        o.put("policyType","Vida");
        o.put("policyNumber",first(t,"(?i)N[º°O]?\\s*(?:DE\\s*)?P[ÓO]LIZA\\s*[:.]?\\s*([0-9]{5,12})"));
        o.put("holder",findAfter(t,"TOMADOR\\s+DEL\\s+SEGURO\\s+Y\\s+DOMICILIO|TOMADOR\\s+DEL\\s+SEGURO"));
        o.put("documentNumber",first(t,"(?i)DOC\\.?\\s*ID\\.?\\s*[:.]?\\s*([0-9XYZ][0-9A-Z-]{7,12})"));
        o.put("effectiveDate",first(t,"(?i)EFECTO(?:\\s+DE\\s+LA\\s+P[ÓO]LIZA)?(?:\\s+DESDE[^\\d]*)?\\s*(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})"));
        o.put("policyExpiry",first(t,"(?i)VENCIMIENTO[^\\d]{0,35}(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})"));
        o.put("paymentFrequency",first(t,"(?i)FORMA\\s+DE\\s+PAGO\\s+([A-ZÁÉÍÓÚ]+)"));
        o.put("premiums",first(t,"(?i)PR\\.?\\s*NETA\\s+ANUAL[^\\d]*([0-9]+(?:[.,][0-9]{2}))"));
        o.put("receiptAmount",first(t,"(?i)TOT\\.?\\s*RECIBO\\s*([0-9]+(?:[.,][0-9]{2}))"));
        o.put("insuredName",findAfter(t,"NOMBRE"));
        o.put("insuredBirthDate",first(t,"(?i)FECHA\\s+NACIMIENTO\\s*[:.]?\\s*(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})"));
        JSONArray caps=new JSONArray();
        Matcher m=Pattern.compile("(?im)^\\s*CAPITAL\\s+POR\\s+FALLECIMIENTO.*?([0-9]{1,3}(?:[.][0-9]{3})*,[0-9]{2}|[0-9]+(?:[.,][0-9]{2}))\\s*$").matcher(t);
        if(m.find()){JSONObject r=new JSONObject();r.put("coverage","Capital por fallecimiento");r.put("value",m.group(1));caps.put(r);}
        Matcher c=Pattern.compile("(?im)^\\s*1[- ]INVALIDEZ\\s+ABSOLUTA\\s+Y\\s+PERMANENTE.*?([0-9]{1,3}(?:[.][0-9]{3})*,[0-9]{2}|[0-9]+(?:[.,][0-9]{2}))\\s*$").matcher(t);
        if(c.find()){JSONObject r=new JSONObject();r.put("coverage","Invalidez absoluta y permanente");r.put("value",c.group(1));caps.put(r);}
        Matcher cm=Pattern.compile("(?im)^\\s*C[AÁ]NCER\\s+MASCULINO.*?([0-9]{1,3}(?:[.][0-9]{3})*,[0-9]{2}|[0-9]+(?:[.,][0-9]{2}))\\s*$").matcher(t);
        if(cm.find()){JSONObject r=new JSONObject();r.put("coverage","Cáncer masculino");r.put("value",cm.group(1));caps.put(r);}
        o.put("capitales",caps);
        return o;
    }

'''
    s=s.replace(marker,helper+marker,1)
    old='    private JSONObject parse(String raw)throws Exception{JSONObject o=PolicyOcrParser.parse(raw);String type=findPolicyTypeV2(raw);if(!"Póliza".equals(type)||raw.toUpperCase(Locale.ROOT).contains("TOMADOR")||raw.toUpperCase(Locale.ROOT).contains("CONDICIONES PARTICULARES")){JSONObject p=parsePolicyExamplesV2(raw);for(java.util.Iterator<String> it=p.keys();it.hasNext();){String k=it.next();o.put(k,p.get(k));}}return o;}'
    new='    private JSONObject parse(String raw)throws Exception{JSONObject o=PolicyOcrParser.parse(raw);String up=raw==null?"":raw.toUpperCase(Locale.ROOT);if(up.contains("PÓLIZA DE SEGURO DE VIDA INDIVIDUAL")||up.contains("OCASO VIDA")){JSONObject p=parseVidaMauricioV2(raw);for(java.util.Iterator<String> it=p.keys();it.hasNext();){String k=it.next();o.put(k,p.get(k));}return o;}String type=findPolicyTypeV2(raw);if(!"Póliza".equals(type)||up.contains("TOMADOR")||up.contains("CONDICIONES PARTICULARES")){JSONObject p=parsePolicyExamplesV2(raw);for(java.util.Iterator<String> it=p.keys();it.hasNext();){String k=it.next();o.put(k,p.get(k));}}return o;}'
    s=s.replace(old,new,1)
    p.write_text(s,encoding='utf-8')
    print('Vida Mauricio policy parser applied')
