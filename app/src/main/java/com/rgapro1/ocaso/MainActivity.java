```java
package com.rgapro1.ocaso;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int margen = 32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarInicio();
    }

    private LinearLayout crearContenedor() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(margen, 50, margen, 40);
        layout.setBackgroundColor(Color.WHITE);
        return layout;
    }

    private TextView crearTitulo(String texto) {
        TextView titulo = new TextView(this);
        titulo.setText(texto);
        titulo.setTextSize(30);
        titulo.setTextColor(Color.rgb(20, 45, 80));
        titulo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, 0, 0, 35);
        return titulo;
    }

    private TextView crearTexto(String texto) {
        TextView textoView = new TextView(this);
        textoView.setText(texto);
        textoView.setTextSize(18);
        textoView.setTextColor(Color.DKGRAY);
        textoView.setGravity(Gravity.CENTER);
        textoView.setPadding(10, 10, 10, 30);
        return textoView;
    }

    private Button crearBoton(String texto, final Runnable accion) {
        Button boton = new Button(this);
        boton.setText(texto);
        boton.setTextSize(17);
        boton.setAllCaps(false);

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 8, 0, 8);
        boton.setLayoutParams(parametros);

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accion.run();
            }
        });

        return boton;
    }

    private void mostrarInicio() {

        LinearLayout contenido = crearContenedor();

        TextView titulo = crearTitulo("RgaPro");
        contenido.addView(titulo);

        TextView subtitulo = crearTexto(
                "Mi Cartera Ocaso\n\n" +
                "Gestión sencilla de tu cartera"
        );
        subtitulo.setTextSize(20);
        contenido.addView(subtitulo);

        contenido.addView(
                crearBoton("👥  Clientes", new Runnable() {
                    @Override
                    public void run() {
                        mostrarClientes();
                    }
                })
        );

        contenido.addView(
                crearBoton("📋  Pólizas", new Runnable() {
                    @Override
                    public void run() {
                        mostrarPolizas();
                    }
                })
        );

        contenido.addView(
                crearBoton("💼  Cartera", new Runnable() {
                    @Override
                    public void run() {
                        mostrarCartera();
                    }
                })
        );

        contenido.addView(
                crearBoton("📷  Escáner", new Runnable() {
                    @Override
                    public void run() {
                        mostrarEscaner();
                    }
                })
        );

        contenido.addView(
                crearBoton("⚙️  Configuración", new Runnable() {
                    @Override
                    public void run() {
                        mostrarConfiguracion();
                    }
                })
        );

        contenido.addView(crearTexto(
                "\nRgaPro V1\n" +
                "Mi Cartera Ocaso"
        ));

        mostrarVista(contenido);
    }

    private void mostrarClientes() {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo("Clientes"));

        contenido.addView(crearTexto(
                "Gestión de clientes\n\n" +
                "En esta sección podrás consultar y organizar " +
                "la información de tus clientes."
        ));

        contenido.addView(
                crearBoton("➕  Nuevo cliente", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Nuevo cliente",
                                "El formulario de clientes se incorporará en la siguiente versión."
                        );
                    }
                })
        );

        contenido.addView(
                crearBoton("🔎  Buscar cliente", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Buscar cliente",
                                "La búsqueda de clientes se incorporará en la siguiente versión."
                        );
                    }
                })
        );

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
    }

    private void mostrarPolizas() {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo("Pólizas"));

        contenido.addView(crearTexto(
                "Gestión de pólizas\n\n" +
                "Aquí podrás consultar y organizar las pólizas " +
                "asociadas a tus clientes."
        ));

        contenido.addView(
                crearBoton("➕  Nueva póliza", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Nueva póliza",
                                "El formulario de pólizas se incorporará en la siguiente versión."
                        );
                    }
                })
        );

        contenido.addView(
                crearBoton("🔎  Buscar póliza", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Buscar póliza",
                                "La búsqueda de pólizas se incorporará en la siguiente versión."
                        );
                    }
                })
        );

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
    }

    private void mostrarCartera() {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo("Cartera"));

        contenido.addView(crearTexto(
                "Resumen de cartera\n\n" +
                "Clientes: 0\n" +
                "Pólizas: 0\n\n" +
                "Todavía no hay datos guardados."
        ));

        contenido.addView(
                crearBoton("🔄  Actualizar", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Cartera",
                                "La cartera está actualizada."
                        );
                    }
                })
        );

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
    }

    private void mostrarEscaner() {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo("Escáner"));

        contenido.addView(crearTexto(
                "Escáner de documentos\n\n" +
                "Esta sección está preparada para incorporar " +
                "la captura y lectura de documentos."
        ));

        contenido.addView(
                crearBoton("📷  Abrir cámara", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Escáner",
                                "La cámara y el escáner se incorporarán en una siguiente versión."
                        );
                    }
                })
        );

        contenido.addView(
                crearBoton("📄  Documentos", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Documentos",
                                "El gestor de documentos se incorporará posteriormente."
                        );
                    }
                })
        );

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
    }

    private void mostrarConfiguracion() {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo("Configuración"));

        contenido.addView(crearTexto(
                "Configuración de RgaPro\n\n" +
                "Versión: 1.0\n" +
                "Aplicación: RgaPro\n" +
                "Cartera: Ocaso"
        ));

        contenido.addView(
                crearBoton("ℹ️  Información", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "RgaPro",
                                "RgaPro V1\n\nMi Cartera Ocaso"
                        );
                    }
                })
        );

        contenido.addView(
                crearBoton("🗑️  Datos", new Runnable() {
                    @Override
                    public void run() {
                        mostrarMensaje(
                                "Datos",
                                "Todavía no hay datos almacenados."
                        );
                    }
                })
        );

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
    }

    private void agregarBotonVolver(LinearLayout contenido) {

        contenido.addView(crearBoton("←  Volver al inicio", new Runnable() {
            @Override
            public void run() {
                mostrarInicio();
            }
        }));
    }

    private void mostrarMensaje(String titulo, String mensaje) {

        LinearLayout contenido = crearContenedor();

        contenido.addView(crearTitulo(titulo));
        contenido.addView(crearTexto(mensaje));

        agregarBotonVolver(contenido);

        mostrarVista(contenido);
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
}
```
