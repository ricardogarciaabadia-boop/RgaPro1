package com.rgapro1.ocaso.ocr;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Classifies OCR text before fields are mapped to the client/policy record. */
public final class DocumentTypeClassifier {
    public enum Type { DNI, NIE, POLICY, UNKNOWN }

    public static final class Result {
        public final Type type;
        public final int confidence;
        public final String identityNumber;
        public final String policyNumber;

        public Result(Type type, int confidence, String identityNumber, String policyNumber) {
            this.type = type;
            this.confidence = confidence;
            this.identityNumber = identityNumber;
            this.policyNumber = policyNumber;
        }
    }

    private static final Pattern DNI = Pattern.compile("\\b\\d{8}[A-Z]\\b");
    private static final Pattern NIE = Pattern.compile("\\b[XYZ]\\d{7}[A-Z]\\b");
    private static final Pattern CIF = Pattern.compile("\\b[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]\\b");
    private static final Pattern POLICY = Pattern.compile("(?i)\\b(?:P[ÓO]LIZA|POLIZA|N[ÚU]M(?:ERO)?\\s+DE\\s+P[ÓO]LIZA|N[ÚU]M(?:ERO)?\\s+P[ÓO]LIZA)\\b\\s*[:#Nº.-]*\\s*([A-Z0-9][A-Z0-9./_-]{4,})");

    private DocumentTypeClassifier() {}

    public static Result classify(String raw) {
        String text = normalize(raw);
        Matcher dni = DNI.matcher(text);
        Matcher nie = NIE.matcher(text);
        Matcher cif = CIF.matcher(text);
        Matcher policy = POLICY.matcher(text);

        boolean policyLabel = containsAny(text,
                "POLIZA", "Nº POLIZA", "NUMERO DE POLIZA", "NUM POLIZA",
                "FECHA DE EFECTO", "FECHA DE VENCIMIENTO", "PRIMA", "ASEGURADO",
                "TOMADOR", "COMPANIA ASEGURADORA", "GARANTIAS", "COBERTURAS");
        boolean dniLabel = containsAny(text,
                "DNI", "NIE", "APELLIDOS", "NOMBRE", "NACIONALIDAD",
                "LUGAR DE NACIMIENTO", "DOMICILIO", "NUM SOPORTE", "IDESP", "ESP");

        if (policyLabel && !dniLabel) {
            return new Result(Type.POLICY, policyNumberScore(text), "", policy.find() ? policy.group(1).trim() : "");
        }
        if (nie.find()) return new Result(Type.NIE, Math.min(100, 80 + (dniLabel ? 20 : 0)), nie.group(), "");
        if (dni.find()) return new Result(Type.DNI, Math.min(100, 80 + (dniLabel ? 20 : 0)), dni.group(), "");
        if (policyLabel) return new Result(Type.POLICY, policyNumberScore(text), "", policy.find() ? policy.group(1).trim() : "");
        if (cif.find() && !dniLabel) return new Result(Type.POLICY, 75, cif.group(), "");
        return new Result(Type.UNKNOWN, 0, "", "");
    }

    private static int policyNumberScore(String text) {
        int score = 55;
        if (containsAny(text, "POLIZA", "NUMERO DE POLIZA", "NUM POLIZA")) score += 20;
        if (containsAny(text, "ASEGURADO", "TOMADOR", "COBERTURAS", "PRIMA")) score += 10;
        return Math.min(100, score);
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase(Locale.ROOT)
                .replace('Ó', 'O').replace('Á', 'A').replace('É', 'E')
                .replace('Í', 'I').replace('Ú', 'U')
                .replaceAll("[\\r\\t]+", " ")
                .replaceAll("[ ]{2,}", " ");
    }
}
