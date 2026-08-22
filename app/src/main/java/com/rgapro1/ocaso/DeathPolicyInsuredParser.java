package com.rgapro1.ocaso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the insured table of a death/funeral policy without confusing the policy holder
 * with the policy itself. The parser is deliberately conservative: it only emits a person
 * when it can identify a DNI/NIE or a clearly labelled insured row.
 */
public final class DeathPolicyInsuredParser {
    private static final Pattern ID = Pattern.compile("(?<![A-Z0-9])(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])(?![A-Z0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("\\b\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}\\b");
    private static final Pattern MONEY = Pattern.compile("(?:€|EUR)?\\s*\\d{1,3}(?:[.\\s]\\d{3})*(?:,\\d{1,2})?\\s*(?:€|EUR)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPITAL_LABEL = Pattern.compile("(?:CAPITAL(?:ES)?|SUMA ASEGURADA|CAPITAL ASEGURADO)\\s*[:.]?\\s*(.{1,40})", Pattern.CASE_INSENSITIVE);

    private DeathPolicyInsuredParser() {}

    public static List<InsuredPerson> parse(String ocrText, String holderDni, String holderName) {
        if (ocrText == null || ocrText.trim().isEmpty()) return Collections.emptyList();
        String text = ocrText.replace('\r', '\n');
        String upper = text.toUpperCase(Locale.ROOT);
        int start = indexOfInsuredSection(upper);
        if (start < 0) return Collections.emptyList();
        String section = text.substring(start);
        String[] lines = section.split("\\n+");
        List<InsuredPerson> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = normalize(lines[i]);
            if (line.isEmpty() || looksLikeHeader(line)) continue;
            Matcher idMatcher = ID.matcher(line);
            if (!idMatcher.find()) continue;
            String id = InsuredPerson.normalizeId(idMatcher.group());
            String beforeId = normalize(line.substring(0, idMatcher.start()));
            String afterId = normalize(line.substring(idMatcher.end()));
            String name = extractName(beforeId);
            if (name.isEmpty()) name = extractName(afterId);
            String birthDate = extractDate(line);
            String capital = extractCapital(line);
            if (capital.isEmpty() && i + 1 < lines.length) capital = extractCapital(normalize(lines[i + 1]));
            if (name.isEmpty()) name = holderName == null ? "" : holderName.trim();
            if (name.isEmpty()) continue;
            boolean holder = sameId(id, holderDni) || sameName(name, holderName);
            addUnique(result, new InsuredPerson(name, birthDate, id, capital, holder));
        }
        return result;
    }

    private static int indexOfInsuredSection(String upper) {
        String[] markers = {"RELACIÓN DE ASEGURADOS", "RELACION DE ASEGURADOS", "RELACIÓN ASEGURADOS", "ASEGURADOS"};
        int best = Integer.MAX_VALUE;
        for (String marker : markers) {
            int p = upper.indexOf(marker);
            if (p >= 0 && p < best) best = p;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static boolean looksLikeHeader(String line) {
        String x = line.toUpperCase(Locale.ROOT);
        return x.contains("NOMBRE") && x.contains("DNI") || x.contains("NIF") && x.contains("CAPITAL");
    }

    private static String extractName(String value) {
        String x = value.replaceAll("(?i)^(ASEGURADO|ASEGURADA|TITULAR|TOMADOR)\\s*[:.-]?\\s*", "").trim();
        x = x.replaceAll("(?i)\\b(?:FECHA|NACIMIENTO|CAPITAL|CAPITALES|DNI|NIF)\\b.*$", "").trim();
        if (x.length() < 5 || !x.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*")) return "";
        return x.replaceAll("\\s+", " ");
    }

    private static String extractDate(String value) {
        Matcher m = DATE.matcher(value);
        return m.find() ? m.group() : "";
    }

    private static String extractCapital(String value) {
        Matcher label = CAPITAL_LABEL.matcher(value);
        if (label.find()) {
            Matcher money = MONEY.matcher(label.group(1));
            if (money.find()) return money.group().trim();
        }
        Matcher money = MONEY.matcher(value);
        return money.find() ? money.group().trim() : "";
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().replaceAll("[ \\t]+", " "); }
    private static boolean sameId(String a, String b) { return !a.isEmpty() && a.equals(InsuredPerson.normalizeId(b)); }
    private static boolean sameName(String a, String b) { return b != null && !b.trim().isEmpty() && a.equalsIgnoreCase(b.trim()); }
    private static void addUnique(List<InsuredPerson> list, InsuredPerson person) { if (!list.contains(person)) list.add(person); }
}
