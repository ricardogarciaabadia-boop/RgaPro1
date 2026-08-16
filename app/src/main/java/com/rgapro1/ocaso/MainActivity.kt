```kotlin
package com.rgapro1.ocaso

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var contenido: LinearLayout
    private val prefs by lazy {
        getSharedPreferences("rgapro_data", Context.MODE_PRIVATE)
    }

    private val clientes = mutableListOf<Cliente>()
    private val polizas = mutableListOf<Poliza>()

    data class Cliente(
        val id: Long,
        var nombre: String,
        var telefono: String,
        var email: String
    )

    data class Poliza(
        val id: Long,
        var clienteId: Long,
        var numero: String,
        var tipo: String,
        var prima: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cargarDatos()
        mostrarInicio()
    }

    // ============================================================
    // DATOS
    // ============================================================

    private fun cargarDatos() {
        clientes.clear()
        polizas.clear()

        val clientesJson = prefs.getString("clientes", "[]") ?: "[]"
        val arrayClientes = JSONArray(clientesJson)

        for (i in 0 until arrayClientes.length()) {
            val o = arrayClientes.getJSONObject(i)

            clientes.add(
                Cliente(
                    id = o.getLong("id"),
                    nombre = o.getString("nombre"),
                    telefono = o.getString("telefono"),
                    email = o.getString("email")
                )
            )
        }

        val polizasJson = prefs.getString("polizas", "[]") ?: "[]"
        val arrayPolizas = JSONArray(polizasJson)

        for (i in 0 until arrayPolizas.length()) {
            val o = arrayPolizas.getJSONObject(i)

            polizas.add(
                Poliza(
                    id = o.getLong("id"),
                    clienteId = o.getLong("clienteId"),
                    numero = o.getString("numero"),
                    tipo = o.getString("tipo"),
                    prima = o.getDouble("prima")
                )
            )
        }
    }

    private fun guardarDatos() {
        val clientesArray = JSONArray()

        clientes.forEach {
            val o = JSONObject()

            o.put("id", it.id)
            o.put("nombre", it.nombre)
            o.put("telefono", it.telefono)
            o.put("email", it.email)

            clientesArray.put(o)
        }

        val polizasArray = JSONArray()

        polizas.forEach {
            val o = JSONObject()

            o.put("id", it.id)
            o.put("clienteId", it.clienteId)
            o.put("numero", it.numero)
            o.put("tipo", it.tipo)
            o.put("prima", it.prima)

            polizasArray.put(o)
        }

        prefs.edit()
            .putString("clientes", clientesArray.toString())
            .putString("polizas", polizasArray.toString())
            .apply()
    }

    // ============================================================
    // INTERFAZ GENERAL
    // ============================================================

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
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
    }

    private fun boton(texto: String, accion: () -> Unit): Button {
        return Button(this).apply {
            text = texto
            textSize = 17f
            setOnClickListener {
                accion()
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
    }

    private fun campo(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 17f
            setPadding(20, 15, 20, 15)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        }
    }

    private fun mostrarVista(vista: LinearLayout) {
        val scroll = ScrollView(this)
        scroll.addView(vista)
        setContentView(scroll)
    }

    // ============================================================
    // INICIO
    // ============================================================

    private fun mostrarInicio() {
        val root = crearContenedor()

        root.addView(titulo("RgaPro"))

        val bienvenida = TextView(this).apply {
            text = "Gestión de cartera"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        root.addView(bienvenida)

        root.addView(
            boton("👥 Clientes (${clientes.size})") {
                mostrarClientes()
            }
        )

        root.addView(
            boton("📋 Pólizas (${polizas.size})") {
                mostrarPolizas()
            }
        )

        root.addView(
            boton("💼 Cartera") {
                mostrarCartera()
            }
        )

        root.addView(
            boton("📷 Escáner") {
                mostrarEscaner()
            }
        )

        root.addView(
            boton("⚙️ Configuración") {
                mostrarConfiguracion()
            }
        )

        mostrarVista(root)
    }

    // ============================================================
    // CLIENTES
    // ============================================================

    private fun mostrarClientes() {
        val root = crearContenedor()

        root.addView(titulo("Clientes"))

        root.addView(
            boton("➕ Nuevo cliente") {
                nuevoCliente()
            }
        )

        val buscador = campo("Buscar cliente")
        root.addView(buscador)

        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun actualizarLista(texto: String = "") {
            lista.removeAllViews()

            val filtro = texto.trim().lowercase()

            clientes
                .filter {
                    filtro.isEmpty() ||
                            it.nombre.lowercase().contains(filtro) ||
                            it.telefono.lowercase().contains(filtro) ||
                            it.email.lowercase().contains(filtro)
                }
                .forEach { cliente ->

                    val caja = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 20, 20, 20)
                    }

                    val nombre = TextView(this).apply {
                        text = cliente.nombre
                        textSize = 20f
                    }

                    val datos = TextView(this).apply {
                        text = "${cliente.telefono}\n${cliente.email}"
                        textSize = 16f
                    }

                    caja.addView(nombre)
                    caja.addView(datos)

                    caja.setOnClickListener {
                        menuCliente(cliente)
                    }

                    lista.addView(caja)
                }

            if (lista.childCount == 0) {
                val vacio = TextView(this).apply {
                    text = "No hay clientes."
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                lista.addView(vacio)
            }
        }

        buscador.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    actualizarLista(s?.toString() ?: "")
                }

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            }
        )

        actualizarLista()

        root.addView(lista)

        root.addView(
            boton("← Volver") {
                mostrarInicio()
            }
        )

        mostrarVista(root)
    }

    private fun nuevoCliente() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
        }

        val nombre = campo("Nombre completo")
        val telefono = campo("Teléfono")
        val email = campo("Email")

        root.addView(nombre)
        root.addView(telefono)
        root.addView(email)

        AlertDialog.Builder(this)
            .setTitle("Nuevo cliente")
            .setView(root)
            .setPositiveButton("Guardar") { _, _ ->

                if (nombre.text.toString().trim().isEmpty()) {
                    Toast.makeText(
                        this,
                        "Introduce el nombre del cliente",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                clientes.add(
                    Cliente(
                        id = System.currentTimeMillis(),
                        nombre = nombre.text.toString().trim(),
                        telefono = telefono.text.toString().trim(),
                        email = email.text.toString().trim()
                    )
                )

                guardarDatos()
                mostrarClientes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun menuCliente(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle(cliente.nombre)
            .setItems(
                arrayOf(
                    "Editar",
                    "Eliminar",
                    "Ver pólizas"
                )
            ) { _, opcion ->

                when (opcion) {
                    0 -> editarCliente(cliente)
                    1 -> eliminarCliente(cliente)
                    2 -> mostrarPolizas(cliente.id)
                }
            }
            .show()
    }

    private fun editarCliente(cliente: Cliente) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
        }

        val nombre = campo("Nombre")
        val telefono = campo("Teléfono")
        val email = campo("Email")

        nombre.setText(cliente.nombre)
        telefono.setText(cliente.telefono)
        email.setText(cliente.email)

        root.addView(nombre)
        root.addView(telefono)
        root.addView(email)

        AlertDialog.Builder(this)
            .setTitle("Editar cliente")
            .setView(root)
            .setPositiveButton("Guardar") { _, _ ->

                cliente.nombre = nombre.text.toString().trim()
                cliente.telefono = telefono.text.toString().trim()
                cliente.email = email.text.toString().trim()

                guardarDatos()
                mostrarClientes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCliente(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cliente")
            .setMessage(
                "¿Quieres eliminar a ${cliente.nombre}?\n\n" +
                        "También se eliminarán sus pólizas."
            )
            .setPositiveButton("Eliminar") { _, _ ->

                clientes.removeAll {
                    it.id == cliente.id
                }

                polizas.removeAll {
                    it.clienteId == cliente.id
                }

                guardarDatos()
                mostrarClientes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============================================================
    // POLIZAS
    // ============================================================

    private fun mostrarPolizas(clienteId: Long? = null) {
        val root = crearContenedor()

        root.addView(titulo("Pólizas"))

        root.addView(
            boton("➕ Nueva póliza") {
                nuevaPoliza()
            }
        )

        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val datos = if (clienteId == null) {
            polizas
        } else {
            polizas.filter {
                it.clienteId == clienteId
            }
        }

        datos.forEach { poliza ->

            val cliente = clientes.find {
                it.id == poliza.clienteId
            }

            val caja = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }

            val info = TextView(this).apply {
                text =
                    "Póliza: ${poliza.numero}\n" +
                            "Tipo: ${poliza.tipo}\n" +
                            "Cliente: ${cliente?.nombre ?: "Sin cliente"}\n" +
                            "Prima: ${"%.2f".format(poliza.prima)} €"

                textSize = 17f
            }

            caja.addView(info)

            caja.setOnClickListener {
                menuPoliza(poliza)
            }

            lista.addView(caja)
        }

        if (lista.childCount == 0) {
            val vacio = TextView(this).apply {
                text = "No hay pólizas."
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }

            lista.addView(vacio)
        }

        root.addView(lista)

        root.addView(
            boton("← Volver") {
                mostrarInicio()
            }
        )

        mostrarVista(root)
    }

    private fun nuevaPoliza() {
        if (clientes.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin clientes")
                .setMessage("Primero debes crear un cliente.")
                .setPositiveButton("Crear cliente") { _, _ ->
                    nuevoCliente()
                }
                .setNegativeButton("Cancelar", null)
                .show()

            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
        }

        val numero = campo("Número de póliza")
        val tipo = campo("Tipo de póliza")
        val prima = campo("Prima anual (€)")

        val spinner = Spinner(this)

        val nombres = clientes.map {
            it.nombre
        }

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            nombres
        )

        root.addView(numero)
        root.addView(tipo)
        root.addView(prima)
        root.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Nueva póliza")
            .setView(root)
            .setPositiveButton("Guardar") { _, _ ->

                val cliente = clientes[spinner.selectedItemPosition]

                val importe = prima.text.toString()
                    .replace(",", ".")
                    .toDoubleOrNull() ?: 0.0

                polizas.add(
                    Poliza(
                        id = System.currentTimeMillis(),
                        clienteId = cliente.id,
                        numero = numero.text.toString().trim(),
                        tipo = tipo.text.toString().trim(),
                        prima = importe
                    )
                )

                guardarDatos()
                mostrarPolizas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun menuPoliza(poliza: Poliza) {
        AlertDialog.Builder(this)
            .setTitle("Póliza ${poliza.numero}")
            .setItems(
                arrayOf(
                    "Editar",
                    "Eliminar"
                )
            ) { _, opcion ->

                when (opcion) {
                    0 -> editarPoliza(poliza)
                    1 -> eliminarPoliza(poliza)
                }
            }
            .show()
    }

    private fun editarPoliza(poliza: Poliza) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 10, 30, 10)
        }

        val numero = campo("Número")
        val tipo = campo("Tipo")
        val prima = campo("Prima anual (€)")

        numero.setText(poliza.numero)
        tipo.setText(poliza.tipo)
        prima.setText(poliza.prima.toString())

        root.addView(numero)
        root.addView(tipo)
        root.addView(prima)

        AlertDialog.Builder(this)
            .setTitle("Editar póliza")
            .setView(root)
            .setPositiveButton("Guardar") { _, _ ->

                poliza.numero = numero.text.toString().trim()
                poliza.tipo = tipo.text.toString().trim()

                poliza.prima =
                    prima.text.toString()
                        .replace(",", ".")
                        .toDoubleOrNull() ?: 0.0

                guardarDatos()
                mostrarPolizas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPoliza(poliza: Poliza) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar póliza")
            .setMessage("¿Quieres eliminar esta póliza?")
            .setPositiveButton("Eliminar") { _, _ ->

                polizas.removeAll {
                    it.id == poliza.id
                }

                guardarDatos()
                mostrarPolizas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============================================================
    // CARTERA
    // ============================================================

    private fun mostrarCartera() {
        val root = crearContenedor()

        root.addView(titulo("💼 Cartera"))

        val totalClientes = clientes.size
        val totalPolizas = polizas.size
        val totalPrimas = polizas.sumOf {
            it.prima
        }

        val resumen = TextView(this).apply {
            text =
                "Clientes: $totalClientes\n\n" +
                        "Pólizas: $totalPolizas\n\n" +
                        "Primas anuales: ${"%.2f".format(totalPrimas)} €"

            textSize = 21f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 50)
        }

        root.addView(resumen)

        root.addView(
            boton("📋 Ver pólizas") {
                mostrarPolizas()
            }
        )

        root.addView(
            boton("← Volver") {
                mostrarInicio()
            }
        )

        mostrarVista(root)
    }

    // ============================================================
    // ESCANER
    // ============================================================

    private fun mostrarEscaner() {
        val root = crearContenedor()

        root.addView(titulo("📷 Escáner"))

        val texto = TextView(this).apply {
            text =
                "Escáner de documentos\n\n" +
                        "Esta V1 deja preparada la sección para " +
                        "integrar la cámara y el OCR en la siguiente versión."

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

    // ============================================================
    // CONFIGURACION
    // ============================================================

    private fun mostrarConfiguracion() {
        val root = crearContenedor()

        root.addView(titulo("⚙️ Configuración"))

        root.addView(
            boton("🗑️ Borrar todos los datos") {

                AlertDialog.Builder(this)
                    .setTitle("Borrar datos")
                    .setMessage(
                        "Se eliminarán todos los clientes y pólizas. " +
                                "Esta acción no se puede deshacer."
                    )
                    .setPositiveButton("Borrar") { _, _ ->

                        clientes.clear()
                        polizas.clear()

                        prefs.edit().clear().apply()

                        Toast.makeText(
                            this,
                            "Datos eliminados",
                            Toast.LENGTH_SHORT
                        ).show()

                        mostrarInicio()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        root.addView(
            boton("ℹ️ Información") {

                AlertDialog.Builder(this)
                    .setTitle("RgaPro")
                    .setMessage(
                        "RgaPro V1\n\n" +
                                "Gestión local de clientes, pólizas y cartera.\n\n" +
                                "Los datos se almacenan en el dispositivo."
                    )
                    .setPositiveButton("Aceptar", null)
                    .show()
            }
        )

        root.addView(
            boton("← Volver") {
                mostrarInicio()
            }
        )

        mostrarVista(root)
    }

    // ============================================================
    // BOTON ATRÁS
    // ============================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        mostrarInicio()
    }
}
```
