package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser OCR tolerante para DNI/NIE español. Prioriza campos esenciales y evita texto irrelevante. */
public final class DniOcrParser {
    public static final class Result {
        public String holder="", surname="", name="", dni="", birthDate="", nationality="", sex="";
        public String address="", birthPlace="", parents="", supportNumber="", issueDate="", validityDate="", mrz="";
        public int confidence=0;
    }
    private DniOcrParser(){}

    public static Result parse(String raw){
        Result r=new Result();
        String text=normalizeOcr(raw==null?"":raw);
        String[] lines=text.split("\\R");
        for(int i=0;i<lines.length;i++){
            String line=clean(lines[i]);
            if(line.isEmpty()) continue;
            if(isLabel(line,"APELLIDOS","APELLIDO")) r.surname=collectSurname(lines,i);
            else if(isLabel(line,"NOMBRE")){
                String v=valueAfter(line,"NOMBRE");
                if(v.isEmpty()) v=nextSimpleValue(lines,i);
                if(isHumanName(v)) r.name=v;
            } else if(line.contains("NACIONALIDAD")) r.nationality=valueAfter(line,"NACIONALIDAD");
            else if(isLabel(line,"SEXO")) r.sex=valueAfter(line,"SEXO");
            else if(line.contains("DOMICILIO")){
                String v=valueAfter(line,"DOMICILIO"); if(v.isEmpty())v=nextSimpleValue(lines,i); r.address=v;
            } else if(line.contains("LUGAR DE NACIMIENTO")){
                String v=valueAfter(line,"LUGAR DE NACIMIENTO"); if(v.isEmpty())v=nextSimpleValue(lines,i); r.birthPlace=v;
            } else if(line.contains("NUM SOPORTE")||line.contains("Nº SOPORTE")||line.contains("N° SOPORTE")){
                String v=valueAfter(line,"NUM SOPORTE","Nº SOPORTE","N° SOPORTE"); if(v.isEmpty())v=nextSimpleValue(lines,i); r.supportNumber=v;
            } else if(line.contains("EMISION")||line.contains("EMISIÓN")){
                r.issueDate=firstDate(line); if(r.issueDate.isEmpty())r.issueDate=firstDate(nextSimpleValue(lines,i));
            } else if(line.contains("VALIDEZ")||line.contains("CADUCIDAD")){
                r.validityDate=firstDate(line); if(r.validityDate.isEmpty())r.validityDate=firstDate(nextSimpleValue(lines,i));
            }
        }

        r.dni=findDni(text);
        Matcher birth=Pattern.compile("(?:NACIMIENTO|NAC)\\s*[:.-]?\\s*(\\d{2}[ /.-]\\d{2}[ /.-]\\d{4})").matcher(text);
        if(birth.find()) r.birthDate=normalizeDate(birth.group(1));
        if(r.birthDate.isEmpty()){
            for(int i=0;i<lines.length;i++) if(lines[i].contains("NACIMIENTO")){
                String v=firstDate(lines[i]); if(v.isEmpty()&&i+1<lines.length)v=firstDate(lines[i+1]);
                if(!v.isEmpty()){r.birthDate=v;break;}
            }
        }

        String mrz=collectMrz(lines); r.mrz=mrz;
        if(!mrz.isEmpty()){
            Matcher names=Pattern.compile("([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)+)<<([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)").matcher(mrz);
            if(names.find()){
                String s=names.group(1).replace('<',' ').replaceAll("\\s+"," ").trim();
                String n=names.group(2).replace('<',' ').replaceAll("\\s+"," ").trim();
                if(!s.isEmpty())r.surname=s; if(!n.isEmpty())r.name=n;
            }
            Matcher id=Pattern.compile("(?:IDESP|IDESP<|IDESP<<|IDESP<C?ID?)([0-9]{8}[A-Z])").matcher(mrz);
            if(id.find()&&isValidDni(id.group(1)))r.dni=id.group(1);
            String compact=mrz.replace("<","");
            Matcher dates=Pattern.compile("(\\d{6})\\d([MF])(\\d{6})").matcher(compact);
            if(dates.find()){
                r.birthDate=mrzDate(dates.group(1)); r.sex=dates.group(2); r.validityDate=mrzDate(dates.group(3));
            }
            if(r.nationality.isEmpty()&&mrz.contains("ESP"))r.nationality="ESP";
        }
        if(r.name.isEmpty()||r.surname.isEmpty()){
            for(int i=0;i<lines.length;i++){
                String l=clean(lines[i]);
                if(r.name.isEmpty()&&l.startsWith("NOMBRE")){
                    String v=valueAfter(l,"NOMBRE"); if(v.isEmpty()&&i+1<lines.length)v=clean(lines[i+1]); if(isHumanName(v))r.name=v;
                }
                if(r.surname.isEmpty()&&(l.startsWith("APELLIDOS")||l.startsWith("APELLIDO")))r.surname=collectSurname(lines,i);
            }
        }
        if(!r.name.isEmpty()||!r.surname.isEmpty())r.holder=(r.name+" "+r.surname).trim();
        int score=0; if(!r.dni.isEmpty())score+=25; if(!r.name.isEmpty())score+=20; if(!r.surname.isEmpty())score+=20;
        if(!r.birthDate.isEmpty())score+=15; if(!r.validityDate.isEmpty())score+=10; if(!r.nationality.isEmpty())score+=5; if(!r.sex.isEmpty())score+=5;
        r.confidence=Math.min(100,score); return r;
    }

