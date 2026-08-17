package com.rgapro1.ocaso.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Stores only a salted PBKDF2 verifier, never the clear PIN. */
public final class SecurePinStore {
    private static final String PREF = "rgapro_security";
    private static final String SALT = "pin_salt";
    private static final String HASH = "pin_hash";
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private final SharedPreferences prefs;

    public SecurePinStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void setPin(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) throw new IllegalArgumentException("PIN must contain 6 digits");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(pin, salt);
        prefs.edit()
                .putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .apply();
    }

    public boolean verify(String pin) {
        try {
            String salt64 = prefs.getString(SALT, null);
            String hash64 = prefs.getString(HASH, null);
            if (salt64 == null || hash64 == null) return false;
            byte[] actual = derive(pin, Base64.decode(salt64, Base64.NO_WRAP));
            byte[] expected = Base64.decode(hash64, Base64.NO_WRAP);
            if (actual.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isConfigured() { return prefs.contains(SALT) && prefs.contains(HASH); }

    private byte[] derive(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive PIN verifier", e);
        }
    }
}
