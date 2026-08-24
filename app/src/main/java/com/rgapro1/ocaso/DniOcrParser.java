package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser OCR para DNI/NIE: solo extrae los campos necesarios para el cliente. */
public final class DniOcrParser {
    public static final class Result {
        public String holder="", surname="", name="", dni="", birthDate="", address="", phone="", email="";
        public int confidence=0;
    }
    private DniOcrParser(){}

    public static Result parse(String raw){
        Result r=new Result();
        String text=normalize(raw==null?"":raw);
        String[] lines=text.split("\\R");
        for(int i=0;i<lines.length;i++){
            String line=clean(lines[i]);
            if(line.isEmpty()) continue;
            if(starts(line,"APELLIDOS","APELLIDO")) r.surname=valueOrNext(line,lines,i,"APELLIDOS","APELLIDO");
            else if(starts(line,"NOMBRE")) r.name=valueOrNext(line,lines,i,"NOMBRE");
            else if(contains(line,"DOMICILIO","DIRECCION","DIRECCIÓN")) r.address=valueAfterAny(line,"DOMICILIO","DIRECCION","DIRECCIÓN");
            else if(contains(line,"TELÉFONO","TELEFONO","MÓVIL","MOVIL")) r.phone=findPhone(line);
            else if(contains(line,"EMAIL","E-MAIL","CORREO")) r.email=findEmail(line);
        }
        r.dni=findDni(text);
        r.birthDate=findBirthDate(text);
        if(r.address.isEmpty()) r.address=findLabeled(text,"DOMICILIO","DIRECCION","DIRECCIÓN");
        if(r.phone.isEmpty()) r.phone=findPhone(text);
        if(r.email.isEmpty()) r.email=findEmail(text);

        String[] mrz=findMrz(lines);
        if(mrz.length>0){
            Matcher names=Pattern.compile("P<ESP([A-ZÑ]+(?:<[A-ZÑ]+)+)<<([A-ZÑ]+(?:<[A-ZÑ]+)*)").matcher(mrz[0]);
            if(names.find()){
                if(r.surname.isEmpty()) r.surname=clean(namePart(names.group(1)));
                if(r.name.isEmpty()) r.name=clean(namePart(names.group(2)));
            }
            if(r.dni.isEmpty()){
                Matcher id=Pattern.compile("(?:IDESP|IDESP<|IDESP<<)([0-9]{8}[A-Z])").matcher(mrz[0]);
                if(id.find()) r.dni=id.group(1);
            }
            Matcher dates=Pattern.compile("([0-9]{6})[0-9][MF]([0-9]{6})").matcher(mrz[1]);
            if(dates.find() && r.birthDate.isEmpty()) r.birthDate=mrzDate(dates.group(1));
        }
        if(r.name.isEmpty()||r.surname.isEmpty()){
            Matcher m=Pattern.compile("NOMBRE\\s*[:.-]?\\s*([A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){0,2})").matcher(text);
            if(m.find() && r.name.isEmpty()) r.name=clean(m.group(1));
            Matcher s=Pattern.compile("APELLIDOS?\\s*[:.-]?\\s*([A-ZÁÉÍÓÚÑ]{2,}(?:\\s+[A-ZÁÉÍÓÚÑ]{2,}){1,3})").matcher(text);
            if(s.find() && r.surname.isEmpty()) r.surname=clean(s.group(1));
        }
        r.holder=(r.name+" "+r.surname).trim();
        int score=0;
        if(!r.holder.isEmpty()) score+=30;
        if(!r.dni.isEmpty()) score+=35;
        if(!r.birthDate.isEmpty()) score+=20;
        if(!r.address.isEmpty()) score+=10;
        if(!r.phone.isEmpty()||!r.email.isEmpty()) score+=5;
        r.confidence=Math.min(100,score);
        return r;
    }