    private static String normalizeOcr(String s){
        return s.toUpperCase(Locale.ROOT)
                .replace("APELLlDOS","APELLIDOS").replace("APELLlDO","APELLIDO")
                .replace("N0MBRE","NOMBRE").replace("N0MBRES","NOMBRES")
                .replace("NACIMlENTO","NACIMIENTO").replace("NACIMlENT0","NACIMIENTO")
                .replace("NAC10NALIDAD","NACIONALIDAD").replace("DOMIC1LIO","DOMICILIO")
                .replace("VALlDEZ","VALIDEZ").replace("EMlSION","EMISION");
    }
    private static String findDni(String text){
        Pattern labeled=Pattern.compile("(?:DNI|NIF|NIE)\\s*[:.-]?\\s*((?:[0-9]\\s*){8}|[XYZ]\\s*[0-9\\s]{7})\\s*([A-Z])\\b");
        Matcher m=labeled.matcher(text); String fallback="";
        while(m.find()){
            String c=(m.group(1)+m.group(2)).replaceAll("\\s","");
            if(c.matches("\\d{8}[A-Z]")&&isValidDni(c))return c;
            if(c.matches("[XYZ]\\d{7}[A-Z]")&&isValidNie(c))return c;
            if(fallback.isEmpty())fallback=c;
        }
        Pattern loose=Pattern.compile("(?<![A-Z0-9])((?:\\d\\s*){8})([A-Z])(?![A-Z0-9])");
        m=loose.matcher(text); while(m.find()){
            String c=(m.group(1)+m.group(2)).replaceAll("\\s",""); if(isValidDni(c))return c; if(fallback.isEmpty())fallback=c;
        }
        Pattern nie=Pattern.compile("(?<![A-Z0-9])([XYZ]\\s*(?:\\d\\s*){7})([A-Z])(?![A-Z0-9])");
        m=nie.matcher(text); while(m.find()){
            String c=(m.group(1)+m.group(2)).replaceAll("\\s",""); if(isValidNie(c))return c; if(fallback.isEmpty())fallback=c;
        }
        return fallback;
    }
    private static String collectSurname(String[] lines,int i){
        String first=valueAfter(lines[i],"APELLIDOS","APELLIDO"); StringBuilder b=new StringBuilder();
        if(isHumanName(first))b.append(first);
        for(int j=i+1;j<Math.min(lines.length,i+4);j++){
            String x=clean(lines[j]); if(x.isEmpty()||isFieldLabel(x))break;
            if(isHumanName(x)){if(b.length()>0)b.append(' ');b.append(x);}else break;
        }
        return b.toString().replaceAll("\\s+"," ").trim();
    }
    private static boolean isFieldLabel(String s){return s.startsWith("NOMBRE")||s.startsWith("SEXO")||s.contains("NACIONALIDAD")||s.contains("NACIMIENTO")||s.contains("DOMICILIO")||s.contains("SOPORTE")||s.contains("VALIDEZ")||s.contains("CADUCIDAD")||s.contains("EMISION")||s.startsWith("FIRMA")||s.startsWith("NUMERO");}
    private static boolean isHumanName(String s){return s!=null&&s.matches("[A-ZÁÉÍÓÚÑ]{2,}(?:[ -][A-ZÁÉÍÓÚÑ]{2,})*")&&!isFieldLabel(s)&&!s.contains("REINO")&&!s.contains("ESPAÑA")&&!s.equals("DOCUMENTO")&&!s.equals("NACIONAL");}
    private static boolean isLabel(String line,String... labels){for(String l:labels)if(line.startsWith(l))return true;return false;}
    private static String valueAfter(String line,String... labels){for(String l:labels)if(line.startsWith(l))return line.substring(l.length()).replaceFirst("^[ :.-]+","").trim();return "";}
    private static String nextSimpleValue(String[] lines,int i){if(i+1<lines.length){String x=clean(lines[i+1]);if(x.length()>1&&!isFieldLabel(x))return x;}return "";}
    private static String firstDate(String s){if(s==null)return "";Matcher m=Pattern.compile("\\b\\d{2}[ /.-]\\d{2}[ /.-]\\d{4}\\b").matcher(s);return m.find()?normalizeDate(m.group()):"";}
    private static String normalizeDate(String s){return s.replace('-','/').replace('.','/').replaceAll("\\s+","");}
    private static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static String collectMrz(String[] lines){StringBuilder b=new StringBuilder();for(String line:lines){String c=line.toUpperCase(Locale.ROOT).replace(" ","");if(c.length()>=20&&(c.contains("<<")||c.startsWith("IDESP")||c.matches("[A-Z0-9<]{20,}"))){if(b.length()>0)b.append('\\n');b.append(c.replaceAll("[^A-Z0-9<]",""));}}return b.toString().trim();}
    private static boolean isValidDni(String v){if(v==null||!v.matches("\\d{8}[A-Z]"))return false;String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(v.substring(0,8))%23)==v.charAt(8);}catch(Exception e){return false;}}
    private static boolean isValidNie(String v){if(v==null||!v.matches("[XYZ]\\d{7}[A-Z]"))return false;String n=(v.charAt(0)=='X'?"0":v.charAt(0)=='Y'?"1":"2")+v.substring(1,8);String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(n)%23)==v.charAt(8);}catch(Exception e){return false;}}
    private static String mrzDate(String yyMMdd){try{int yy=Integer.parseInt(yyMMdd.substring(0,2));int year=yy<=30?2000+yy:1900+yy;return String.format(Locale.ROOT,"%02d/%02d/%04d",Integer.parseInt(yyMMdd.substring(4,6)),Integer.parseInt(yyMMdd.substring(2,4)),year);}catch(Exception e){return "";}}
}
