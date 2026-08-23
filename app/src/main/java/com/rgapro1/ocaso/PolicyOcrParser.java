package com.rgapro1.ocaso;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

public final class PolicyOcrParser {
  private PolicyOcrParser(){}
  public static JSONObject parse(String raw) throws Exception {
    String text=raw==null?"":raw; String u=text.toUpperCase(Locale.ROOT);
    DniOcrParser.Result d=DniOcrParser.parse(text); JSONObject o=new JSONObject();
    o.put("documentNumber",d.dni); o.put("birthDate",d.birthDate); o.put("name",d.name); o.put("surname",d.surname); o.put("confidence",d.confidence); o.put("raw",text);
    if(!isPolicy(u)){o.put("policyType","");return o;}
    String type=product(u); o.put("policyType",type);
    o.put("policyNumber",first(text,"(?i)(?:N[º°O]?\\s*)?P[ÓO]LIZA\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})"));
    o.put("holder",labeled(text,"TOMADOR DEL SEGURO","TOMADOR","TITULAR"));
    String eff=date(text,"FECHA DE EFECTO|FECHA EFECTO|EFECTO|INICIO"); String exp=date(text,"HASTA|VENCIMIENTO|FECHA DE VENCIMIENTO|CADUCIDAD|VALIDEZ");
    o.put("effectiveDate",eff); o.put("issueDate",eff); o.put("policyExpiry",exp);
    o.put("phone",first(text,"(?:\\+34\\s*)?[6789]\\d{8}")); o.put("email",first(text,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));
    if(type.equals("Vida")||type.equals("Ahorro"))o.put("premiums",labeled(text,"PRIMA","PRIMAS","PRIMA ANUAL","PRIMA PERIÓDICA"));
    if(type.equals("Decesos"))o.put("insureds",insureds(text));
    return o;
  }
  static boolean isPolicy(String u){return u.matches("(?s).*\\b(TOMADOR|N[ÚU]MERO DE P[ÓO]LIZA|N[º°]?\\s*P[ÓO]LIZA|CONDICIONES PARTICULARES|FECHA DE EFECTO|P[ÓO]LIZA)\\b.*");}
  static String product(String u){
    if(u.contains("DECESOS")||u.contains("ASISTENCIA FAMILIAR"))return "Decesos";
    if(u.contains("AHORRO")||u.contains("PLAN DE AHORRO")||u.contains("RENTA"))return "Ahorro";
    if(u.contains("VIDA")||u.contains("FALLECIMIENTO"))return "Vida";
    if(u.contains("COMUNIDADES")||u.contains("COMUNIDAD"))return "Comunidades";
    if(u.contains("HOGAR")||u.contains("VIVIENDA ASEGURADA")||(u.contains("CONTINENTE")&&u.contains("CONTENIDO")))return "Hogar";
    if(u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL")||u.contains("VEHÍCULO")||u.contains("MATRÍCULA"))return "Auto";
    if(u.contains("SALUD")||u.contains("ASISTENCIA SANITARIA"))return "Salud";
    return "Póliza";
  }
  static String first(String t,String r){Matcher m=Pattern.compile(r,Pattern.CASE_INSENSITIVE).matcher(t);if(!m.find())return "";return m.groupCount()>0&&m.group(1)!=null?m.group(1).trim():m.group().trim();}
  static String labeled(String t,String...labels){for(String line:t.split("\\R")){String u=line.toUpperCase(Locale.ROOT);for(String l:labels){int p=u.indexOf(l);if(p>=0){String v=line.substring(Math.min(line.length(),p+l.length())).replaceFirst("^[\\s:.-]+","").trim();if(!v.isEmpty()&&!v.equalsIgnoreCase("DEL SEGURO:"))return v;}}}return "";}
  static String date(String t,String labels){Matcher m=Pattern.compile("(?i)(?:"+labels+")\\s*[:.-]?\\s*(\\d{2}\\s*[/. -]\\s*\\d{2}\\s*[/. -]\\s*\\d{4})").matcher(t);return m.find()?m.group(1).replaceAll("\\s+","").replace('-','/').replace('.','/') :"";}
  static JSONArray insureds(String t)throws Exception{JSONArray out=new JSONArray();String[] ls=t.split("\\R");for(int i=0;i<ls.length;i++){String line=ls[i].trim();if(!line.toUpperCase(Locale.ROOT).contains("ASEGURAD"))continue;String b=line+(i+1<ls.length?" "+ls[i+1]:"")+(i+2<ls.length?" "+ls[i+2]:"");Matcher dm=Pattern.compile("\\b(?:\\d{8}[A-Z]|[XYZ]\\d{7}[A-Z])\\b",Pattern.CASE_INSENSITIVE).matcher(b);String dni=dm.find()?dm.group():"";Matcher dt=Pattern.compile("\\b\\d{2}\\s*[/. -]\\s*\\d{2}\\s*[/. -]\\s*\\d{4}\\b").matcher(b);ArrayList<String> ds=new ArrayList<>();while(dt.find())ds.add(dt.group().replaceAll("\\s+",""));Matcher cm=Pattern.compile("(?:\\d{1,3}(?:\\.\\d{3})*|\\d+)(?:,\\d{2})?\\s*€").matcher(b);String cap=cm.find()?cm.group():"";String name=b.replace(dni,"").replaceAll("\\b\\d{2}\\s*[/. -]\\s*\\d{2}\\s*[/. -]\\s*\\d{4}\\b","").replace(cap,"").replaceAll("\\s+"," ").trim();JSONObject x=new JSONObject();x.put("name",name);x.put("surname","");x.put("dni",dni);x.put("birthDate",ds.size()>0?ds.get(0):"");x.put("rightsDate",ds.size()>1?ds.get(1):"");x.put("capital",cap);out.put(x);}return out;}
}
