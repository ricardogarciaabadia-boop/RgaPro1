package com.rgapro1.ocaso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {

    private static final int NAVY = Color.rgb(20, 55, 100);
    private static final int BLUE = Color.rgb(11, 114, 231);
    private static final int BG = Color.rgb(245, 247, 250);
    private static final int TEXT = Color.rgb(25, 35, 50);
    private static final int MUTED = Color.rgb(95, 105, 120);
    private static final int PBKDF2_ITERATIONS = 120000;

    private LinearLayout content;
    private SharedPreferences prefs;
    private boolean authenticated = false;
    private boolean authDialogShowing = false;
    private final Executor biometricExecutor = Executors.newSingleThreadExecutor();
    private final ArrayList<String> clients = new ArrayList<>();
    private final ArrayList<String> documents = new ArrayList<>();

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("rgapro_local", Context.MODE_PRIVATE);
        if (!hasPin()) {
            showCreatePinDialog();
        } else {
            showAccessScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null && hasPin() && !authenticated && !authDialogShowing) {
            showAccessScreen();
        }
    }

    private boolean hasPin() {
        return prefs != null && prefs.contains("pin_hash") && prefs.contains("pin_salt");
    }

    private void showCreatePinDialog() {
        authDialogShowing = true;
        final EditText first = pinInput("Nueva clave de 6 dígitos");
        final EditText second = pinInput("Repite la clave");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(4), dp(22), 0);
        box.addView(first, new LinearLayout.LayoutParams(-1, dp(52)));
        box.addView(second, new LinearLayout.LayoutParams(-1, dp(52)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Protege RgaPro")
                .setMessage("Crea una clave de acceso. Después podrás entrar también con la biometría del dispositivo.")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("Activar seguridad", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String a = first.getText().toString();
            String b = second.getText().toString();
            if (!a.matches("\\d{6}")) {
                first.setError("Usa exactamente 6 dígitos");
                return;
            }
            if (!a.equals(b)) {
                second.setError("Las claves no coinciden");
                return;
            }
            savePin(a);
            prefs.edit().putBoolean("biometric", true).apply();
            dialog.dismiss();
            authDialogShowing = false;
            authenticated = true;
            loadLocalData();
            buildHome();
            Toast.makeText(this, "Seguridad activada", Toast.LENGTH_SHORT).show();
        }));
        dialog.setOnDismissListener(d -> authDialogShowing = false);
        dialog.show();
    }

    private EditText pinInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setSelectAllOnFocus(false);
        return input;
    }

    private void savePin(String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = derivePin(pin, salt);
            prefs.edit()
                    .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la clave", e);
        }
    }

    private boolean verifyPin(String pin) {
        try {
            byte[] salt = Base64.decode(prefs.getString("pin_salt", ""), Base64.NO_WRAP);
            byte[] expected = Base64.decode(prefs.getString("pin_hash", ""), Base64.NO_WRAP);
            return MessageDigest.isEqual(expected, derivePin(pin, salt));
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] derivePin(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
        try {
            SecretKeyFactory factory;
            try {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (Exception ignored) {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private void showAccessScreen() {
        authDialogShowing = true;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(BG);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_rgapro);
        logo.setContentDescription("Logotipo RgaPro");
        root.addView(logo, new LinearLayout.LayoutParams(dp(90), dp(90)));

        TextView title = label("RgaPro", 28, NAVY, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView subtitle = label("Acceso protegido", 16, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(40)));

        Button pin = primaryButton("Introducir clave");
        pin.setOnClickListener(v -> showPinLogin());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(52));
        bp.topMargin = dp(18);
        root.addView(pin, bp);

        if (prefs.getBoolean("biometric", true) && Build.VERSION.SDK_INT >= 28) {
            Button bio = button("Usar biometría");
            bio.setOnClickListener(v -> authenticateBiometric());
            LinearLayout.LayoutParams bb = new LinearLayout.LayoutParams(-1, dp(52));
            bb.topMargin = dp(10);
            root.addView(bio, bb);
        }

        TextView info = label("La clave se almacena como un hash derivado con salt y no se guarda en texto plano.", 12, MUTED, false);
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(8), dp(18), dp(8), 0);
        root.addView(info, new LinearLayout.LayoutParams(-1, dp(65)));
        setContentView(root);
    }

    private void showPinLogin() {
        final EditText input = pinInput("Clave de 6 dígitos");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Desbloquear RgaPro")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Entrar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (verifyPin(input.getText().toString())) {
                dialog.dismiss();
                authenticated = true;
                authDialogShowing = false;
                loadLocalData();
                buildHome();
            } else {
                input.setError("Clave incorrecta");
            }
        }));
        dialog.show();
    }

    private void authenticateBiometric() {
        if (Build.VERSION.SDK_INT < 28) {
            showPinLogin();
            return;
        }
        BiometricPrompt.Builder builder = new BiometricPrompt.Builder(this)
                .setTitle("Acceso a RgaPro")
                .setSubtitle("Confirma tu identidad para continuar")
                .setDescription("Usa la huella o el reconocimiento biométrico configurado en el dispositivo.");
        if (Build.VERSION.SDK_INT >= 29) {
            builder.setDeviceCredentialAllowed(false);
        } else {
            builder.setNegativeButton("Usar clave", biometricExecutor, (dialog, which) -> showPinLogin());
        }
        BiometricPrompt prompt = builder.build();
        prompt.authenticate(new BiometricPrompt.CryptoObject(new javax.crypto.Mac() {
            @Override public String getAlgorithm() { return ""; }
            @Override public void init(java.security.Key key) { }
            @Override public void init(java.security.Key key, java.security.AlgorithmParameterSpec params) { }
            @Override public void init(java.security.Key key, java.security.AlgorithmParameterSpec params, java.security.SecureRandom random) { }
            @Override public void init(java.security.Key key, java.security.spec.AlgorithmParameterSpec params) { }
            @Override public void init(java.security.Key key, java.security.spec.AlgorithmParameterSpec params, java.security.SecureRandom random) { }
            @Override public byte[] doFinal() { return new byte[0]; }
            @Override public byte[] doFinal(byte[] input) { return input; }
            @Override public int doFinal(byte[] input, int offset, int len, byte[] output, int outOffset) { return 0; }
            @Override public void update(byte input) { }
            @Override public void update(byte[] input) { }
            @Override public void update(byte[] input, int offset, int len) { }
            @Override public void update(java.nio.ByteBuffer input) { }
            @Override public byte[] getMacLength() { return new byte[0]; }
            @Override public void reset() { }
        }), biometricExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                runOnUiThread(() -> {
                    authenticated = true;
                    authDialogShowing = false;
                    loadLocalData();
                    buildHome();
                });
            }

            @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Biometría no disponible: " + errString, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadLocalData() {
        clients.clear();
        documents.clear();
        Set<String> savedClients = prefs.getStringSet("clients", null);
        if (savedClients != null) clients.addAll(savedClients);
        Set<String> savedDocuments = prefs.getStringSet("documents", null);
        if (savedDocuments != null) documents.addAll(savedDocuments);
        if (clients.isEmpty()) {
            clients.add("Cliente de demostración");
            saveClients();
        }
        if (documents.isEmpty()) {
            documents.add("Documento de demostración.pdf");
            saveDocuments();
        }
    }

    private void saveClients() { prefs.edit().putStringSet("clients", new HashSet<>(clients)).apply(); }
    private void saveDocuments() { prefs.edit().putStringSet("documents", new HashSet<>(documents)).apply(); }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return view;
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(15);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setPadding(dp(16), 0, dp(16), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(220, 225, 232));
        b.setBackground(bg);
        return b;
    }

    private Button primaryButton(String title) {
        Button b = button(title);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        return b;
    }

    private LinearLayout page(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), 0, dp(16), 0);
        bar.setBackgroundColor(NAVY);
        Button back = new Button(this);
        back.setText("‹"); back.setTextColor(Color.WHITE); back.setTextSize(30); back.setAllCaps(false); back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> buildHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(64)));
        TextView heading = label(title, 21, Color.WHITE, true);
        bar.addView(heading, new LinearLayout.LayoutParams(0, dp(64), 1));
        root.addView(bar);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(18), dp(18), dp(18), dp(28));
        if (subtitle != null) { TextView sub = label(subtitle, 15, MUTED, false); sub.setPadding(dp(4), 0, dp(4), dp(14)); content.addView(sub, new LinearLayout.LayoutParams(-1, dp(48))); }
        ScrollView scroll = new ScrollView(this); scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root); return root;
    }

    private void buildHome() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(16), dp(8), dp(18), dp(8)); header.setBackgroundColor(NAVY);
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ic_rgapro); logo.setContentDescription("Logotipo RgaPro"); header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = label("RgaPro", 25, Color.WHITE, true); title.setPadding(dp(10), 0, 0, 0); header.addView(title, new LinearLayout.LayoutParams(0, dp(64), 1)); root.addView(header);
        ScrollView scroll = new ScrollView(this); LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20), dp(22), dp(20), dp(30));
        TextView welcome = label("Bienvenido a RgaPro", 25, TEXT, true); welcome.setGravity(Gravity.CENTER); body.addView(welcome, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView desc = label("Gestión profesional, organizada y sencilla.", 16, MUTED, false); desc.setGravity(Gravity.CENTER); body.addView(desc, new LinearLayout.LayoutParams(-1, dp(45)));
        addHomeButton(body, "CLIENTES", "Gestiona tus clientes", v -> buildClients()); addHomeButton(body, "DOCUMENTOS", "Consulta y organiza documentos", v -> buildDocuments()); addHomeButton(body, "CONFIGURACIÓN", "Seguridad y preferencias", v -> buildSettings());
        TextView local = label("Datos locales de demostración", 13, Color.GRAY, false); local.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(45)); lp.topMargin = dp(22); body.addView(local, lp);
        TextView version = label("RgaPro V1.2 · Acceso protegido", 13, Color.GRAY, false); version.setGravity(Gravity.CENTER); body.addView(version, new LinearLayout.LayoutParams(-1, dp(35)));
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }

    private void addHomeButton(LinearLayout parent, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18), dp(10), dp(18), dp(10)); GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), Color.rgb(225, 229, 235)); card.setBackground(bg); card.setOnClickListener(listener);
        card.addView(label(title, 17, TEXT, true), new LinearLayout.LayoutParams(-1, dp(28))); card.addView(label(subtitle, 13, MUTED, false), new LinearLayout.LayoutParams(-1, dp(24))); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(72)); cp.topMargin = dp(12); parent.addView(card, cp);
    }

    private void buildClients() { page("Clientes", "Clientes guardados únicamente en este dispositivo."); Button add = primaryButton("+ Añadir cliente"); add.setOnClickListener(v -> showAddClientDialog()); content.addView(add, new LinearLayout.LayoutParams(-1, dp(50))); addListHeader(content, "Tus clientes"); renderClients(); }
    private void renderClients() { while (content.getChildCount() > 2) content.removeViewAt(2); for (int i = 0; i < clients.size(); i++) { final int index = i; LinearLayout row = listRow(clients.get(i), "Cliente local"); row.setOnClickListener(v -> showClientActions(index)); content.addView(row); } }
    private void showAddClientDialog() { EditText input = new EditText(this); input.setHint("Nombre del cliente"); input.setSingleLine(true); AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Nuevo cliente").setView(input).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create(); dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String name = input.getText().toString().trim(); if (name.isEmpty()) { input.setError("Introduce un nombre"); return; } clients.add(name); saveClients(); dialog.dismiss(); buildClients(); })); dialog.show(); }
    private void showClientActions(int index) { new AlertDialog.Builder(this).setTitle(clients.get(index)).setItems(new String[]{"Ver ficha", "Eliminar"}, (d, which) -> { if (which == 0) Toast.makeText(this, "Ficha local de " + clients.get(index), Toast.LENGTH_SHORT).show(); else { clients.remove(index); saveClients(); buildClients(); } }).show(); }

    private void buildDocuments() { page("Documentos", "Organización local de documentos. La sincronización aún no está conectada."); Button add = primaryButton("+ Añadir documento"); add.setOnClickListener(v -> showAddDocumentDialog()); content.addView(add, new LinearLayout.LayoutParams(-1, dp(50))); addListHeader(content, "Documentos recientes"); renderDocuments(); }
    private void renderDocuments() { while (content.getChildCount() > 2) content.removeViewAt(2); for (int i = 0; i < documents.size(); i++) { final int index = i; LinearLayout row = listRow(documents.get(i), "Documento local"); row.setOnClickListener(v -> showDocumentActions(index)); content.addView(row); } }
    private void showAddDocumentDialog() { EditText input = new EditText(this); input.setHint("Nombre del documento"); input.setSingleLine(true); new AlertDialog.Builder(this).setTitle("Nuevo documento").setView(input).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", (d, which) -> { String name = input.getText().toString().trim(); if (!name.isEmpty()) { documents.add(name); saveDocuments(); buildDocuments(); } }).show(); }
    private void showDocumentActions(int index) { new AlertDialog.Builder(this).setTitle(documents.get(index)).setItems(new String[]{"Abrir ficha", "Eliminar"}, (d, which) -> { if (which == 0) Toast.makeText(this, "Documento preparado para integración", Toast.LENGTH_SHORT).show(); else { documents.remove(index); saveDocuments(); buildDocuments(); } }).show(); }
    private LinearLayout listRow(String title, String subtitle) { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(16), dp(8), dp(16), dp(8)); GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), Color.rgb(225, 229, 235)); row.setBackground(bg); row.addView(label(title, 16, TEXT, true), new LinearLayout.LayoutParams(-1, dp(30))); row.addView(label(subtitle, 12, MUTED, false), new LinearLayout.LayoutParams(-1, dp(22))); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64)); lp.topMargin = dp(10); row.setLayoutParams(lp); return row; }
    private void addListHeader(LinearLayout parent, String title) { TextView h = label(title, 17, TEXT, true); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44)); lp.topMargin = dp(14); parent.addView(h, lp); }

    private void buildSettings() {
        page("Configuración", "Ajustes locales de RgaPro.");
        TextView security = label("SEGURIDAD", 13, BLUE, true); content.addView(security, new LinearLayout.LayoutParams(-1, dp(36)));
        Button bio = button(prefs.getBoolean("biometric", true) ? "✓ Biometría activada" : "Activar biometría");
        bio.setOnClickListener(v -> { boolean value = !prefs.getBoolean("biometric", true); prefs.edit().putBoolean("biometric", value).apply(); buildSettings(); });
        content.addView(bio, new LinearLayout.LayoutParams(-1, dp(52)));
        Button change = button("Cambiar clave de acceso"); change.setOnClickListener(v -> showChangePinDialog()); content.addView(change, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView note = label("La clave usa PBKDF2 con salt aleatorio y no se almacena en texto plano. La biometría utiliza el sistema seguro del dispositivo.", 13, MUTED, false); note.setPadding(dp(4), dp(8), dp(4), dp(14)); content.addView(note, new LinearLayout.LayoutParams(-1, dp(76)));
        TextView data = label("DATOS LOCALES", 13, BLUE, true); content.addView(data, new LinearLayout.LayoutParams(-1, dp(36)));
        Button clear = button("Borrar datos de demostración"); clear.setOnClickListener(v -> confirmClearData()); content.addView(clear, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView about = label("RgaPro V1.2\nAPK de desarrollo · Sin backend conectado\nLa autenticación protege el acceso local a la app.", 13, MUTED, false); about.setPadding(dp(4), dp(22), dp(4), 0); content.addView(about, new LinearLayout.LayoutParams(-1, dp(90)));
    }

    private void showChangePinDialog() {
        final EditText oldPin = pinInput("Clave actual"); final EditText newPin = pinInput("Nueva clave de 6 dígitos"); final EditText confirm = pinInput("Repite la nueva clave");
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), 0, dp(22), 0); box.addView(oldPin, new LinearLayout.LayoutParams(-1, dp(52))); box.addView(newPin, new LinearLayout.LayoutParams(-1, dp(52))); box.addView(confirm, new LinearLayout.LayoutParams(-1, dp(52)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Cambiar clave").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> { String o=oldPin.getText().toString(), n=newPin.getText().toString(), c=confirm.getText().toString(); if(!verifyPin(o)){oldPin.setError("Clave actual incorrecta");return;} if(!n.matches("\\d{6}")){newPin.setError("Usa 6 dígitos");return;} if(!n.equals(c)){confirm.setError("No coincide");return;} savePin(n); dialog.dismiss(); Toast.makeText(this,"Clave actualizada",Toast.LENGTH_SHORT).show(); })); dialog.show();
    }

    private void confirmClearData() { new AlertDialog.Builder(this).setTitle("Borrar datos").setMessage("Se eliminarán los clientes y documentos de demostración guardados localmente.").setNegativeButton("Cancelar", null).setPositiveButton("Borrar", (d,w)->{clients.clear();documents.clear();prefs.edit().clear().apply();Toast.makeText(this,"Datos locales borrados",Toast.LENGTH_SHORT).show();authenticated=false;showCreatePinDialog();}).show(); }

    @Override protected void onDestroy() { biometricExecutor.execute(() -> { }); super.onDestroy(); }
}
