package com.rgapro1.ocaso;

import android.content.SharedPreferences;

/**
 * Production launcher that keeps the legacy MainActivity UI intact while
 * transparently routing its local PIN through SecurePinStore.
 */
public class SecureMainActivity extends MainActivity {
    private SharedPreferences secureLocalPreferences;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (!"rgapro_local".equals(name)) {
            return super.getSharedPreferences(name, mode);
        }
        if (secureLocalPreferences == null) {
            SharedPreferences delegate = super.getSharedPreferences(name, mode);
            secureLocalPreferences = new SecurePinPreferences(
                    delegate,
                    new SecurePinStore(getApplicationContext())
            );
        }
        return secureLocalPreferences;
    }
}
