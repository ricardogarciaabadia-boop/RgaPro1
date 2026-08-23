package com.rgapro1.ocaso;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative document classifier used before assigning OCR fields. */
public final class DocumentTypeClassifier {
    public enum Type { DNI_NIE, POLICY_DEATH, POLICY_LIFE, POLICY_HOME, POLICY_SAVINGS, POLICY_ACCIDENTS, POLICY_OTHER, UNKNOWN }
    private static final Pattern ID = Pattern.compile("(?:[0-9]{8}[A-Z]|[XYZ][0-9]{7}[A-Z])");
    private DocumentTypeClassifier() {}

    public static Type classify(String text) {
        String x = text == null ? "" : text.toUpperCase(Locale.ROOT);
        boolean policy = x.contains("PÓLIZA") || x.contains("POLIZA") || x.contains("TOMADOR DEL SEGURO");
        if (x.contains("DECESOS") || x.contains("RELACIÓN DE ASEGURADOS") || x.contains("RELACION DE ASEGURADOS")) return Type.POLICY_DEATH;
        if (x.contains("VIDA")) return Type.POLICY_LIFE;
        if (x.contains("HOGAR") || x.contains("MULTIRRIESGO HOGAR")) return Type.POLICY_HOME;
        if (x.contains("AHORRO") || x.contains("PLAN DE AHORRO")) return Type.POLICY_SAVINGS;
        if (x.contains("ACCIDENTES") || x.contains("ACCIDENTE")) return Type.POLICY_ACCIDENTS;
        if (policy) return Type.POLICY_OTHER;
        if (ID.matcher(x).find() && (x.contains("DNI") || x.contains("NIE") || x.contains("DOC. ID") || x.contains("DOCUMENTO NACIONAL"))) return Type.DNI_NIE;
        return Type.UNKNOWN;
    }

    public static String label(Type type) {
        switch (type) {
            case DNI_NIE: return "DNI/NIE";
            case POLICY_DEATH: return "Decesos";
            case POLICY_LIFE: return "Vida";
            case POLICY_HOME: return "Hogar";
            case POLICY_SAVINGS: return "Ahorro";
            case POLICY_ACCIDENTS: return "Accidentes";
            case POLICY_OTHER: return "Otros";
            default: return "Desconocido";
        }
    }
}
