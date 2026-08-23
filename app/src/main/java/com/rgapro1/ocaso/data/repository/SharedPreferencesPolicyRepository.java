package com.rgapro1.ocaso.data.repository;

import android.content.SharedPreferences;
import com.rgapro1.ocaso.domain.model.Policy;
import com.rgapro1.ocaso.domain.repository.PolicyRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Transitional local implementation. It isolates the current JSON/SharedPreferences storage
 * behind the domain repository contract so the Activity does not need to own persistence forever.
 */
public final class SharedPreferencesPolicyRepository implements PolicyRepository {
    private static final String KEY = "policies";
    private final SharedPreferences preferences;

    public SharedPreferencesPolicyRepository(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public List<Policy> getAll() {
        List<Policy> result = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(preferences.getString(KEY, "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                result.add(new Policy(
                        item.optString("holder"),
                        item.optString("type"),
                        item.optString("number"),
                        item.optString("identityNumber"),
                        item.optString("expiry", item.optString("validityDate")),
                        item.optString("ocrText"),
                        item.optLong("updatedAt", item.optLong("createdAt", 0L))));
            }
        } catch (Exception ignored) {
            // Corrupt local data must not crash the UI; callers receive an empty snapshot.
        }
        return result;
    }

    @Override
    public void save(Policy policy) {
        try {
            JSONArray data = new JSONArray(preferences.getString(KEY, "[]"));
            JSONObject item = new JSONObject();
            item.put("holder", policy.getHolder());
            item.put("type", policy.getType());
            item.put("number", policy.getNumber());
            item.put("identityNumber", policy.getIdentityNumber());
            item.put("expiry", policy.getExpiry());
            item.put("ocrText", policy.getOcrText());
            item.put("updatedAt", policy.getUpdatedAt());
            data.put(item);
            preferences.edit().putString(KEY, data.toString()).apply();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist policy", e);
        }
    }

    @Override
    public void deleteLast() {
        try {
            JSONArray data = new JSONArray(preferences.getString(KEY, "[]"));
            if (data.length() == 0) return;
            data.remove(data.length() - 1);
            preferences.edit().putString(KEY, data.toString()).apply();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to remove last policy", e);
        }
    }
}
