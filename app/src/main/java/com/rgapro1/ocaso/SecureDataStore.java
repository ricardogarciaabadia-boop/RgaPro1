package com.rgapro1.ocaso;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts sensitive JSON data at rest with an AES-256-GCM key held by Android Keystore.
 * The plaintext is never persisted in the backing SharedPreferences file.
 */
public final class SecureDataStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "RgaProDataKey";
    private static final String PREFS = "rgapro_secure_data";
    private static final String VALUE = "encrypted_policies";
    private static final int GCM_TAG_BITS = 128;

    private final Context context;

    public SecureDataStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasData() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(VALUE);
    }

    public String read() {
        try {
            String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(VALUE, null);
            if (stored == null) return null;
            String[] parts = stored.split("\\.", 2);
            if (parts.length != 2) return null;
            byte[] iv = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] ciphertext = Base64.decode(parts[1], Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void write(String value) throws Exception {
        if (value == null) throw new IllegalArgumentException("value == null");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String stored = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(VALUE, stored)
                .apply();
    }

    /** Migrates the legacy plaintext JSON once, then removes it from the legacy preferences. */
    public boolean migrateLegacy(SharedPreferences legacyPrefs, String legacyKey) {
        if (legacyPrefs == null || legacyKey == null) return false;
        String legacy = legacyPrefs.getString(legacyKey, null);
        if (legacy == null) return hasData();
        try {
            if (!hasData()) write(legacy);
            legacyPrefs.edit().remove(legacyKey).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void clear() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        } catch (Exception ignored) {
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
