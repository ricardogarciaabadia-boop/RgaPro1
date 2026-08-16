package com.rgapro1.ocaso

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val texto = TextView(this).apply {
            text = "Mi Cartera Ocaso"
            textSize = 28f
            setPadding(40, 80, 40, 40)
        }

        setContentView(texto)
    }
}
