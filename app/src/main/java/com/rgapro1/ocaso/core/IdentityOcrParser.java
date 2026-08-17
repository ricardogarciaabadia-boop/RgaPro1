package com.rgapro1.ocaso.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses identity OCR text conservatively. It never invents missing values. */
public final class IdentityOcrParser {
    private static final Pattern DNI = Pattern.compile("\\b([0-9]{8}[A-Z])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NIE = Pattern.compile("\\b([XYZ][0-9]{7}[A-Z])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("\\b(0?[1-9]|[12][0-9]|3[01])[\\-/](0?[1-9]|1[0-2])[\\-/](19\\d{2}|20\\d{2})\\b");
    private static final Pattern CIF = Pattern.compile("\\b([ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J])\\b", Pattern.CASE_INSENSITIVE);

    private IdentityOcrParser() {}

    public static Result parse(String rawText) {
        String text = rawText == null ? "" : rawText.replace('\r', '\n');
        Result r = new Result();
        Matcher m = DNI.matcher(text); if (m.find()) { r.type = "DNI"; r.number = m.group(1).toUpperCase(Locale.ROOT); }
        if (r.number.isEmpty()) { m = NIE.matcher(text); if (m.find()) { r.type = "NIE"; r.number = m.group(1).toUpperCase(Locale.ROOT); } }
        if (r.number.isEmpty()) { m = CIF.matcher(text); if (m.find()) { r.type = "CIF"; r.number = m.group(1).toUpperCase(Locale.ROOT); } }
        m = DATE.matcher(text); if (m.find()) r.firstDate = m.group();
        r.mrz = findMrz(text);
        r.name = labeled(text, "NOMBRE", "NAME");
        r.surname = labeled(text, "APELLIDOS", "SURNAME");
        r.birthDate = labeled(text, "FECHA DE NACIMIENTO", "NACIMIENTO", "DATE OF BIRTH");
        if (r.birthDate.isEmpty()) r.birthDate = r.firstDate;
        r.nationality = labeled(text, "NACIONALIDAD", "NATIONALITY");
        r.sex = labeled(text, "SEXO", "SEX");
        r.expiry = labeled(text, "VALIDEZ", "FECHA DE VALIDEZ", "EXPIRY");
        r.confidence = score(r);
        return r;
    }

    private static String labeled(String text, String... labels) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String normalized = line.trim();
            for (String label : labels) {
                String upper = normalized.toUpperCase(Locale.ROOT);
                int p = upper.indexOf(label);
                if (p >= 0) {
                    String value = normalized.substring(p + label.length()).replaceFirst("^[\\s:.-]+", "").trim();
                    if (!value.isEmpty()) return value;
                }
            }
        }
        return "";
    }

    private static String findMrz(String text) {
        StringBuilder b = new StringBuilder();
        for (String line : text.split("\\n")) {
            String s = line.replace(" ", "").toUpperCase(Locale.ROOT);
            if (s.length() >= 25 && s.matches("[A-Z0-9<]+")) {
                int count = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '<') count++;
                if (count >= 2) { if (b.length() > 0) b.append('\n'); b.append(s); }
            }
        }
        return b.toString();
    }

    private static int score(Result r) {
        int score = 0;
        if (!r.number.isEmpty()) score += 35;
        if (!r.name.isEmpty()) score += 15;
        if (!r.surname.isEmpty()) score += 15;
        if (!r.birthDate.isEmpty()) score += 15;
        if (!r.mrz.isEmpty()) score += 20;
        return Math.min(score, 100);
    }

    public static final class Result {
        public String type = "";
        public String number = "";
        public String name = "";
        public String surname = "";
        public String birthDate = "";
        public String nationality = "";
        public String sex = "";
        public String expiry = "";
        public String firstDate = "";
        public String mrz = "";
        public int confidence;
    }
}
