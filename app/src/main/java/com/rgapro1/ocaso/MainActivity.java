```java
package com.rgapro1.ocaso;

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

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView createText(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView header = createText(
                "RgaPro",
                28,
                Color.WHITE,
                true
        );

        header.setGravity(Gravity.CENTER);
        header.setBackgroundColor(Color.rgb(20, 55, 100));

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(70)
                )
        );

        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(30));

        TextView welcome = createText(
                "Bienvenido a RgaPro",
                24,
                Color.rgb(25, 35, 50),
                true
        );

        content.addView(
                welcome,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );

        TextView description = createText(
                "Tu aplicación de gestión profesional.",
                17,
                Color.DKGRAY,
                false
        );

        LinearLayout.LayoutParams descriptionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        content.addView(description, descriptionParams);

        Button clientes = new Button(this);
        clientes.setText("CLIENTES");
        clientes.setTextSize(16);
        clientes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage("Módulo de clientes");
            }
        });

        content.addView(
                clientes,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );

        Button documentos = new Button(this);
        documentos.setText("DOCUMENTOS");
        documentos.setTextSize(16);
        documentos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage("Módulo de documentos");
            }
        });

        LinearLayout.LayoutParams documentosParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        documentosParams.topMargin = dp(12);
        content.addView(documentos, documentosParams);

        Button configuracion = new Button(this);
        configuracion.setText("CONFIGURACIÓN");
        configuracion.setTextSize(16);
        configuracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage("Configuración");
            }
        });

        LinearLayout.LayoutParams configuracionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        configuracionParams.topMargin = dp(12);
        content.addView(configuracion, configuracionParams);

        TextView version = createText(
                "RgaPro V1.0",
                14,
                Color.GRAY,
                false
        );

        version.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams versionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(60)
                );

        versionParams.topMargin = dp(30);
        content.addView(version, versionParams);

        scrollView.addView(content);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
```
