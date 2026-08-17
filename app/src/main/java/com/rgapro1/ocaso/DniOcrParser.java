package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser específico para DNI/NIE español. Combina texto frontal y MRZ posterior. */
public final class DniOcrParser {
    public static final class Result {
        public String holder="", surname="", name="", dni="", birthDate="", nationality="", sex="";
        public String address="", birthPlace="", parents="", supportNumber="", issueDate="", validityDate="";
        public String mrz="";
        public int confidence=0;
    }

    private DniOcrParser() {}

    public static Result parse(String raw) {
        Result r = new Result();
        String text = raw == null ? "" : raw.replace('\r','\n');
        String upper = normalizeOcr(text);
        String[] lines = upper.split("\\n");

        r.surname = labeled(lines, "APELLIDOS");
        r.name = labeled(lines, "NOMBRE");
        r.nationality = labeled(lines, "NACIONALIDAD");
        r.sex = labeled(lines, "SEXO");
        r.address = labeledOrNext(lines, "DOMICILIO");
        r.birthPlace = labeledOrNext(lines, "LUGAR DE NACIMIENTO");
        r.parents = labeledOrNext(lines, "HIJO/A DE", "HIJO DE");
        r.supportNumber = labeledOrNext(lines, "NUM SOPORTE", "Nº SOPORTE", "N° SOPORTE", "NUMERO SOPORTE");
        r.issueDate = dateAfterLabel(lines, "EMISION", "EMISIÓN", "FECHA DE EMISION", "FECHA DE EMISIÓN");
        r.validityDate = dateAfterLabel(lines, "VALIDEZ", "CADUCIDAD", "FECHA DE CADUCIDAD");

        // La fecha de nacimiento puede aparecer con separadores distintos o sin etiqueta.
        Matcher birth = Pattern.compile("(?:NACIMIENTO|NAC)[^0-9]{0,12}(\\d{2}[ /.-]\\d{2}[ /.-]\\d{4})").matcher(upper);
        if (birth.find()) r.birthDate = normalizeDate(birth.group(1));
        if (r.birthDate.isEmpty()) {
            Matcher allDates = Pattern.compile("\\b(\\d{2})[ /.-](\\d{2})[ /.-](\\d{4})\\b").matcher(upper);
            while (allDates.find()) {
                String candidate = normalizeDate(allDates.group());
                if (!candidate.equals(r.issueDate) && !candidate.equals(r.validityDate)) {
                    r.birthDate = candidate;
                    break;
                }
            }
        }

        Matcher id = Pattern.compile("(?<![0-9])[0-9]{8}[A-Z](?![A-Z0-9])").matcher(upper);
        if (id.find()) r.dni = id.group();
        if (r.dni.isEmpty()) {
            Matcher nie = Pattern.compile("(?<![A-Z0-9])[XYZ][0-9]{7}[A-Z](?![A-Z0-9])").matcher(upper);
            if (nie.find()) r.dni = nie.group();
        }

        StringBuilder mrzLines = new StringBuilder();
        for (String line : lines) {
            String compact = compactMrz(line);
            if (compact.startsWith("IDESP") || compact.contains("IDESP") || compact.contains("<<")) {
                mrzLines.append(compact).append('\n');
            }
        }
        String mrz = mrzLines.toString();
        r.mrz = mrz.trim();

        if (!mrz.isEmpty()) {
            Matcher mid = Pattern.compile("IDESP(?:C)?(?:ID)?([0-9]{8}[A-Z])").matcher(mrz);
            if (mid.find()) r.dni = mid.group(1);
            if (r.dni.isEmpty()) {
                Matcher anyId = Pattern.compile("(?<![0-9])([0-9]{8}[A-Z])(?![A-Z0-9])").matcher(mrz);
                if (anyId.find()) r.dni = anyId.group(1);
            }

            Matcher names = Pattern.compile("([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)<<([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)").matcher(mrz);
            if (names.find()) {
                String s = names.group(1).replace('<',' ').trim().replaceAll("\\s+"," ");
                String n = names.group(2).replace('<',' ').trim().replaceAll("\\s+"," ");
                if (!s.isEmpty()) r.surname = s;
                if (!n.isEmpty()) r.name = n;
            }

            Matcher datesMrz = Pattern.compile("(\\d{6})\\d([MF])(\\d{6})\\d").matcher(mrz.replace("<", ""));
            if (datesMrz.find()) {
                r.birthDate = mrzDate(datesMrz.group(1));
                r.sex = datesMrz.group(2);
                r.validityDate = mrzDate(datesMrz.group(3));
            } else {
                Matcher loose = Pattern.compile("(\\d{6})\\d?([MF])(\\d{6})").matcher(mrz.replace("<", ""));
                if (loose.find()) {
                    r.birthDate = mrzDate(loose.group(1));
                    r.sex = loose.group(2);
                    r.validityDate = mrzDate(loose.group(3));
                }
            }
            if (r.nationality.isEmpty() && mrz.contains("ESP")) r.nationality = "ESP";
        }

        // Si el frontal entrega nombre/apellidos, el titular se forma de manera consistente.
        if (!r.name.isEmpty() || !r.surname.isEmpty()) r.holder = (r.name + " " + r.surname).trim();
        if (r.holder.isEmpty()) {
            Matcher fallback = Pattern.compile("\\b([A-ZÁÉÍÓÚÑ]{3,})\\s+([A-ZÁÉÍÓÚÑ]{3,})\\s+([A-ZÁÉÍÓÚÑ]{3,})\\b").matcher(upper);
            if (fallback.find()) {
                r.surname = fallback.group(1) + " " + fallback.group(2);
                r.name = fallback.group(3);
                r.holder = r.name + " " + r.surname;
            }
        }

        int total=0;
        if(!r.dni.isEmpty()) total+=25;
        if(!r.name.isEmpty()) total+=20;
        if(!r.surname.isEmpty()) total+=20;
        if(!r.birthDate.isEmpty()) total+=15;
        if(!r.validityDate.isEmpty()) total+=10;
        if(!r.nationality.isEmpty()) total+=5;
        if(!r.sex.isEmpty()) total+=5;
        r.confidence=total;
        return r;
    }

