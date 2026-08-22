package com.rgapro1.ocaso.ocr;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Finds an existing client by DNI/NIE before a policy can create a new client. */
public final class ClientLinker {
    public int findClientIndex(JSONArray records, String identity) {
        String wanted = normalizeIdentity(identity);
        if (wanted.isEmpty()) return -1;
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) continue;
            String existing = record.optString("identityNumber",
                    record.optString("holderDni", ""));
            if (wanted.equals(normalizeIdentity(existing))) return i;
        }
        return -1;
    }

    public String normalizeIdentity(String value) {
        if (value == null) return "";
        return value.toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "")
                .trim();
    }
}
