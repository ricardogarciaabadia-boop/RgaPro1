package com.rgapro1.ocaso;

/** Relationship between a person and a policy. A person can be linked to many policies. */
public final class PolicyPersonRelationship {
    public static final String ROLE_HOLDER = "TOMADOR";
    public static final String ROLE_INSURED = "ASEGURADO";

    private final String personIdentityNumber;
    private final String policyNumber;
    private final String role;
    private final String capital;

    public PolicyPersonRelationship(String personIdentityNumber, String policyNumber, String role, String capital) {
        this.personIdentityNumber = InsuredPerson.normalizeId(personIdentityNumber);
        this.policyNumber = policyNumber == null ? "" : policyNumber.trim();
        this.role = role == null ? ROLE_INSURED : role.trim().toUpperCase();
        this.capital = capital == null ? "" : capital.trim();
    }

    public String getPersonIdentityNumber() { return personIdentityNumber; }
    public String getPolicyNumber() { return policyNumber; }
    public String getRole() { return role; }
    public String getCapital() { return capital; }
}
