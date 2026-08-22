package com.rgapro1.ocaso;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/** Structured insured person extracted from a policy. A person may hold other policies. */
public final class InsuredPerson {
    private final String fullName;
    private final String birthDate;
    private final String identityNumber;
    private final String capital;
    private final String accidentCapital;
    private final boolean holder;
    private final IdentityStatus identityStatus;

    public InsuredPerson(String fullName, String birthDate, String identityNumber, String capital, boolean holder) {
        this(fullName, birthDate, identityNumber, capital, "", holder, IdentityStatus.PRESENT);
    }

    public InsuredPerson(String fullName, String birthDate, String identityNumber, String capital, String accidentCapital, boolean holder) {
        this(fullName, birthDate, identityNumber, capital, accidentCapital, holder,
                identityNumber == null || identityNumber.trim().isEmpty()
                        ? IdentityStatus.MISSING_REVIEW : IdentityStatus.PRESENT);
    }

    public InsuredPerson(String fullName, String birthDate, String identityNumber, String capital,
                         String accidentCapital, boolean holder, IdentityStatus identityStatus) {
        this.fullName = clean(fullName);
        this.birthDate = clean(birthDate);
        this.identityNumber = normalizeId(identityNumber);
        this.capital = clean(capital);
        this.accidentCapital = clean(accidentCapital);
        this.holder = holder;
        this.identityStatus = identityStatus == null
                ? (this.identityNumber.isEmpty() ? IdentityStatus.MISSING_REVIEW : IdentityStatus.PRESENT)
                : identityStatus;
    }

    public String getFullName() { return fullName; }
    public String getBirthDate() { return birthDate; }
    public String getIdentityNumber() { return identityNumber; }
    public String getCapital() { return capital; }
    public String getAccidentCapital() { return accidentCapital; }
    public boolean isHolder() { return holder; }
    public IdentityStatus getIdentityStatus() { return identityStatus; }
    public boolean hasIdentityNumber() { return !identityNumber.isEmpty(); }

    /**
     * Applies the insurer rule used by the current death-policy workflow:
     * a person under 14 at the policy start may legitimately have no DNI/NIE
     * printed on the policy. This does not invent an identity number.
     */
    public InsuredPerson withMinorIdentityStatus(String policyStartDate) {
        if (hasIdentityNumber() || birthDate.isEmpty() || policyStartDate == null || policyStartDate.trim().isEmpty()) {
            return this;
        }
        if (isUnder14At(birthDate, policyStartDate)) {
            return new InsuredPerson(fullName, birthDate, identityNumber, capital, accidentCapital,
                    holder, IdentityStatus.OPTIONAL_FOR_MINOR);
        }
        return this;
    }

    public static boolean isUnder14At(String birthDate, String referenceDate) {
        LocalDate birth = parseDate(birthDate);
        LocalDate reference = parseDate(referenceDate);
        if (birth == null || reference == null || birth.isAfter(reference)) return false;
        return Period.between(birth, reference).getYears() < 14;
    }

    public static String normalizeId(String value) {
        if (value == null) return "";
        return value.toUpperCase().replaceAll("[^A-Z0-9]", "").trim();
    }

    private static String clean(String value) { return value == null ? "" : value.trim().replaceAll("\\s+", " "); }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        String x = value.trim();
        DateTimeFormatter[] formats = {
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy")
        };
        for (DateTimeFormatter format : formats) {
            try { return LocalDate.parse(x, format); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof InsuredPerson)) return false;
        InsuredPerson other = (InsuredPerson) o;
        if (!identityNumber.isEmpty() && !other.identityNumber.isEmpty()) {
            return identityNumber.equals(other.identityNumber);
        }
        return identityNumber.isEmpty() && other.identityNumber.isEmpty()
                && fullName.equalsIgnoreCase(other.fullName)
                && birthDate.equals(other.birthDate);
    }

    @Override public int hashCode() {
        if (!identityNumber.isEmpty()) return Objects.hash("ID", identityNumber);
        return Objects.hash("NAME_BIRTH", fullName.toUpperCase(), birthDate);
    }
}