    private static String normalizeOcr(String s) {
        return s.toUpperCase(Locale.ROOT)
                .replace('0','O')
                .replace("N0MBRE", "NOMBRE")
                .replace("APELLlDOS", "APELLIDOS")
                .replace("NACIMlENTO", "NACIMIENTO")
                .replace("VALlDEZ", "VALIDEZ")
                .replace("EMlSION", "EMISION");
    }

    private static String labeled(String[] lines, String label) { return labeledOrNext(lines, label); }

    private static String labeledOrNext(String[] lines, String... labels) {
        for (int i=0;i<lines.length;i++) {
            String line=clean(lines[i]);
            for(String label:labels) {
                String lab=label.toUpperCase(Locale.ROOT);
                int p=line.indexOf(lab);
                if(p>=0) {
                    String v=line.substring(p+lab.length()).replaceFirst("^[ :.-]+","").trim();
                    if(!v.isEmpty()) return v;
                    if(i+1<lines.length) return clean(lines[i+1]);
                }
            }
        }
        return "";
    }

    private static String dateAfterLabel(String[] lines,String... labels){
        for(String l:labels){
            String v=labeledOrNext(lines,l);
            Matcher m=Pattern.compile("\\d{2}[ /.-]\\d{2}[ /.-]\\d{4}").matcher(v);
            if(m.find()) return normalizeDate(m.group());
        }
        return "";
    }

    private static String compactMrz(String s){
        String x=s.toUpperCase(Locale.ROOT).replace(" ","").replace("–","-");
        x=x.replace("ID ESP","IDESP").replace("IDESP ","IDESP").replace("C I D","CID");
        return x.replaceAll("[^A-Z0-9<]","");
    }

    private static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
    private static String normalizeDate(String s){return s.replace('-','/').replace('.','/');}
    private static String mrzDate(String yyMMdd){
        try{int yy=Integer.parseInt(yyMMdd.substring(0,2));int year=yy<=30?2000+yy:1900+yy;return String.format(Locale.ROOT,"%02d/%02d/%04d",Integer.parseInt(yyMMdd.substring(4,6)),Integer.parseInt(yyMMdd.substring(2,4)),year);}catch(Exception e){return "";}
    }
}
