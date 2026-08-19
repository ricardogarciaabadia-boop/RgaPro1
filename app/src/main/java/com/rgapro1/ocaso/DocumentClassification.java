package com.rgapro1.ocaso;

/** Resultado de clasificación antes de extraer campos. */
public final class DocumentClassification {
    public final DocumentType type;
    public final int confidence;
    public final boolean validIdentityNumberFound;

    public DocumentClassification(DocumentType type, int confidence, boolean validIdentityNumberFound) {
        this.type = type;
        this.confidence = confidence;
        this.validIdentityNumberFound = validIdentityNumberFound;
    }

    public boolean isReliable() {
        return confidence >= 70 && type != DocumentType.UNKNOWN;
    }
}
