package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** OCR DNI/NIE: solo campos definidos para RgaPro. */
public final class DniOcrParser {
    public static final class Result {
        public String name="", surname="", dni="", birthDate="", address="";
        public int confidence=0;
    }
    private DniOcrParser() {}

    public static Result parse(String raw){
        Result r=new Result();
        String text=normalize(raw==null?"":raw);
        String[] lines=text.split("\\R");
        for(int i=0;i<lines.length;i++){
            String line=clean(lines[i]);
            if(line.isEmpty()) continue;
            if(starts(line,"APELLIDOS","APELLIDO")) r.surname=readValue(lines,i,"APELLIDOS","APELLIDO");
            else if(starts(line,"NOMBRE")) r.name=readValue(lines,i,"NOMBRE");
            else if(containsLabel(line,"DOMICILIO","DIRECCION","DIRECCIÓN")) r.address=readLabeled(lines,i,"DOMICILIO","DIRECCION","DIRECCIÓN");
        }
        r.dni=findDni(text);
        r.birthDate=findBirthDate(text,lines);
        String mrz=collectMrz(lines);
        if(!mrz.isEmpty()) parseMrz(mrz,r);
        if(r.name.isEmpty()||r.surname.isEmpty()) parseMrzNames(mrz,r);
        r.name=clean(r.name); r.surname=clean(r.surname); r.address=clean(r.address);
        int score=0;
        if(!r.dni.isEmpty() && (isValidDni(r.dni)||isValidNie(r.dni))) score+=35;
        if(!r.name.isEmpty()) score+=25;
        if(!r.surname.isEmpty()) score+=20;
        if(!r.birthDate.isEmpty()) score+=20;
        r.confidence=Math.min(100,score);
        return r;
    }

    private static String normalize(String s){
        return s.toUpperCase(Locale.ROOT)
            .replace("APELLlDOS","APELLIDOS").replace("APELLlDO","APELLIDO")
            .replace("N0MBRE","NOMBRE").replace("NACIMlENTO","NACIMIENTO")
            .replace("NACIMlENT0","NACIMIENTO").replace("DOMIC1LIO","DOMICILIO");
    }

    private static String readValue(String[] lines,int i,String... labels){
        String v=valueAfter(lines[i],labels);
        if(isUseful(v)) return v;
        if(i+1<lines.length){String n=clean(lines[i+1]); if(isUseful(n) && !fieldLabel(n)) return n;}
        return "";
    }

    private static String readLabeled(String[] lines,int i,String... labels){
        String v=valueAfter(lines[i],labels);
        if(isUseful(v)) return v;
        if(i+1<lines.length){String n=clean(lines[i+1]); if(isUseful(n) && !fieldLabel(n)) return n;}
        return "";
    }

    private static String valueAfter(String line,String... labels){
        for(String label:labels){
            if(line.startsWith(label)) return line.substring(label.length()).replaceFirst("^[\\s:.;-]+","").trim();
            int p=line.indexOf(" "+label+" ");
            if(p>=0) return line.substring(p+label.length()+1).replaceFirst("^[\\s:.;-]+","").trim();
        }
        return "";
    }

    private static boolean starts(String line,String... labels){for(String l:labels) if(line.startsWith(l)) return true; return false;}
    private static boolean containsLabel(String line,String... labels){for(String l:labels) if(line.contains(l)) return true; return false;}
    private static boolean isUseful(String s){return s!=null && s.length()>1 && !fieldLabel(s);}
    private static boolean fieldLabel(String s){
        return s.startsWith("NOMBRE")||s.startsWith("APELLIDO")||s.startsWith("SEXO")||s.contains("NACIONALIDAD")||
               s.contains("NACIMIENTO")||s.contains("DOMICILIO")||s.contains("DIRECCION")||s.contains("SOPORTE")||
               s.contains("VALIDEZ")||s.contains("CADUCIDAD")||s.contains("EMISION")||s.startsWith("FIRMA");
    }

