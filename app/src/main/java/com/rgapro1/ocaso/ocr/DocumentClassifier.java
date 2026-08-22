package com.rgapro1.ocaso.ocr;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative classifier: policy evidence wins over identity-like numbers. */
public final class DocumentClassifier {
    private static final Pattern DNI = Pattern.compile("\\b\\d{8}[A-Z]\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NIE = Pattern.compile("\\b[XYZ]\\d{7}[A-Z]\\b", Pattern.CASE_INSENSITIVE);

    public DocumentType classify(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return DocumentType.UNKNOWN;
        String t = rawText.toUpperCase(Locale.ROOT);

        int policy = 0;
        String[] policyWords = {
                "POLIZA", "PÓLIZA", "ASEGURADO", "TOMADOR", "PRIMA",
                "COBERTURA", "COBERTURAS", "VENCIMIENTO", "FECHA DE EFECTO",
                "EFECTO", "RIESGO", "CONDICIONES GENERALES", "Nº POLIZA", "N POLIZA"
        };
        for (String word : policyWords) if (t.contains(word)) policy += 2;

        int identity = 0;
        if (DNI.matcher(t).find() || NIE.matcher(t).find()) identity += 4;
        String[] identityWords = {
                "DNI", "NIE", "NOMBRE", "APELLIDOS", "NACIONALIDAD",
                "FECHA DE NACIMIENTO", "LUGAR DE NACIMIENTO", "DOMICILIO",
                "IDESP", "SOPORTE", "VALIDEZ"
        };
        for (String word : identityWords) if (t.contains(word)) identity++;

        // A policy containing a DNI must remain a policy.
        if (policy >= 2 && policy >= identity) return DocumentType.POLICY;
        if (identity >= 4) return DocumentType.DNI_NIE;
        return policy >= 2 ? DocumentType.POLICY : DocumentType.UNKNOWN;
    }
}
