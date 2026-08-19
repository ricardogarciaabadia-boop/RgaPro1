package com.rgapro1.ocaso;

import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Production launcher that keeps the legacy MainActivity UI intact while routing
 * sensitive local state through Android Keystore-backed stores.
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
                    new SecurePinStore(getApplicationContext()),
                    new SecureDataStore(getApplicationContext())
            );
        }
        return secureLocalPreferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClientAutoLinker.start(this);
    }
}