    private static String findDni(String text){
        Matcher m=Pattern.compile("(?:DNI|NIF|NIE)\\s*[:.-]?\\s*((?:[0-9]\\s*){8}|[XYZ]\\s*(?:[0-9]\\s*){7})\\s*([A-Z])\\b").matcher(text);
        while(m.find()){
            String c=(m.group(1)+m.group(2)).replaceAll("\\s","");
            if(isValidDni(c)||isValidNie(c)) return c;
        }
        m=Pattern.compile("(?<![A-Z0-9])((?:\\d\\s*){8})([A-Z])(?![A-Z0-9])").matcher(text);
        while(m.find()){String c=(m.group(1)+m.group(2)).replaceAll("\\s","");if(isValidDni(c))return c;}
        m=Pattern.compile("(?<![A-Z0-9])([XYZ]\\s*(?:\\d\\s*){7})([A-Z])(?![A-Z0-9])").matcher(text);
        while(m.find()){String c=(m.group(1)+m.group(2)).replaceAll("\\s","");if(isValidNie(c))return c;}
        return "";
    }

    private static String findBirthDate(String text,String[] lines){
        Matcher m=Pattern.compile("(?:NACIMIENTO|NACIMIENT0|NACIMlENTO|NAC)\\s*[:.-]?\\s*(\\d{2}\\s*[ /.-]\\s*\\d{2}\\s*[ /.-]\\s*\\d{4})").matcher(text);
        if(m.find()) return normalizeDate(m.group(1));
        for(int i=0;i<lines.length;i++) if(lines[i].contains("NACIMIENTO") && i+1<lines.length){String d=firstDate(lines[i+1]);if(!d.isEmpty())return d;}
        return "";
    }

    private static void parseMrz(String mrz,Result r){
        String compact=mrz.replace("<","");
        Matcher dates=Pattern.compile("(\\d{6})\\d[MF](\\d{6})").matcher(compact);
        if(dates.find()) r.birthDate=mrzDate(dates.group(1));
        Matcher id=Pattern.compile("(?:IDESP|IDESP<|IDESP<<)([0-9]{8}[A-Z])").matcher(mrz);
        if(id.find() && isValidDni(id.group(1))) r.dni=id.group(1);
        parseMrzNames(mrz,r);
    }

    private static void parseMrzNames(String mrz,Result r){
        if(mrz==null||mrz.isEmpty()) return;
        Matcher names=Pattern.compile("([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)+)<<([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)").matcher(mrz);
        if(names.find()){
            if(r.surname.isEmpty()) r.surname=names.group(1).replace('<',' ').replaceAll("\\s+"," ").trim();
            if(r.name.isEmpty()) r.name=names.group(2).replace('<',' ').replaceAll("\\s+"," ").trim();
        }
    }

    private static String firstDate(String s){Matcher m=Pattern.compile("\\b\\d{2}\\s*[ /.-]\\s*\\d{2}\\s*[ /.-]\\s*\\d{4}\\b").matcher(s==null?"":s);return m.find()?normalizeDate(m.group()):"";}
    private static String normalizeDate(String s){return s.replaceAll("\\s+","").replace('-','/').replace('.','/');}
    private static String collectMrz(String[] lines){StringBuilder b=new StringBuilder();for(String line:lines){String c=line.toUpperCase(Locale.ROOT).replace(" ","");if(c.length()>=20&&(c.contains("<<")||c.startsWith("IDESP"))){if(b.length()>0)b.append('\n');b.append(c.replaceAll("[^A-Z0-9<]",""));}}return b.toString();}
    private static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static boolean isValidDni(String v){if(v==null||!v.matches("\\d{8}[A-Z]"))return false;String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(v.substring(0,8))%23)==v.charAt(8);}catch(Exception e){return false;}}
    private static boolean isValidNie(String v){if(v==null||!v.matches("[XYZ]\\d{7}[A-Z]"))return false;String n=(v.charAt(0)=='X'?"0":v.charAt(0)=='Y'?"1":"2")+v.substring(1,8);String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(n)%23)==v.charAt(8);}catch(Exception e){return false;}}
    private static String mrzDate(String yyMMdd){try{int yy=Integer.parseInt(yyMMdd.substring(0,2));int year=yy<=30?2000+yy:1900+yy;return String.format(Locale.ROOT,"%02d/%02d/%04d",Integer.parseInt(yyMMdd.substring(4,6)),Integer.parseInt(yyMMdd.substring(2,4)),year);}catch(Exception e){return "";}}
}
