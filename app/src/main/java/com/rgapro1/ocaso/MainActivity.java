package com.rgapro1.ocaso;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return view;
    }

    private Button actionButton(String label, final String message) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView header = text("RgaPro", 28, Color.WHITE, true);
        header.setBackgroundColor(Color.rgb(20, 55, 100));
        header.setPadding(dp(16), 0, dp(16), 0);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20), dp(24), dp(20), dp(30));

        TextView welcome = text("Bienvenido a RgaPro", 24, Color.rgb(25, 35, 50), true);
        content.addView(welcome, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        TextView description = text("Tu aplicación de gestión profesional.", 17, Color.DKGRAY, false);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        content.addView(description, descriptionParams);

        Button clientes = actionButton("CLIENTES", "Módulo de clientes");
        content.addView(clientes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55)));

        Button documentos = actionButton("DOCUMENTOS", "Módulo de documentos");
        LinearLayout.LayoutParams documentosParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55));
        documentosParams.topMargin = dp(12);
        content.addView(documentos, documentosParams);

        Button configuracion = actionButton("CONFIGURACIÓN", "Configuración");
        LinearLayout.LayoutParams configuracionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55));
        configuracionParams.topMargin = dp(12);
        content.addView(configuracion, configuracionParams);

        TextView version = text("RgaPro V1.0", 14, Color.GRAY, false);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        versionParams.topMargin = dp(30);
        content.addView(version, versionParams);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }
}
