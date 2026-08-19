package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrae únicamente los campos de interés de una póliza. */
public final class PolicyOcrParser {
    public static final class Result {
        public String policyNumber = "";
        public String policyType = "";
        public String expiryDate = "";
        public String insurer = "";
        public int confidence = 0;
    }

    private PolicyOcrParser() {}

    public static Result parse(String raw) {
        Result r = new Result();
        String text = normalize(raw);
        String[] lines = text.split("\\n");

        r.policyNumber = labeled(lines, "NUMERO DE POLIZA", "N DE POLIZA", "Nº POLIZA", "POLIZA Nº", "POLIZA N");
        if (r.policyNumber.isEmpty()) {
            Matcher m = Pattern.compile("\\b[A-Z0-9][A-Z0-9./-]{5,24}\\b").matcher(text);
            while (m.find()) {
                String candidate = m.group();
                if (candidate.matches(".*\\d.*") && !candidate.matches("\\d{8}[A-Z]")) {
                    r.policyNumber = candidate;
                    break;
                }
            }
        }

        r.expiryDate = dateAfterLabel(lines, "VENCIMIENTO", "FECHA DE VENCIMIENTO", "CADUCIDAD", "VALIDEZ", "RENOVACION");
        r.policyType = labeled(lines, "TIPO DE POLIZA", "TIPO DE SEGURO", "MODALIDAD", "RAMO");
        if (r.policyType.isEmpty()) r.policyType = inferType(text);

        if (text.contains("OCASO")) r.insurer = "OCASO";
        else if (text.contains("MAPFRE")) r.insurer = "MAPFRE";
        else if (text.contains("AXA")) r.insurer = "AXA";
        else if (text.contains("ALLIANZ")) r.insurer = "ALLIANZ";

        int score = 0;
        if (!r.policyNumber.isEmpty()) score += 35;
        if (!r.policyType.isEmpty()) score += 25;
        if (!r.expiryDate.isEmpty()) score += 30;
        if (!r.insurer.isEmpty()) score += 10;
        r.confidence = Math.min(100, score);
        return r;
    }

    private static String labeled(String[] lines, String... labels) {
        for (int i = 0; i < lines.length; i++) {
            String line = clean(lines[i]);
            for (String label : labels) {
                String l = label.toUpperCase(Locale.ROOT);
                int p = line.indexOf(l);
                if (p >= 0) {
                    String value = line.substring(p + l.length()).replaceFirst("^[ :.-]+", "").trim();
                    if (!value.isEmpty()) return value;
                    if (i + 1 < lines.length) return clean(lines[i + 1]);
                }
            }
        }
        return "";
    }

    private static String dateAfterLabel(String[] lines, String... labels) {
        for (String label : labels) {
            String value = labeled(lines, label);
            Matcher m = Pattern.compile("\\b\\d{1,2}[ /.-]\\d{1,2}[ /.-]\\d{4}\\b").matcher(value);
            if (m.find()) return normalizeDate(m.group());
        }
        return "";
    }

    private static String inferType(String text) {
        if (text.contains("DECESOS") || text.contains("ASISTENCIA EN DECESOS")) return "Decesos";
        if (text.contains("HOGAR") || text.contains("VIVIENDA")) return "Hogar";
        if (text.contains("AUTO") || text.contains("AUTOMOVIL") || text.contains("VEHICULO")) return "Auto";
        if (text.contains("VIDA")) return "Vida";
        if (text.contains("SALUD") || text.contains("ASISTENCIA SANITARIA")) return "Salud";
        if (text.contains("EMPRESA") || text.contains("PYME")) return "Empresa";
        return "";
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.replace('\r', '\n').toUpperCase(Locale.ROOT).replace("Nº", "N").replace("N°", "N");
    }

    private static String clean(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }
    private static String normalizeDate(String s) { return s.replace('-', '/').replace('.', '/'); }
}
