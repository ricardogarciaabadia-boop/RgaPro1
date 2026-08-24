from pathlib import Path
import re

p=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s=p.read_text(encoding='utf-8')
# This patch keeps the proven OCR engine but improves the extraction of common
# community-policy fields. It never changes the DNI field set.

# Replace policy signal scoring if present.
old='''    private int policySignals(String u){int s=0;String[] keys={"PÓLIZA","POLIZA","TOMADOR","ASEGURADO","FECHA DE EFECTO","VENCIMIENTO","PRIMA","CAPITAL","COMUNIDAD","COMUNIDADES","DIRECCIÓN DEL RIESGO","OBJETO DEL SEGURO"};for(String k:keys)if(u.contains(k))s++;return s;}'''
new='''    private int policySignals(String u){int s=0;String[] keys={"PÓLIZA","POLIZA","TOMADOR","ASEGURADO","FECHA DE EFECTO","FECHA DE VENCIMIENTO","VENCIMIENTO","PRIMA","CAPITAL","COMUNIDAD","COMUNIDADES","DIRECCIÓN DEL RIESGO","DIRECCION DEL RIESGO","OBJETO DEL SEGURO","CONDICIONES PARTICULARES","DATOS DEL SEGURO","RIESGO ASEGURADO","CUOTA"};for(String k:keys)if(u.contains(k))s++;return s;}'''
s=s.replace(old,new)

# Make holder extraction tolerate labels split across punctuation/line breaks.
old='''    private String findLabeled(String raw,String...labels){if(raw==null)return"";for(String line:raw.split("\\\\R")){String u=line.toUpperCase(Locale.ROOT);for(String label:labels){int p=u.indexOf(label);if(p>=0){String v=line.substring(Math.min(line.length(),p+label.length())).replaceFirst("^[\\\\s:.-]+","").trim();if(!v.isEmpty())return v;}}}return"";}'''
new='''    private String findLabeled(String raw,String...labels){if(raw==null)return"";for(String line:raw.split("\\\\R")){String u=line.toUpperCase(Locale.ROOT);for(String label:labels){int p=u.indexOf(label);if(p>=0){String v=line.substring(Math.min(line.length(),p+label.length())).replaceFirst("^[\\\\s:;,.#-]+","").trim();if(!v.isEmpty()&&!looksLikePolicyLabel(v))return v;}}}for(String label:labels){Matcher m=Pattern.compile("(?is)"+Pattern.quote(label)+"\\\\s*[:#-]?\\\\s*([^\\\\r\\\\n]{3,120})").matcher(raw);if(m.find()){String v=m.group(1).trim();if(!looksLikePolicyLabel(v))return v;}}return"";}
    private boolean looksLikePolicyLabel(String v){String u=v.toUpperCase(Locale.ROOT);String[] x={"Nº DE PÓLIZA","NUMERO DE POLIZA","TOMADOR","ASEGURADO","VENCIMIENTO","CAPITAL","PRIMA","FECHA","COMPAÑÍA"};for(String k:x)if(u.equals(k)||u.startsWith(k+":"))return true;return false;}'''
s=s.replace(old,new)

# More tolerant policy dates.
old='''    private String findDate(String raw,String labels){if(raw==null)return"";Matcher m=Pattern.compile("(?i)"+labels+"\\\\s*[:.-]?\\\\s*(\\\\d{2}\\\\s*[ /.-]\\\\s*\\\\d{2}\\\\s*[ /.-]\\\\s*\\\\d{4})").matcher(raw);return m.find()?m.group(1).replaceAll("\\\\s+","").replace('-','/').replace('.','/') :"";}'''
new='''    private String findDate(String raw,String labels){if(raw==null)return"";Matcher m=Pattern.compile("(?i)"+labels+"\\\\s*[:.-]?\\\\s*(\\\\d{1,2}\\\\s*[ /.-]\\\\s*\\\\d{1,2}\\\\s*[ /.-]\\\\s*\\\\d{4})").matcher(raw);if(m.find())return m.group(1).replaceAll("\\\\s+","").replace('-','/').replace('.','/');return"";}'''
s=s.replace(old,new)

p.write_text(s,encoding='utf-8')
print('policy OCR improvement patch applied')
