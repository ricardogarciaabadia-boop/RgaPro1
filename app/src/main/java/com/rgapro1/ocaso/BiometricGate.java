package com.rgapro1.ocaso;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/** Small reusable biometric gate for login/unlock. */
public final class BiometricGate {
    private BiometricGate() {}

    public static boolean canUse(Activity activity) {
        if (!(activity instanceof FragmentActivity)) return false;
        return BiometricManager.from(activity).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void prompt(Activity activity, Runnable onSuccess, Runnable onError) {
        if (!(activity instanceof FragmentActivity) || !canUse(activity)) {
            if (onError != null) onError.run();
            return;
        }
        FragmentActivity host = (FragmentActivity) activity;
        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(host);
        BiometricPrompt prompt = new BiometricPrompt(host, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                if (onSuccess != null) onSuccess.run();
            }
            @Override public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (onError != null) onError.run();
            }
            @Override public void onAuthenticationFailed() { }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso biométrico RgaPro")
                .setSubtitle("Confirma tu identidad para entrar")
                .setNegativeButtonText("Usar PIN")
                .build();
        prompt.authenticate(info);
    }
}
