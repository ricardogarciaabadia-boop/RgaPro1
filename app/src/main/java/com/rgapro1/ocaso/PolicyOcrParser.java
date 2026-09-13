package com.rgapro1.ocaso;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

public final class PolicyOcrParser {
  private PolicyOcrParser(){}
  public static JSONObject parse(String raw) throws Exception {
    String text=raw==null?"":raw;
    String u=text.toUpperCase(Locale.ROOT);
    JSONObject o=new JSONObject();
    o.put("raw",text);
    if(!isPolicy(u)){
      DniOcrParser.Result d=DniOcrParser.parse(text);
      o.put("documentNumber",d.dni); o.put("birthDate",d.birthDate);
      o.put("name",d.name); o.put("surname",d.surname); o.put("confidence",d.confidence);
      o.put("policyType","");
      return o;
    }
    String type=product(u);
    o.put("policyType",type);
    String number=first(text,"(?i)(?:N[º°O]?\\s*(?:DE\\s*)?|N[ÚU]MERO\\s+(?:DE\\s*)?)?P[ÓO]LIZA\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})");
    if(number.isEmpty()) number=first(text,"(?i)(?:N[º°O]?|N[ÚU]MERO)\\s*(?:DE\\s*)?(?:CONTRATO|P[ÓO]LIZA)\\s*[:#-]?\\s*([A-Z0-9./_-]{4,})");
    o.put("policyNumber",number);
    String holder=valueAfterLabel(text,"TOMADOR DEL SEGURO","TOMADOR","TITULAR DEL SEGURO","TITULAR","ASEGURADO");
    String[] hs=splitName(holder);
    o.put("holder",holder); o.put("name",hs[0]); o.put("surname",hs[1]);
    String eff=date(text,"FECHA DE EFECTO|FECHA EFECTO|EFECTO|INICIO|INICIO DEL SEGURO|DESDE");
    String exp=date(text,"HASTA|FECHA DE VENCIMIENTO|FECHA VENCIMIENTO|VENCIMIENTO|CADUCIDAD|VALIDEZ|FIN DEL SEGURO|HASTA EL");
    if(eff.isEmpty() || exp.isEmpty()){
      String[] range=dateRange(text);
      if(eff.isEmpty()) eff=range[0];
      if(exp.isEmpty()) exp=range[1];
    }
    o.put("effectiveDate",eff); o.put("issueDate",eff); o.put("policyExpiry",exp);
    o.put("phone",first(text,"(?<!\\d)(?:\\+34[ .-]?)?[6789]\\d{8}(?!\\d)"));
    o.put("email",first(text,"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"));
    o.put("address",valueAfterLabel(text,"DIRECCIÓN DEL RIESGO","DIRECCION DEL RIESGO","DIRECCIÓN","DIRECCION","DOMICILIO DEL RIESGO","DOMICILIO","RIESGO ASEGURADO","VIVIENDA ASEGURADA"));
    o.put("postalCode",first(text,"(?<!\\d)[0-9]{5}(?!\\d)"));
    if(type.equals("Vida")||type.equals("Ahorro"))o.put("premiums",valueAfterLabel(text,"PRIMA ANUAL","PRIMA PERIÓDICA","PRIMA PERIODICA","PRIMA","PRIMAS"));
    if(type.equals("Hogar")){
      o.put("insuredCapital",valueAfterLabel(text,"CONTINENTE","CAPITAL CONTINENTE","CAPITAL ASEGURADO CONTINENTE"));
      o.put("contentsCapital",valueAfterLabel(text,"CONTENIDO","CAPITAL CONTENIDO","CAPITAL ASEGURADO CONTENIDO"));
    }
    if(type.equals("Decesos"))o.put("insureds",insureds(text));
    return o;
  }
  static boolean isPolicy(String u){return u.matches("(?s).*\\b(TOMADOR|N[ÚU]MERO DE P[ÓO]LIZA|N[º°O]?\\s*P[ÓO]LIZA|CONDICIONES PARTICULARES|FECHA DE EFECTO|P[ÓO]LIZA|MULTIRRIESGO HOGAR|SEGURO DE HOGAR|HOGAR)\\b.*");}
  static String product(String u){
    if(u.contains("DECESOS")||u.contains("ASISTENCIA FAMILIAR"))return "Decesos";
    if(u.contains("AHORRO")||u.contains("PLAN DE AHORRO")||u.contains("RENTA"))return "Ahorro";
    if(u.contains("VIDA")||u.contains("FALLECIMIENTO"))return "Vida";
    if(u.contains("COMUNIDADES")||u.contains("COMUNIDAD"))return "Comunidades";
    if(u.contains("HOGAR")||u.contains("VIVIENDA ASEGURADA")||u.contains("MULTIRRIESGO HOGAR")||(u.contains("CONTINENTE")&&u.contains("CONTENIDO")))return "Hogar";
    if(u.contains("AUTOMOVIL")||u.contains("AUTOMÓVIL")||u.contains("VEHÍCULO")||u.contains("MATRÍCULA"))return "Auto";
    if(u.contains("SALUD")||u.contains("ASISTENCIA SANITARIA"))return "Salud";
    return "Póliza";
  }
  static String first(String t,String r){Matcher m=Pattern.compile(r,Pattern.CASE_INSENSITIVE).matcher(t);if(!m.find())return "";return m.groupCount()>0&&m.group(1)!=null?m.group(1).trim():m.group().trim();}
  static String valueAfterLabel(String t,String...labels){
    String[] lines=t.split("\\R",-1);
    for(int i=0;i<lines.length;i++){
      String line=lines[i].trim(); String up=line.toUpperCase(Locale.ROOT);
      for(String l:labels){int p=up.indexOf(l);if(p<0)continue;String v=line.substring(Math.min(line.length(),p+l.length())).replaceFirst("^[\\s:;,.#-]+","").trim();if(!v.isEmpty()&&!v.equalsIgnoreCase("DEL SEGURO"))return cleanValue(v);for(int j=i+1;j<Math.min(lines.length,i+3);j++){v=lines[j].trim();if(!v.isEmpty())return cleanValue(v);}}
    }
    return "";
  }
  static String cleanValue(String s){return s.replaceAll("\\s+"," ").trim();}
  static String[] splitName(String h){String v=cleanValue(h);if(v.isEmpty())return new String[]{"",""};String[] p=v.split(" ",2);return p.length==1?new String[]{p[0],""}:new String[]{p[0],p[1]};}
  static String date(String t,String labels){String r="(?i)(?:"+labels+")\\s*[:;.-]?\\s*(\\d{1,2}\\s*[/. -]\\s*\\d{1,2}\\s*[/. -]\\s*\\d{4})";Matcher m=Pattern.compile(r).matcher(t);return m.find()?normDate(m.group(1)):"";}
  static String[] dateRange(String t){Matcher m=Pattern.compile("(?i)(?:PER[IÍ]ODO(?: DEL SEGURO)?|VIGENCIA|VIGENTE|SEGURO)\\s*[:;.-]?\\s*(\\d{1,2}[/. -]\\d{1,2}[/. -]\\d{4})\\s*(?:AL|A|HASTA|[-–])\\s*(\\d{1,2}[/. -]\\d{1,2}[/. -]\\d{4})").matcher(t);if(m.find())return new String[]{normDate(m.group(1)),normDate(m.group(2))};Matcher n=Pattern.compile("\\b(\\d{1,2}[/. -]\\d{1,2}[/. -]\\d{4})\\s*(?:AL|A|HASTA|[-–])\\s*(\\d{1,2}[/. -]\\d{1,2}[/. -]\\d{4})\\b").matcher(t);return n.find()?new String[]{normDate(n.group(1)),normDate(n.group(2))}:new String[]{"",""};}
  static String normDate(String s){return s.replaceAll("\\s+","").replace('-','/').replace('.','/');}
  static JSONArray insureds(String t)throws Exception{JSONArray out=new JSONArray();String[] ls=t.split("\\R");for(int i=0;i<ls.length;i++){String line=ls[i].trim();if(!line.toUpperCase(Locale.ROOT).contains("ASEGURAD"))continue;String b=line+(i+1<ls.length?" "+ls[i+1]:"")+(i+2<ls.length?" "+ls[i+2]:"");Matcher dm=Pattern.compile("\\b(?:\\d{8}[A-Z]|[XYZ]\\d{7}[A-Z])\\b",Pattern.CASE_INSENSITIVE).matcher(b);String dni=dm.find()?dm.group():"";Matcher dt=Pattern.compile("\\b\\d{2}\\s*[/. -]\\s*\\d{2}\\s*[/. -]\\s*\\d{4}\\b").matcher(b);ArrayList<String> ds=new ArrayList<>();while(dt.find())ds.add(normDate(dt.group()));Matcher cm=Pattern.compile("(?:\\d{1,3}(?:\\.\\d{3})*|\\d+)(?:,\\d{2})?\\s*€").matcher(b);String cap=cm.find()?cm.group():"";String name=b.replace(dni,"").replaceAll("\\b\\d{2}\\s*[/. -]\\s*\\d{2}\\s*[/. -]\\s*\\d{4}\\b","").replace(cap,"").replaceAll("\\s+"," ").trim();JSONObject x=new JSONObject();x.put("name",name);x.put("surname","");x.put("dni",dni);x.put("birthDate",ds.size()>0?ds.get(0):"");x.put("rightsDate",ds.size()>1?ds.get(1):"");x.put("capital",cap);out.put(x);}return out;}
}
