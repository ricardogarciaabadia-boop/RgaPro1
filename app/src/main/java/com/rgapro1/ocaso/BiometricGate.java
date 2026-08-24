package com.rgapro1.ocaso;

import android.app.Activity;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

/** Small reusable biometric gate for login/unlock. */
public final class BiometricGate {
    private BiometricGate() {}

    public static boolean canUse(Activity activity) {
        return BiometricManager.from(activity).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void prompt(Activity activity, Runnable onSuccess, Runnable onError) {
        if (!canUse(activity)) { if (onError != null) onError.run(); return; }
        ExecutorHolder executor = new ExecutorHolder(activity);
        BiometricPrompt prompt = new BiometricPrompt(activity, executor.executor, new BiometricPrompt.AuthenticationCallback() {
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

    private static final class ExecutorHolder {
        final java.util.concurrent.Executor executor;
        ExecutorHolder(Activity a) { executor = ContextCompat.getMainExecutor(a); }
    }
}
