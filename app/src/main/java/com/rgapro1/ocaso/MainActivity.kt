package com.rgapro1.ocaso

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var contenido: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mostrarInicio()
    }

    private fun crearContenedor(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }
    }

    private fun titulo(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
    }

    private fun boton(texto: String, accion: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            textSize = 18f
            setOnClickListener { accion() }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
    }

    private fun mostrarInicio() {

        val root = crearContenedor()

        root.addView(titulo("RgaPro"))

        val bienvenida = TextView(this).apply {
            text = "Mi Cartera Ocaso"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        root.addView(bienvenida)

        root.addView(
            boton("👥 Clientes") {
                mostrarSeccion(
                    "Clientes",
                    "Gestión de clientes de RgaPro."
                )
            }
        )

        root.addView(
            boton("📋 Pólizas") {
                mostrarSeccion(
                    "Pólizas",
                    "Consulta y gestión de pólizas."
                )
            }
        )

        root.addView(
            boton("💼 Cartera") {
                mostrarSeccion(
                    "Cartera",
                    "Resumen de la cartera."
                )
            }
        )

        root.addView(
            boton("📷 Escáner") {
                mostrarSeccion(
                    "Escáner",
                    "El módulo de escáner está preparado para integrarse."
                )
            }
        )

        root.addView(
            boton("⚙️ Configuración") {
                mostrarSeccion(
                    "Configuración",
                    "Configuración de RgaPro."
                )
            }
        )

        mostrarVista(root)
    }

    private fun mostrarSeccion(
        nombre: String,
        descripcion: String
    ) {

        val root = crearContenedor()

        root.addView(titulo(nombre))

        val texto = TextView(this).apply {
            text = descripcion
            textSize = 19f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        root.addView(texto)

        root.addView(
            boton("← Volver") {
                mostrarInicio()
            }
        )

        mostrarVista(root)
    }

    private fun mostrarVista(vista: View) {

        val scroll = ScrollView(this).apply {
            addView(vista)
        }

        setContentView(scroll)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        mostrarInicio()
    }
}
