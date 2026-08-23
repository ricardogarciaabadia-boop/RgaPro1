package com.rgapro1.ocaso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts insured rows and policy-level capital data from death/funeral policy OCR. */
public final class DeathPolicyInsuredParser {
    private static final Pattern ID = Pattern.compile("(?<![A-Z0-9])(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])(?![A-Z0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW = Pattern.compile("^\\s*\\d{1,3}\\s+(.+)$");
    private static final Pattern DATE = Pattern.compile("\\b\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}\\b");
    private static final Pattern MONEY = Pattern.compile("(?:€|EUR)?\\s*(?:\\d{1,3}(?:[.\\s]\\d{3})+|\\d{4,})(?:,\\d{1,2})?\\s*(?:€|EUR)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY_NO_SYMBOL = Pattern.compile("\\b(?:\\d{1,3}(?:[.]\\d{3})+|\\d{4,})(?:,\\d{1,2})?\\b");

    private DeathPolicyInsuredParser() {}

    public static List<InsuredPerson> parse(String ocrText, String holderDni, String holderName) {
        return parse(ocrText, holderDni, holderName, "");
    }

    /**
     * Parses a death policy and applies the insurer's under-14 DNI rule when
     * the policy start date is available. Missing DNI is retained as data,
     * never fabricated from OCR or inferred from another person's identity.
     */
    public static List<InsuredPerson> parse(String ocrText, String holderDni, String holderName, String policyStartDate) {
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
            String id = idMatcher.find() ? InsuredPerson.normalizeId(idMatcher.group()) : "";
            Matcher rowMatcher = ROW.matcher(line);
            if (id.isEmpty() && !rowMatcher.matches()) continue;
            String beforeId = id.isEmpty() ? "" : normalize(line.substring(0, idMatcher.start()));
            String afterId = id.isEmpty() ? rowMatcher.group(1) : normalize(line.substring(idMatcher.end()));
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
        List<InsuredPerson> withCapitals = applyPolicyCapitals(result, upper);
        if (policyStartDate == null || policyStartDate.trim().isEmpty()) return withCapitals;
        List<InsuredPerson> classified = new ArrayList<>();
        for (InsuredPerson person : withCapitals) {
            classified.add(person.withMinorIdentityStatus(policyStartDate));
        }
        return classified;
    }

    private static List<InsuredPerson> applyPolicyCapitals(List<InsuredPerson> people, String upper) {
        if (people.isEmpty()) return people;
        String deathCapital = labelledCapital(upper, "TOTAL DECESOS", "DECESOS", "DECESOS NIVELADA");
        String accidentCapital = labelledCapital(upper, "MUERTE POR ACCIDENTE", "MUERTE CIRCULACIÓN", "MUERTE CIRCULACION");
        List<InsuredPerson> out = new ArrayList<>();
        for (int i = 0; i < people.size(); i++) {
            InsuredPerson p = people.get(i);
            String death = p.getCapital().isEmpty() ? deathCapital : p.getCapital();
            String accident = p.getAccidentCapital().isEmpty() && i == 0 ? accidentCapital : p.getAccidentCapital();
            out.add(new InsuredPerson(p.getFullName(), p.getBirthDate(), p.getIdentityNumber(), death, accident,
                    p.isHolder(), p.getIdentityStatus()));
        }
        return out;
    }

    private static String labelledCapital(String text, String... labels) {
        for (String label : labels) {
            Pattern p = Pattern.compile(Pattern.quote(label) + "[^\\n]{0,100}", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                String line = m.group();
                Matcher money = MONEY.matcher(line); if (money.find()) return money.group().trim();
                Matcher bare = MONEY_NO_SYMBOL.matcher(line); if (bare.find()) return bare.group().trim();
            }
        }
        return "";
    }

    private static int indexOfInsuredSection(String upper) {
        String[] markers = {"RELACIÓN DE ASEGURADOS", "RELACION DE ASEGURADOS", "RELACIÓN ASEGURADOS", "ASEGURADOS"};
        int best = Integer.MAX_VALUE;
        for (String marker : markers) { int p = upper.indexOf(marker); if (p >= 0 && p < best) best = p; }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static boolean looksLikeHeader(String line) {
        String x = line.toUpperCase(Locale.ROOT);
        return (x.contains("NOMBRE") && x.contains("DNI")) || (x.contains("NIF") && x.contains("CAPITAL"));
    }

    private static String extractName(String value) {
        String x = value.replaceAll("(?i)^(ASEGURADO|ASEGURADA|TITULAR|TOMADOR)\\s*[:.-]?\\s*", "").trim();
        x = x.replaceAll("(?i)\\b(?:FECHA|NACIMIENTO|CAPITAL|CAPITALES|DNI|NIF)\\b.*$", "").trim();
        x = x.replaceFirst("^\\d{1,3}\\s+", "").trim();
        x = x.replaceFirst("^(?:[XYZ]?[0-9]{7,8}[A-Z])\\s+", "").trim();
        if (x.length() < 5 || !x.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*")) return "";
        return x.replaceAll("\\s+", " ");
    }

    private static String extractDate(String value) { Matcher m = DATE.matcher(value); return m.find() ? m.group() : ""; }
    private static String extractCapital(String value) { Matcher money=MONEY.matcher(value); if(money.find())return money.group().trim(); Matcher bare=MONEY_NO_SYMBOL.matcher(value); return bare.find()?bare.group().trim():""; }
    private static String normalize(String value) { return value == null ? "" : value.trim().replaceAll("[ \\t]+", " "); }
    private static boolean sameId(String a, String b) { return !a.isEmpty() && a.equals(InsuredPerson.normalizeId(b)); }
    private static boolean sameName(String a, String b) { return b != null && !b.trim().isEmpty() && a.equalsIgnoreCase(b.trim()); }
    private static void addUnique(List<InsuredPerson> list, InsuredPerson person) { if (!list.contains(person)) list.add(person); }
}
