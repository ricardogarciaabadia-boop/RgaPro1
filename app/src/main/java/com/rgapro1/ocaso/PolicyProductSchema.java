package com.rgapro1.ocaso;

import java.util.*;

/** Canonical schema: classify product before persisting OCR fields. */
public final class PolicyProductSchema {
    private PolicyProductSchema() {}

    public enum ProductType { VIDA, DECESOS, AHORRO, HOGAR, ACCIDENTES, DESCONOCIDO }

    public static final class Common {
        public String policyNumber;
        public String holderName;
        public String holderDni;
        public String issueDate;
        public String effectiveDate;
        public String expiryDate;
        public String renewalDate;
    }

    public static final class Insured {
        public String name;
        public String birthDate;
        public String dni;
        public String capital;
    }

    public static ProductType classify(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsAny(t, "decesos", "asistencia familiar", "servicio funerario")) return ProductType.DECESOS;
        if (containsAny(t, "seguro de vida", "vida riesgo", "capital por fallecimiento")) return ProductType.VIDA;
        if (containsAny(t, "ahorro", "aportación periódica", "aportacion periodica")) return ProductType.AHORRO;
        if (containsAny(t, "hogar", "continente", "contenido")) return ProductType.HOGAR;
        if (containsAny(t, "accidentes", "accidente personal")) return ProductType.ACCIDENTES;
        return ProductType.DESCONOCIDO;
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    /** Only the common fields belong to every product. Product-specific parsers add their own data. */
    public static Set<String> allowedCommonFields() {
        return new LinkedHashSet<>(Arrays.asList(
                "policyNumber", "holderName", "holderDni", "issueDate",
                "effectiveDate", "expiryDate", "renewalDate"));
    }
}
