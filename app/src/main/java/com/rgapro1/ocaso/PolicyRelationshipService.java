package com.rgapro1.ocaso;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/** Applies policy/person relationships without creating clients implicitly. */
public final class PolicyRelationshipService {
    private PolicyRelationshipService() {}

    public static void applyPolicy(JSONObject policyRecord, String policyNumber, String policyType,
                                   List<InsuredPerson> insuredPeople) throws Exception {
        if (policyRecord == null) return;
        JSONArray insureds = new JSONArray();
        if (insuredPeople != null) {
            for (InsuredPerson person : insuredPeople) {
                JSONObject row = new JSONObject();
                row.put("name", person.getFullName());
                row.put("birthDate", person.getBirthDate());
                row.put("identityNumber", person.getIdentityNumber());
                row.put("capital", person.getCapital());
                row.put("accidentCapital", person.getAccidentCapital());
                row.put("holder", person.isHolder());
                insureds.put(row);
            }
        }
        policyRecord.put("insureds", insureds);
        if (!blank(policyNumber)) policyRecord.put("number", policyNumber.trim());
        if (!blank(policyType)) policyRecord.put("type", policyType.trim());
    }

    /** Links only to an existing client identified by DNI/NIE; never creates one. */
    public static boolean linkExistingClient(JSONArray clients, String identityNumber,
                                             String policyNumber, String policyType,
                                             String role, String capital) throws Exception {
        String id = InsuredPerson.normalizeId(identityNumber);
        if (id.isEmpty() || clients == null) return false;
        for (int i = 0; i < clients.length(); i++) {
            JSONObject client = clients.optJSONObject(i);
            if (client == null || !sameIdentity(client, id)) continue;
            JSONArray links = client.optJSONArray("policyRelationships");
            if (links == null) links = new JSONArray();
            JSONObject link = findPolicy(links, policyNumber);
            if (link == null) {
                link = new JSONObject();
                link.put("number", policyNumber == null ? "" : policyNumber.trim());
                link.put("type", policyType == null ? "" : policyType.trim());
                link.put("roles", new JSONArray());
                links.put(link);
            }
            JSONArray roles = link.optJSONArray("roles");
            if (roles == null) roles = new JSONArray();
            addUnique(roles, role);
            link.put("roles", roles);
            if (!blank(capital)) link.put("capital", capital.trim());
            client.put("policyRelationships", links);
            client.put("updatedAt", System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public static boolean sameIdentity(JSONObject client, String normalizedIdentity) {
        if (client == null || normalizedIdentity == null || normalizedIdentity.isEmpty()) return false;
        String a = InsuredPerson.normalizeId(client.optString("identityNumber", ""));
        String b = InsuredPerson.normalizeId(client.optString("holderDni", ""));
        return normalizedIdentity.equals(a) || normalizedIdentity.equals(b);
    }

    private static JSONObject findPolicy(JSONArray links, String number) {
        String target = normalizePolicyNumber(number);
        for (int i = 0; i < links.length(); i++) {
            JSONObject link = links.optJSONObject(i);
            if (link != null && target.equals(normalizePolicyNumber(link.optString("number", "")))) return link;
        }
        return null;
    }

    private static void addUnique(JSONArray values, String value) throws Exception {
        if (blank(value)) return;
        for (int i = 0; i < values.length(); i++) if (value.equalsIgnoreCase(values.optString(i, ""))) return;
        values.put(value);
    }

    private static String normalizePolicyNumber(String value) {
        if (value == null) return "";
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