    private static String normalize(String s){
        return s.toUpperCase(Locale.ROOT)
                .replace("APELLlDOS","APELLIDOS").replace("N0MBRE","NOMBRE")
                .replace("NACIMlENTO","NACIMIENTO").replace("NACIMlENT0","NACIMIENTO")
                .replace("DOMIC1LIO","DOMICILIO");
    }
    private static boolean starts(String s,String... labels){for(String l:labels)if(s.startsWith(l))return true;return false;}
    private static boolean contains(String s,String... labels){for(String l:labels)if(s.contains(l))return true;return false;}
    private static String valueOrNext(String line,String[] lines,int i,String... labels){
        String v=valueAfterAny(line,labels); if(!v.isEmpty()) return v;
        if(i+1<lines.length){String n=clean(lines[i+1]); if(!n.isEmpty()&&!isLabel(n)) return n;}
        return "";
    }
    private static String valueAfterAny(String line,String...labels){
        for(String l:labels){if(line.startsWith(l)){return line.substring(l.length()).replaceFirst("^[\\s:.-]+","").trim();}}
        return "";
    }
    private static boolean isLabel(String s){return starts(s,"NOMBRE","APELLIDOS","APELLIDO","SEXO","NACIONALIDAD","NACIMIENTO","DOMICILIO","DIRECCION","DIRECCIÓN","VALIDEZ","CADUCIDAD","EMISION","EMISIÓN","SOPORTE","NUMERO","NÚMERO","FIRMA");}
    private static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static String namePart(String s){return s.replace('<',' ').replaceAll("\\s+"," ").trim();}
    private static String findDni(String text){
        Matcher m=Pattern.compile("(?:DNI|NIF|NIE)\\s*[:.-]?\\s*((?:[0-9]\\s*){8}|[XYZ]\\s*(?:[0-9]\\s*){7})\\s*([A-Z])\\b").matcher(text);
        while(m.find()){String c=(m.group(1)+m.group(2)).replaceAll("\\s","");if(valid(c))return c;}
        m=Pattern.compile("(?<![A-Z0-9])((?:\\d\\s*){8})([A-Z])(?![A-Z0-9])").matcher(text);
        while(m.find()){String c=(m.group(1)+m.group(2)).replaceAll("\\s","");if(valid(c))return c;}
        m=Pattern.compile("(?<![A-Z0-9])([XYZ]\\s*(?:\\d\\s*){7})([A-Z])(?![A-Z0-9])").matcher(text);
        while(m.find()){String c=(m.group(1)+m.group(2)).replaceAll("\\s","");if(valid(c))return c;}
        return "";
    }
    private static String findBirthDate(String text){
        Matcher m=Pattern.compile("(?:NACIMIENTO|NACIMIENT0|NACIMlENTO)\\s*[:.-]?\\s*(\\d{2}\\s*[ /.-]\\s*\\d{2}\\s*[ /.-]\\s*\\d{4})").matcher(text);
        return m.find()?normalizeDate(m.group(1)):findFirstDateNear(text);
    }
    private static String findFirstDateNear(String text){
        Matcher m=Pattern.compile("\\b\\d{2}\\s*[ /.-]\\s*\\d{2}\\s*[ /.-]\\s*\\d{4}\\b").matcher(text);return m.find()?normalizeDate(m.group()):"";
    }
    private static String findLabeled(String text,String...labels){for(String line:text.split("\\R")){String v=valueAfterAny(clean(line),labels);if(!v.isEmpty())return v;}return"";}
    private static String findPhone(String text){Matcher m=Pattern.compile("(?:\\+34\\s*)?[6789](?:\\s*\\d){8}").matcher(text);return m.find()?m.group().replaceAll("\\s",""):"";}
    private static String findEmail(String text){Matcher m=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE).matcher(text);return m.find()?m.group():"";}
    private static boolean valid(String v){return validDni(v)||validNie(v);}
    private static boolean validDni(String v){if(v==null||!v.matches("\\d{8}[A-Z]"))return false;String l="TRWAGMYFPDXBNJZSQVHLCKE";return l.charAt(Integer.parseInt(v.substring(0,8))%23)==v.charAt(8);}
    private static boolean validNie(String v){if(v==null||!v.matches("[XYZ]\\d{7}[A-Z]"))return false;String n=(v.charAt(0)=='X'?"0":v.charAt(0)=='Y'?"1":"2")+v.substring(1,8);String l="TRWAGMYFPDXBNJZSQVHLCKE";return l.charAt(Integer.parseInt(n)%23)==v.charAt(8);}
    private static String normalizeDate(String s){return s.replaceAll("\\s+","").replace('-','/').replace('.','/');}
    private static String[] findMrz(String[] lines){String a="",b="";for(String raw:lines){String l=raw.toUpperCase(Locale.ROOT).replace(" ","");if(l.startsWith("IDESP")&&a.isEmpty())a=l.replaceAll("[^A-Z0-9<]","");else if(l.length()>=28&&l.contains("<<")&&a.isEmpty())a=l.replaceAll("[^A-Z0-9<]","");else if(!a.isEmpty()&&b.isEmpty()&&l.matches("[0-9A-Z<]{20,}"))b=l.replaceAll("[^A-Z0-9<]","");}return a.isEmpty()||b.isEmpty()?new String[0]:new String[]{a,b};}
    private static String mrzDate(String yymmdd){try{int yy=Integer.parseInt(yymmdd.substring(0,2));int year=yy<=30?2000+yy:1900+yy;return String.format(Locale.ROOT,"%02d/%02d/%04d",Integer.parseInt(yymmdd.substring(4,6)),Integer.parseInt(yymmdd.substring(2,4)),year);}catch(Exception e){return"";}}
}
