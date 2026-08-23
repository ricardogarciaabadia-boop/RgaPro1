package com.rgapro1.ocaso.domain.model;

/** Domain model for a locally managed policy/client record. */
public final class Policy {
    private final String holder;
    private final String type;
    private final String number;
    private final String identityNumber;
    private final String expiry;
    private final String ocrText;
    private final long updatedAt;

    public Policy(String holder, String type, String number, String identityNumber,
                  String expiry, String ocrText, long updatedAt) {
        this.holder = holder == null ? "" : holder;
        this.type = type == null ? "" : type;
        this.number = number == null ? "" : number;
        this.identityNumber = identityNumber == null ? "" : identityNumber;
        this.expiry = expiry == null ? "" : expiry;
        this.ocrText = ocrText == null ? "" : ocrText;
        this.updatedAt = updatedAt;
    }

    public String getHolder() { return holder; }
    public String getType() { return type; }
    public String getNumber() { return number; }
    public String getIdentityNumber() { return identityNumber; }
    public String getExpiry() { return expiry; }
    public String getOcrText() { return ocrText; }
    public long getUpdatedAt() { return updatedAt; }
}
