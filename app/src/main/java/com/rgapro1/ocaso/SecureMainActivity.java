package com.rgapro1.ocaso;

import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.Field;

/**
 * Production launcher that transparently upgrades the legacy MainActivity PIN storage
 * without duplicating the existing UI/business code.
 */
public class SecureMainActivity extends MainActivity {
    private static final String TAG = "RgaProSecurity";

    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        secureLegacyPreferences();
    }

    private void secureLegacyPreferences() {
        try {
            Field prefsField = null;
            for (Field field : MainActivity.class.getDeclaredFields()) {
                if (SharedPreferences.class.isAssignableFrom(field.getType())) {
                    prefsField = field;
                    break;
                }
            }
            if (prefsField == null) throw new NoSuchFieldException("SharedPreferences field not found");
            prefsField.setAccessible(true);
            SharedPreferences current = (SharedPreferences) prefsField.get(this);
            if (current == null) throw new IllegalStateException("Preferences not initialized");
            SecurePinStore store = new SecurePinStore(this);
            prefsField.set(this, new SecurePinPreferences(current, store));
        } catch (Exception e) {
            // Keep the app usable if a future refactor removes the legacy field.
            Log.e(TAG, "Could not install secure preferences facade", e);
        }
    }
}
