from pathlib import Path
import re

# Product-aware extraction for the supplied real-world Ocaso policy examples.
# Keep the proven OCR engine intact; only improve field selection for policies.

p=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s=p.read_text(encoding='utf-8')

# Replace policy-specific parser helper if present or append before parse().
marker='    private JSONObject parse(String raw)throws Exception{'
if 'parsePolicyExamplesV2' not in s and marker in s:
    helper=r'''    private JSONObject parsePolicyExamplesV2(String raw)throws Exception{
        String text=raw==null?"":raw;
        JSONObject o=new JSONObject();
        o.put("policyNumber",first(text,
            "(?i)(?:N[º°O]?\\s*(?:DE\\s*)?P[ÓO]LIZA|N[ÚU]MERO\\s+DE\\s+P[ÓO]LIZA)\\s*[:.]?\\s*([A-Z0-9./_-]{4,})",
            "(?i)N[º°O]?\\s*P[ÓO]LIZA\\s*([0-9]{5,12})"));
        o.put("policyType",findPolicyTypeV2(text));
        String holder=findAfter(text,"TOMADOR\\s+DEL\\s+SEGURO|TOMADOR\\s+DEL\\s+SEGURO\\s+Y\\s+DOMICILIO|TOMADOR DEL SEGURO");
        if(holder.isEmpty()) holder=findAfter(text,"TOMADOR DEL SEGURO:");
        o.put("holder",cleanPolicyHolder(holder));
        o.put("documentNumber",first(text,"(?i)DOC\\.?\\s*ID\\.?\\s*[:.]?\\s*([0-9XYZ][0-9A-Z-]{7,12})"));
        o.put("effectiveDate",first(text,
            "(?i)FECHA\\s+DE\\s+EFECTO\\s*[:.]?\\s*(\\d{1,2}(?:[ /.-]\\d{1,2}[ /.-]\\d{4}|\\s+de?\\s+[A-Za-záéíóúñ]+\\s+de?\\s+\\d{4}))",
            "(?i)EFECTO(?:\\s+DE\\s+LA\\s+P[ÓO]LIZA)?(?:\\s+DESDE[^\\d]*)?(?:\\s*DESDE[^\\d]*)?(?:\\s+)?(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})"));
        o.put("policyExpiry",first(text,
            "(?i)VENCIMIENTO[^\\d]{0,30}(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})",
            "(?i)HASTA\\s+LAS?\\s+0?\\s*HORAS?\\s+DEL\\s+(?:D[IÍ]A)?\\s*(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})"));
        o.put("premiums",findPremium(text));
        o.put("paymentFrequency",first(text,"(?i)FORMA\\s+DE\\s+PAGO\\s*[:.]?\\s*([A-ZÁÉÍÓÚ]+)"));
        o.put("insureds",extractInsuredRowsV2(text));
        o.put("capitales",extractCapitalRowsV2(text,o.optJSONArray("insureds")==null?0:o.optJSONArray("insureds").length()));
        o.put("confidence",Math.min(99,policySignalsV2(text)*7));
        return o;
    }

    private String first(String raw,String...patterns){for(String q:patterns){Matcher m=Pattern.compile(q,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(raw);if(m.find()&&m.groupCount()>=1){String v=m.group(1).trim();if(!v.isEmpty())return normalizeDateText(v);}}return"";}
    private String findAfter(String raw,String labelRegex){Matcher m=Pattern.compile("(?is)"+labelRegex+"\\s*[:.-]?\\s*([^\\r\\n]{3,140})").matcher(raw);return m.find()?m.group(1).trim():"";}
    private String cleanPolicyHolder(String v){if(v==null)return"";String x=v.replaceAll("\\s+"," ").trim();x=x.replaceFirst("(?i)^(?:DOC\\.?\\s*ID\\.?|TEL[ÉE]FONO|EMAIL)\\s*[:.-]?.*$","");return x.trim();}
    private String normalizeDateText(String v){return v.replaceAll("\\s+"," ").trim().replace('-','/').replace('.','/');}
    private String findPolicyTypeV2(String raw){String u=raw.toUpperCase(Locale.ROOT);if(u.contains("COMUNIDADES")||u.contains("COMUNIDAD INTEGRAL"))return"Comunidades";if(u.contains("DECESOS INTEGRAL"))return"Decesos";if(u.contains("ASISTENCIA FAMILIAR"))return"Asistencia Familiar";if(u.contains("ACCIDENTES DE LA MUJER"))return"Accidentes de la Mujer";if(u.contains("VIDA A PRIMA PERIODICA")||u.contains("AHORRO GARANTIZADO FLEXIBLE"))return"Vida/Ahorro";if(u.contains("RESPONSABILIDAD CIVIL"))return"Responsabilidad Civil";if(u.contains("HOGAR PROTECCION")||u.contains("HOGAR SENIOR"))return"Hogar";return findPolicyType(raw);}
    private String findPremium(String raw){Matcher m=Pattern.compile("(?is)(?:TOTAL RECIBO|TOT\\.? RECIBO)\\s*([0-9.,]+\\s*€?)").matcher(raw);if(m.find())return m.group(1).trim();m=Pattern.compile("(?is)(?:PRIMA NETA|IMPORTE DEL RECIBO|IMPORTE DEL SEGURO)\\s*[:.]?\\s*([0-9.,]+\\s*€?)").matcher(raw);return m.find()?m.group(1).trim():"";}
    private JSONArray extractInsuredRowsV2(String raw){JSONArray out=new JSONArray();Matcher m=Pattern.compile("(?m)^\\s*(\\d{1,3})\\s+([0-9]{8}[A-Z])\\s+(.+?)\\s+(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})\\s+([MV])\\s+(\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4})\\s*$",Pattern.CASE_INSENSITIVE).matcher(raw.replace('\\r','\\n'));while(m.find()){try{JSONObject p=new JSONObject();p.put("row",m.group(1));p.put("identityNumber",m.group(2));p.put("name",clean(m.group(3)));p.put("birthDate",m.group(4));p.put("sex",m.group(5));p.put("rightsDate",m.group(6));out.put(p);}catch(Exception ignored){}}return out;}
    private JSONArray extractCapitalRowsV2(String raw,int count){JSONArray out=new JSONArray();Matcher m=Pattern.compile("(?mi)^\\s*(?:A\\s*-\\s*HUELVA|DECESOS|FALLECIMIENTO[^\\n]*)?[^\\n]*?((?:\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+(?:[.,]\\d{2})?))\\s+((?:\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+(?:[.,]\\d{2})?))(?:\\s+((?:\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+(?:[.,]\\d{2})?)))?\\s*$").matcher(raw);while(m.find()){try{String whole=m.group();String u=whole.toUpperCase(Locale.ROOT);if(!(u.contains("DECES")||u.contains("FALLECIMIENTO")||u.contains("INVALIDEZ")||u.contains("CAPITAL")))continue;JSONObject r=new JSONObject();r.put("coverage",whole.replaceAll("\\s+"," ").trim());JSONArray vals=new JSONArray();for(int i=1;i<=m.groupCount();i++)if(m.group(i)!=null)vals.put(m.group(i));r.put("values",vals);out.put(r);}catch(Exception ignored){}}return out;}
    private int policySignalsV2(String raw){String u=raw.toUpperCase(Locale.ROOT);int s=0;String[] k={"PÓLIZA","POLIZA","TOMADOR","FECHA DE EFECTO","VENCIMIENTO","IMPORTE DEL SEGURO","PRIMA NETA","ASEGURADOS","CAPITALES","CONDICIONES PARTICULARES","OCASO","Nº DE PÓLIZA","NÚMERO DE PÓLIZA"};for(String x:k)if(u.contains(x))s++;return s;}

'''
    s=s.replace(marker,helper+marker,1)
# Change parse() body to product-aware parser while preserving DNI base behavior.
old='    private JSONObject parse(String raw)throws Exception{return PolicyOcrParser.parse(raw);}'
if old in s:
    s=s.replace(old,'    private JSONObject parse(String raw)throws Exception{JSONObject o=PolicyOcrParser.parse(raw);String type=findPolicyTypeV2(raw);if(!"Póliza".equals(type)||raw.toUpperCase(Locale.ROOT).contains("TOMADOR")||raw.toUpperCase(Locale.ROOT).contains("CONDICIONES PARTICULARES")){JSONObject p=parsePolicyExamplesV2(raw);for(java.util.Iterator<String> it=p.keys();it.hasNext();){String k=it.next();o.put(k,p.get(k));}}return o;}',1)

p.write_text(s,encoding='utf-8')
print('policy example patterns applied')
