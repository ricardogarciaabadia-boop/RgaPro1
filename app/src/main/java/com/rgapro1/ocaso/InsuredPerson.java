package com.rgapro1.ocaso;

import java.util.Objects;

/** Structured insured person extracted from a policy. A person may hold other policies. */
public final class InsuredPerson {
    private final String fullName;
    private final String birthDate;
    private final String identityNumber;
    private final String capital;
    private final String accidentCapital;
    private final boolean holder;

    public InsuredPerson(String fullName, String birthDate, String identityNumber, String capital, boolean holder) {
        this(fullName, birthDate, identityNumber, capital, "", holder);
    }

    public InsuredPerson(String fullName, String birthDate, String identityNumber, String capital, String accidentCapital, boolean holder) {
        this.fullName = clean(fullName);
        this.birthDate = clean(birthDate);
        this.identityNumber = normalizeId(identityNumber);
        this.capital = clean(capital);
        this.accidentCapital = clean(accidentCapital);
        this.holder = holder;
    }

    public String getFullName() { return fullName; }
    public String getBirthDate() { return birthDate; }
    public String getIdentityNumber() { return identityNumber; }
    public String getCapital() { return capital; }
    public String getAccidentCapital() { return accidentCapital; }
    public boolean isHolder() { return holder; }

    public static String normalizeId(String value) {
        if (value == null) return "";
        return value.toUpperCase().replaceAll("[^A-Z0-9]", "").trim();
    }

    private static String clean(String value) { return value == null ? "" : value.trim().replaceAll("\\s+", " "); }

    @Override public boolean equals(Object o) {
        if (!(o instanceof InsuredPerson)) return false;
        InsuredPerson other = (InsuredPerson) o;
        if (!identityNumber.isEmpty() && !other.identityNumber.isEmpty()) return identityNumber.equals(other.identityNumber);
        return fullName.equalsIgnoreCase(other.fullName) && birthDate.equals(other.birthDate);
    }

    @Override public int hashCode() { return Objects.hash(identityNumber, fullName.toUpperCase(), birthDate); }
}
