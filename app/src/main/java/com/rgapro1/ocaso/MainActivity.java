package com.rgapro1.ocaso;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

```
private int padding(int dp) {
    return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
}

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mostrarInicio();
}

private LinearLayout crearContenedor() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);

    int p = padding(24);
    layout.setPadding(p, p, p, p);

    return layout;
}

private TextView crearTitulo(String texto) {
    TextView titulo = new TextView(this);

    titulo.setText(texto);
    titulo.setTextSize(30);
    titulo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    titulo.setGravity(Gravity.CENTER);
    titulo.setPadding(0, padding(12), 0, padding(20));

    return titulo;
}

private TextView crearTexto(String texto) {
    TextView vista = new TextView(this);

    vista.setText(texto);
    vista.setTextSize(18);
    vista.setGravity(Gravity.CENTER);
    vista.setPadding(
            padding(8),
            padding(12),
            padding(8),
            padding(24)
    );

    return vista;
}

private Button crearBoton(String texto, final Runnable accion) {
    Button boton = new Button(this);

    boton.setText(texto);
    boton.setTextSize(17);

    LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    params.setMargins(
            0,
            padding(6),
            0,
            padding(6)
    );

    boton.setLayoutParams(params);

    boton.setOnClickListener(v -> accion.run());

    return boton;
}

private void mostrarInicio() {

    LinearLayout root = crearContenedor();

    root.addView(crearTitulo("RgaPro"));

    root.addView(
            crearTexto(
                    "Mi Cartera Ocaso\n\n" +
                    "Gestión sencilla de clientes, pólizas y cartera."
            )
    );

    root.addView(
            crearBoton("👥  Clientes", () ->
                    mostrarSeccion(
                            "Clientes",
                            "Aquí podrás consultar y gestionar tus clientes."
                    )
            )
    );

    root.addView(
            crearBoton("📋  Pólizas", () ->
                    mostrarSeccion(
                            "Pólizas",
                            "Aquí podrás consultar y gestionar las pólizas."
                    )
            )
    );

    root.addView(
            crearBoton("💼  Cartera", () ->
                    mostrarSeccion(
                            "Cartera",
                            "Aquí tendrás el resumen de tu cartera."
                    )
            )
    );

    root.addView(
            crearBoton("📷  Escáner", () ->
                    mostrarSeccion(
                            "Escáner",
                            "Módulo preparado para incorporar el escáner de documentos."
                    )
            )
    );

    root.addView(
            crearBoton("⚙️  Configuración", () ->
                    mostrarSeccion(
                            "Configuración",
                            "Configuración general de RgaPro."
                    )
            )
    );

    root.addView(
            crearBoton("ℹ️  Información", () ->
                    mostrarSeccion(
                            "RgaPro",
                            "Mi Cartera Ocaso\n\nVersión 1.0\n\n" +
                            "Aplicación RgaPro para gestión de cartera."
                    )
            )
    );

    mostrarVista(root);
}

private void mostrarSeccion(String nombre, String descripcion) {

    LinearLayout root = crearContenedor();

    root.addView(crearTitulo(nombre));

    root.addView(crearTexto(descripcion));

    root.addView(
            crearBoton("←  Volver al inicio", this::mostrarInicio)
    );

    mostrarVista(root);
}

private void mostrarVista(View vista) {

    ScrollView scroll = new ScrollView(this);

    scroll.setFillViewport(true);
    scroll.addView(vista);

    setContentView(scroll);
}

@Override
public void onBackPressed() {
    mostrarInicio();
}
```

}
