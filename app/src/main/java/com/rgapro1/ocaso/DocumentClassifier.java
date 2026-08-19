package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Clasifica el documento antes de decidir qué campos se pueden extraer. */
public final class DocumentClassifier {
    private static final Pattern DNI = Pattern.compile("(?<![A-Z0-9])(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])(?![A-Z0-9])");

    private DocumentClassifier() {}

    public static DocumentClassification classify(String rawText) {
        String text = normalize(rawText);
        int dniScore = 0;
        int policyScore = 0;
        boolean validIdentity = false;

        Matcher id = DNI.matcher(text);
        while (id.find()) {
            String candidate = id.group();
            if (isValidDni(candidate) || isValidNie(candidate)) {
                validIdentity = true;
                dniScore += 55;
                break;
            }
            dniScore += 15;
        }

        String[] dniTerms = {"DNI", "NIE", "APELLIDOS", "NOMBRE", "FECHA DE NACIMIENTO", "NACIONALIDAD", "IDESP"};
        for (String term : dniTerms) if (text.contains(term)) dniScore += 10;

        String[] policyTerms = {"POLIZA", "PÓLIZA", "NUMERO DE POLIZA", "Nº POLIZA", "N DE POLIZA",
                "VENCIMIENTO", "CADUCIDAD", "TOMADOR", "ASEGURADO", "PRIMA", "COBERTURA", "SEGURO", "CONDICIONES"};
        for (String term : policyTerms) if (text.contains(term)) policyScore += 12;

        if (text.contains("OCASO") || text.contains("MAPFRE") || text.contains("AXA") || text.contains("ALLIANZ")) {
            policyScore += 18;
        }

        if (validIdentity && dniScore >= policyScore) {
            return new DocumentClassification(DocumentType.DNI_NIE, Math.min(100, dniScore), true);
        }
        if (policyScore >= 45 && policyScore > dniScore) {
            return new DocumentClassification(DocumentType.POLICY, Math.min(100, policyScore), false);
        }
        if (dniScore >= 55 && dniScore > policyScore) {
            return new DocumentClassification(DocumentType.DNI_NIE, Math.min(100, dniScore), validIdentity);
        }
        return new DocumentClassification(DocumentType.UNKNOWN, Math.min(100, Math.max(dniScore, policyScore)), validIdentity);
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase(Locale.ROOT)
                .replace("Nº", "N")
                .replace("N°", "N")
                .replace("POLIZA", "POLIZA")
                .replaceAll("\\s+", " ");
    }

    private static boolean isValidDni(String value) {
        if (value == null || !value.matches("\\d{8}[A-Z]")) return false;
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        try { return letters.charAt(Integer.parseInt(value.substring(0, 8)) % 23) == value.charAt(8); }
        catch (Exception e) { return false; }
    }

    private static boolean isValidNie(String value) {
        if (value == null || !value.matches("[XYZ]\\d{7}[A-Z]")) return false;
        String numeric = (value.charAt(0) == 'X' ? "0" : value.charAt(0) == 'Y' ? "1" : "2") + value.substring(1, 8);
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        try { return letters.charAt(Integer.parseInt(numeric) % 23) == value.charAt(8); }
        catch (Exception e) { return false; }
    }
}
