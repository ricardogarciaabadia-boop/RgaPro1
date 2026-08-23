from pathlib import Path

p=Path('app/src/main/java/com/rgapro1/ocaso/PolicyOcrParser.java')
s=p.read_text(encoding='utf-8')

new='''package com.rgapro1.ocaso;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

/** Robust policy OCR parser. It deliberately avoids treating OCR fragments from
 * labels (e.g. "TOMADOR / ASEGURADO", "CAPITALES ASEGURADOS") as field values. */
public final class PolicyOcrParser {
  private PolicyOcrParser(){}

  public static JSONObject parse(String raw) throws Exception {
    String text=raw==null?"":raw;
    String u=text.toUpperCase(Locale.ROOT);
    JSONObject o=new JSONObject();
    DniOcrParser.Result d=DniOcrParser.parse(text);
    o.put("documentNumber",d.dni);
    o.put("birthDate",d.birthDate);
    o.put("name",d.name);
    o.put("surname",d.surname);
    o.put("raw",text);

    boolean policy=isPolicy(u);
    if(!policy){
      o.put("confidence",Math.max(0,d.confidence));
      o.put("policyType","");
      return o;
    }

    String type=product(u);
    o.put("policyType",type);
    o.put("policyNumber",first(text,"(?i)(?:N[º°O]?\\s*)?P[ÓO]LIZA\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})"));
    if(o.optString("policyNumber").isEmpty())
      o.put("policyNumber",first(text,"(?i)(?:N[ÚU]MERO\\s+DE\\s+P[ÓO]LIZA|N[º°]\\s*POLIZA)\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})"));

    String holder=label(text,"TOMADOR DEL SEGURO","TOMADOR","TOMADORA","CONTRATANTE","TITULAR");
    if(isLabelOnly(holder)) holder="";
    o.put("holder",holder);
    o.put("holderDni",d.dni);
    o.put("effectiveDate",date(text,"FECHA DE EFECTO|FECHA EFECTO|EFECTO|INICIO"));
    o.put("issueDate",date(text,"FECHA DE EMISIÓN|FECHA DE EMISION|EMISIÓN|EMISION"));
    o.put("policyExpiry",date(text,"FECHA DE VENCIMIENTO|VENCIMIENTO|VENC|CADUCIDAD|VALIDEZ|HASTA"));
    o.put("phone",first(text,"(?<!\\d)(?:\\+34[\\s.-]?)?[6789]\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{3}(?!\\d)"));
    o.put("email",first(text,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));
    o.put("address",label(text,"DOMICILIO","DIRECCIÓN","DIRECCION","DOMICILIO DEL TOMADOR","DIRECCIÓN DEL TOMADOR"));
    o.put("capital",label(text,"CAPITAL ASEGURADO","SUMA ASEGURADA","CAPITALES ASEGURADOS","CAPITALES ASEGURADOS POR PERSONA","CAPITAL"));
    o.put("company",u.contains("OCASO")?"Ocaso":"");
    if(type.equals("Vida")||type.equals("Ahorro"))
      o.put("premiums",label(text,"PRIMA ANUAL","PRIMA PERIÓDICA","PRIMA PERIODICA","PRIMAS","PRIMA"));
    if(type.equals("Decesos"))
      o.put("insureds",insureds(text));

    int confidence=35;
    if(!o.optString("policyNumber").isEmpty()) confidence+=25;
    if(!holder.isEmpty()) confidence+=15;
    if(!o.optString("policyExpiry").isEmpty()||!o.optString("effectiveDate").isEmpty()) confidence+=10;
    if(!o.optString("capital").isEmpty()) confidence+=5;
    if(!o.optString("company").isEmpty()) confidence+=5;
    if(!type.equals("Póliza")) confidence+=5;
    o.put("confidence",Math.min(100,confidence));
    return o;
  }

  static boolean isPolicy(String u){
    return u.matches("(?s).*\\b(P[ÓO]LIZA|POLIZA|N[ÚU]MERO\\s+DE\\s+P[ÓO]LIZA|TOMADOR|CONTRATANTE|CONDICIONES\\s+PARTICULARES|FECHA\\s+DE\\s+EFECTO|PRIMA|CAPITAL\\s+ASEGURADO)\\b.*");
  }

  static String product(String u){
    if(u.contains("DECESOS")||u.contains("DECESO")||u.contains("SEPELIO")||u.contains("ASISTENCIA FAMILIAR"))return "Decesos";
    if(u.contains("AHORRO")||u.contains("PLAN DE AHORRO")||u.contains("RENTA")||u.contains("PIAS"))return "Ahorro";
    if(u.contains("VIDA")||u.contains("FALLECIMIENTO"))return "Vida";
    if(u.contains("COMUNIDADES")||u.contains("COMUNIDAD"))return "Comunidades";
    if(u.contains("HOGAR")||u.contains("VIVIENDA ASEGURADA")||(u.contains("CONTINENTE")&&u.contains("CONTENIDO")))return "Hogar";
    if(u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL")||u.contains("VEHÍCULO")||u.contains("MATRÍCULA"))return "Auto";
    if(u.contains("SALUD")||u.contains("ASISTENCIA SANITARIA"))return "Salud";
    return "Póliza";
  }

  static String first(String t,String r){
    Matcher m=Pattern.compile(r,Pattern.CASE_INSENSITIVE).matcher(t==null?"":t);
    if(!m.find())return "";
    String v=m.groupCount()>0&&m.group(1)!=null?m.group(1):m.group();
    return clean(v);
  }

  static String label(String t,String...labels){
    if(t==null)return "";
    String[] lines=t.replace('\\r','\\n').split("\\n");
    for(int i=0;i<lines.length;i++){
      String line=lines[i].trim();
      if(line.isEmpty())continue;
      for(String lab:labels){
        Pattern p=Pattern.compile("(?i)(?<![A-ZÁÉÍÓÚÜÑ0-9])"+Pattern.quote(lab)+"(?![A-ZÁÉÍÓÚÜÑ0-9])");
        Matcher m=p.matcher(line);
        if(!m.find())continue;
        String value=line.substring(m.end()).replaceFirst("^[\\s:;,.\\-_/]+","").trim();
        value=stopAtNextLabel(value,labels);
        if(!isLabelOnly(value)&&isUseful(value))return clean(value);
        for(int j=i+1;j<Math.min(lines.length,i+4);j++){
          String next=lines[j].trim();
          if(next.isEmpty())continue;
          if(looksLikeLabel(next))break;
          next=stopAtNextLabel(next,labels);
          if(isUseful(next)&&!isLabelOnly(next))return clean(next);
        }
      }
    }
    return "";
  }

  static String stopAtNextLabel(String v,String[] labels){
    if(v==null)return "";
    String best=v;
    for(String lab:labels){
      Matcher m=Pattern.compile("(?i)\\s+(?:"+Pattern.quote(lab)+")\\s*[:;-]?").matcher(best);
      if(m.find())best=best.substring(0,m.start()).trim();
    }
    return best;
  }

  static boolean looksLikeLabel(String s){
    String u=s.toUpperCase(Locale.ROOT).replaceAll("[^A-ZÁÉÍÓÚÜÑ0-9 ]"," ").trim();
    return u.matches("(?:TOMADOR|TOMADORA|TITULAR|ASEGURADO|ASEGURADOS|ASEGURADA|CAPITAL|CAPITALES|DOMICILIO|DIRECCION|DIRECCIÓN|PRIMA|PÓLIZA|POLIZA|VENCIMIENTO|FECHA|EMAIL|CORREO|TELÉFONO|TELEFONO)(?:\\s+[A-ZÁÉÍÓÚÜÑ]+){0,4}");
  }

  static boolean isLabelOnly(String s){
    if(s==null||s.trim().isEmpty())return true;
    String u=s.toUpperCase(Locale.ROOT).replaceAll("[.:;,_/-]"," ").replaceAll("\\s+"," ").trim();
    return u.matches("(?:TOMADOR|TOMADORA|ASEGURADO|ASEGURADA|ASEGURADOS|ASEGURADAS|TITULAR|CAPITAL|CAPITALES|CAPITALES ASEGURADOS|PRIMA|PÓLIZA|POLIZA|DEL SEGURO|DE SEGURO|ES ASEGURADOS)");
  }

  static boolean isUseful(String s){
    if(s==null)return false;
    String x=s.trim();
    if(x.length()<3||x.length()>180)return false;
    if(x.matches("^[.:;,_/\\- ]+$"))return false;
    return true;
  }

  static String clean(String s){return s==null?"":s.replaceAll("\\s+"," ").trim();}

  static String date(String t,String labels){
    Matcher m=Pattern.compile("(?i)(?:"+labels+")\\s*[:.-]?\\s*(\\d{1,2}\\s*[/. -]\\s*\\d{1,2}\\s*[/. -]\\s*\\d{4})").matcher(t==null?"":t);
    return m.find()?m.group(1).replaceAll("\\s+","").replace('-','/').replace('.','/') :"";
  }

  static JSONArray insureds(String t)throws Exception{
    JSONArray out=new JSONArray();
    String[] ls=(t==null?"":t).replace('\\r','\\n').split("\\n");
    for(int i=0;i<ls.length;i++){
      String line=ls[i].trim();
      if(line.isEmpty())continue;
      String u=line.toUpperCase(Locale.ROOT);
      if(!(u.matches(".*\\bASEGURAD(?:O|A|OS|AS)\\b.*")))continue;
      String b=line;
      for(int k=1;k<=2&&i+k<ls.length;k++)b+=" "+ls[i+k].trim();
      Matcher dm=Pattern.compile("\\b(?:\\d{8}[A-Z]|[XYZ]\\d{7}[A-Z])\\b",Pattern.CASE_INSENSITIVE).matcher(b);
      String dni=dm.find()?dm.group():"";
      Matcher dt=Pattern.compile("\\b\\d{1,2}\\s*[/. -]\\s*\\d{1,2}\\s*[/. -]\\s*\\d{4}\\b").matcher(b);
      String birth=dt.find()?dt.group().replaceAll("\\s+",""):"";
      Matcher cm=Pattern.compile("(?:\\d{1,3}(?:\\.\\d{3})*|\\d+)(?:,\\d{2})?\\s*€").matcher(b);
      String cap=cm.find()?cm.group():"";
      String name=b.replace(dni,"").replace(birth,"").replace(cap,"").replaceAll("(?i)^(?:ASEGURAD(?:O|A|OS|AS))\\s*[:.-]?\\s*","").replaceAll("\\s+"," ").trim();
      if(isLabelOnly(name)||name.length()<3)continue;
      JSONObject x=new JSONObject();x.put("name",name);x.put("surname","");x.put("dni",dni);x.put("birthDate",birth);x.put("rightsDate","");x.put("capital",cap);out.put(x);
    }
    return out;
  }
}
'''
p.write_text(new,encoding='utf-8')

