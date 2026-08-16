package com.rgapro1.ocaso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int NAVY = Color.rgb(20, 55, 100);
    private static final int BLUE = Color.rgb(11, 114, 231);
    private static final int BG = Color.rgb(245, 247, 250);
    private static final int TEXT = Color.rgb(25, 35, 50);
    private static final int MUTED = Color.rgb(95, 105, 120);

    private LinearLayout content;
    private SharedPreferences prefs;
    private final ArrayList<String> clients = new ArrayList<>();
    private final ArrayList<String> documents = new ArrayList<>();

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("rgapro_local", Context.MODE_PRIVATE);
        loadLocalData();
        buildHome();
    }

    private void loadLocalData() {
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

    private void saveClients() {
        prefs.edit().putStringSet("clients", new HashSet<>(clients)).apply();
    }

    private void saveDocuments() {
        prefs.edit().putStringSet("documents", new HashSet<>(documents)).apply();
    }

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
        back.setText("‹");
        back.setTextColor(Color.WHITE);
        back.setTextSize(30);
        back.setAllCaps(false);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> buildHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(64)));

        TextView heading = label(title, 21, Color.WHITE, true);
        bar.addView(heading, new LinearLayout.LayoutParams(0, dp(64), 1));
        root.addView(bar);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));

        if (subtitle != null) {
            TextView sub = label(subtitle, 15, MUTED, false);
            sub.setPadding(dp(4), 0, dp(4), dp(14));
            content.addView(sub, new LinearLayout.LayoutParams(-1, dp(48)));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        return root;
    }

    private void buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(8), dp(18), dp(8));
        header.setBackgroundColor(NAVY);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.rgapro1.ocaso.R.drawable.ic_rgapro);
        logo.setContentDescription("Logotipo RgaPro");
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = label("RgaPro", 25, Color.WHITE, true);
        title.setPadding(dp(10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(64), 1));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(22), dp(20), dp(30));

        TextView welcome = label("Bienvenido a RgaPro", 25, TEXT, true);
        welcome.setGravity(Gravity.CENTER);
        body.addView(welcome, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView desc = label("Gestión profesional, organizada y sencilla.", 16, MUTED, false);
        desc.setGravity(Gravity.CENTER);
        body.addView(desc, new LinearLayout.LayoutParams(-1, dp(45)));

        addHomeButton(body, "CLIENTES", "Gestiona tus clientes", v -> buildClients());
        addHomeButton(body, "DOCUMENTOS", "Consulta y organiza documentos", v -> buildDocuments());
        addHomeButton(body, "CONFIGURACIÓN", "Seguridad y preferencias", v -> buildSettings());

        TextView local = label("Datos locales de demostración", 13, Color.GRAY, false);
        local.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(45));
        lp.topMargin = dp(22);
        body.addView(local, lp);

        TextView version = label("RgaPro V1.1", 13, Color.GRAY, false);
        version.setGravity(Gravity.CENTER);
        body.addView(version, new LinearLayout.LayoutParams(-1, dp(35)));

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void addHomeButton(LinearLayout parent, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(225, 229, 235));
        card.setBackground(bg);
        card.setOnClickListener(listener);

        TextView t = label(title, 17, TEXT, true);
        TextView s = label(subtitle, 13, MUTED, false);
        card.addView(t, new LinearLayout.LayoutParams(-1, dp(28)));
        card.addView(s, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(72));
        cp.topMargin = dp(12);
        parent.addView(card, cp);
    }

    private void buildClients() {
        page("Clientes", "Clientes guardados únicamente en este dispositivo.");
        Button add = primaryButton("+ Añadir cliente");
        add.setOnClickListener(v -> showAddClientDialog());
        content.addView(add, new LinearLayout.LayoutParams(-1, dp(50)));
        addListHeader(content, "Tus clientes");
        renderClients();
    }

    private void renderClients() {
        while (content.getChildCount() > 2) content.removeViewAt(2);
        for (int i = 0; i < clients.size(); i++) {
            final int index = i;
            LinearLayout row = listRow(clients.get(i), "Cliente local");
            row.setOnClickListener(v -> showClientActions(index));
            content.addView(row);
        }
    }

    private void showAddClientDialog() {
        EditText input = new EditText(this);
        input.setHint("Nombre del cliente");
        input.setSingleLine(true);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo cliente")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Introduce un nombre");
                return;
            }
            clients.add(name);
            saveClients();
            dialog.dismiss();
            buildClients();
        }));
        dialog.show();
    }

    private void showClientActions(int index) {
        new AlertDialog.Builder(this)
                .setTitle(clients.get(index))
                .setItems(new String[]{"Ver ficha", "Eliminar"}, (d, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "Ficha local de " + clients.get(index), Toast.LENGTH_SHORT).show();
                    } else {
                        clients.remove(index);
                        saveClients();
                        buildClients();
                    }
                }).show();
    }

    private void buildDocuments() {
        page("Documentos", "Organización local de documentos. La sincronización aún no está conectada.");
        Button add = primaryButton("+ Añadir documento");
        add.setOnClickListener(v -> showAddDocumentDialog());
        content.addView(add, new LinearLayout.LayoutParams(-1, dp(50)));
        addListHeader(content, "Documentos recientes");
        renderDocuments();
    }

    private void renderDocuments() {
        while (content.getChildCount() > 2) content.removeViewAt(2);
        for (int i = 0; i < documents.size(); i++) {
            final int index = i;
            LinearLayout row = listRow(documents.get(i), "Documento local");
            row.setOnClickListener(v -> showDocumentActions(index));
            content.addView(row);
        }
    }

    private void showAddDocumentDialog() {
        EditText input = new EditText(this);
        input.setHint("Nombre del documento");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Nuevo documento")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        documents.add(name);
                        saveDocuments();
                        buildDocuments();
                    }
                }).show();
    }

    private void showDocumentActions(int index) {
        new AlertDialog.Builder(this)
                .setTitle(documents.get(index))
                .setItems(new String[]{"Abrir ficha", "Eliminar"}, (d, which) -> {
                    if (which == 0) Toast.makeText(this, "Documento preparado para integración", Toast.LENGTH_SHORT).show();
                    else {
                        documents.remove(index);
                        saveDocuments();
                        buildDocuments();
                    }
                }).show();
    }

    private LinearLayout listRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(225, 229, 235));
        row.setBackground(bg);
        row.addView(label(title, 16, TEXT, true), new LinearLayout.LayoutParams(-1, dp(30)));
        row.addView(label(subtitle, 12, MUTED, false), new LinearLayout.LayoutParams(-1, dp(22)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64));
        lp.topMargin = dp(10);
        row.setLayoutParams(lp);
        return row;
    }

    private void addListHeader(LinearLayout parent, String title) {
        TextView h = label(title, 17, TEXT, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.topMargin = dp(14);
        parent.addView(h, lp);
    }

    private void buildSettings() {
        page("Configuración", "Ajustes locales de RgaPro.");

        TextView security = label("SEGURIDAD", 13, BLUE, true);
        content.addView(security, new LinearLayout.LayoutParams(-1, dp(36)));

        Button lock = button(prefs.getBoolean("biometric", false) ? "✓ Bloqueo de acceso activado" : "Activar bloqueo de acceso");
        lock.setOnClickListener(v -> {
            boolean value = !prefs.getBoolean("biometric", false);
            prefs.edit().putBoolean("biometric", value).apply();
            Toast.makeText(this, value ? "Preferencia guardada" : "Bloqueo desactivado", Toast.LENGTH_SHORT).show();
            buildSettings();
        });
        content.addView(lock, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView note = label("El acceso biométrico real se conectará en la siguiente fase. Esta opción no protege todavía datos del servidor.", 13, MUTED, false);
        note.setPadding(dp(4), dp(8), dp(4), dp(14));
        content.addView(note, new LinearLayout.LayoutParams(-1, dp(68)));

        TextView data = label("DATOS LOCALES", 13, BLUE, true);
        content.addView(data, new LinearLayout.LayoutParams(-1, dp(36)));

        Button clear = button("Borrar datos de demostración");
        clear.setOnClickListener(v -> confirmClearData());
        content.addView(clear, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView about = label("RgaPro V1.1\nAPK de desarrollo · Sin backend conectado\nNo introducir datos reales de clientes hasta completar cifrado, autenticación y servidor privado.", 13, MUTED, false);
        about.setPadding(dp(4), dp(22), dp(4), 0);
        content.addView(about, new LinearLayout.LayoutParams(-1, dp(100)));
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar datos")
                .setMessage("Se eliminarán los clientes y documentos de demostración guardados localmente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (d, w) -> {
                    clients.clear();
                    documents.clear();
                    prefs.edit().clear().apply();
                    Toast.makeText(this, "Datos locales borrados", Toast.LENGTH_SHORT).show();
                    buildHome();
                }).show();
    }
}
