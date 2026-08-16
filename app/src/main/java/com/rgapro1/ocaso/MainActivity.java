package com.rgapro1.ocaso;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 250));
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("RgaPro");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(20, 55, 100));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(10), 0, dp(25));

        root.addView(title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        TextView welcome = new TextView(this);
        welcome.setText("Bienvenido a RgaPro");
        welcome.setTextSize(24);
        welcome.setTextColor(Color.rgb(25, 35, 50));
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(0, dp(10), 0, dp(10));

        root.addView(welcome,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        TextView description = new TextView(this);
        description.setText("Tu aplicación de gestión profesional.");
        description.setTextSize(17);
        description.setTextColor(Color.DKGRAY);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 0, 0, dp(25));

        root.addView(description,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        Button clientes = new Button(this);
        clientes.setText("CLIENTES");
        clientes.setOnClickListener(v ->
                Toast.makeText(this, "Módulo de clientes", Toast.LENGTH_SHORT).show()
        );

        root.addView(clientes,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                ));

        Button documentos = new Button(this);
        documentos.setText("DOCUMENTOS");
        documentos.setOnClickListener(v ->
                Toast.makeText(this, "Módulo de documentos", Toast.LENGTH_SHORT).show()
        );

        LinearLayout.LayoutParams documentosParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );
        documentosParams.topMargin = dp(12);

        root.addView(documentos, documentosParams);

        Button configuracion = new Button(this);
        configuracion.setText("CONFIGURACIÓN");
        configuracion.setOnClickListener(v ->
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        );

        LinearLayout.LayoutParams configuracionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );
        configuracionParams.topMargin = dp(12);

        root.addView(configuracion, configuracionParams);

        TextView version = new TextView(this);
        version.setText("RgaPro V1.0");
        version.setTextSize(14);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(30), 0, 0);

        root.addView(version,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        setContentView(root);
    }
}
