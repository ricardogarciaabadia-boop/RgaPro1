```kotlin
package com.rgapro1.ocaso

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val titulo = TextView(this).apply {
            text = "RgaPro"
            textSize = 32f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val subtitulo = TextView(this).apply {
            text = "Mi Cartera Ocaso"
            textSize = 20f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        layout.addView(titulo)
        layout.addView(subtitulo)

        setContentView(layout)
    }
}
```
