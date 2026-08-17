package com.rgapro1.ocaso;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Secure local storage for the application PIN using Android Keystore + AES/GCM. */
public final class SecurePinStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "RgaProPinKey";
    private static final String PREFS = "rgapro_secure_pin";
    private static final String VALUE = "encrypted_pin";
    private static final int GCM_TAG_BITS = 128;

    private final Context context;

    public SecurePinStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasPin() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(VALUE);
    }

    public void setPin(String pin) throws Exception {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must contain exactly 6 digits");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
        String value = Base64.getEncoder().encodeToString(cipher.getIV()) + "." +
                Base64.getEncoder().encodeToString(encrypted);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(VALUE, value).apply();
    }

    public boolean verifyPin(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) return false;
        try {
            String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(VALUE, null);
            if (value == null) return false;
            String[] parts = value.split("\\.", 2);
            if (parts.length != 2) return false;
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            byte[] candidate = pin.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(plain, candidate);
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