# Make the native OCR Activity consume the product-aware parser for every document.
a=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s=a.read_text(encoding='utf-8')
old='private JSONObject parse(String raw)throws Exception{DniOcrParser.Result r=DniOcrParser.parse(raw);JSONObject o=new JSONObject();o.put("documentNumber",r.dni);o.put("birthDate",r.birthDate);o.put("name",r.name);o.put("surname",r.surname);o.put("mrzStatus",r.mrz.isEmpty()?"No confirmada":"Detectada: revisar checksum");o.put("confidence",r.confidence);o.put("raw",raw==null?"":raw);o.put("policyNumber",findPolicyNumber(raw));o.put("policyType",findPolicyType(raw));o.put("policyExpiry",findDate(raw,"(?:VENCIMIENTO|VENC|FECHA DE VENCIMIENTO|VALIDEZ|CADUCIDAD)"));o.put("effectiveDate",findDate(raw,"(?:FECHA DE EFECTO|EFECTO|INICIO)"));o.put("holder",findLabeled(raw,"TOMADOR","ASEGURADO","CLIENTE","TITULAR"));o.put("capital",findLabeled(raw,"CAPITAL","SUMA ASEGURADA","CAPITALES","CAPITAL ASEGURADO"));o.put("phone",find(raw,"(?:\\\\+34\\\\s*)?[6789]\\\\d{8}"));o.put("email",find(raw,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\\\.[A-Z]{2,}"));o.put("address",findLabeled(raw,"DIRECCIÓN","DIRECCION","DOMICILIO","RIESGO"));o.put("company",findCompany(raw));return o;}'
start=s.find('private JSONObject parse(String raw)throws Exception{')
if start<0: raise SystemExit('parse method not found')
brace=s.find('{',start);depth=0;end=None
for i in range(brace,len(s)):
    if s[i]=='{':depth+=1
    elif s[i]=='}':
        depth-=1
        if depth==0:
            end=i+1;break
if end is None: raise SystemExit('parse braces')
replacement='private JSONObject parse(String raw)throws Exception{return PolicyOcrParser.parse(raw);}'
s=s[:start]+replacement+s[end:]
a.write_text(s,encoding='utf-8')
