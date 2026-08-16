package com.rgapro1.ocaso

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.TextView

class MainActivity : Activity() {

```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val texto = TextView(this)

    texto.text = "RgaPro\n\nMi Cartera Ocaso"
    texto.textSize = 28f
    texto.setTextColor(Color.BLACK)
    texto.setBackgroundColor(Color.WHITE)
    texto.setPadding(40, 80, 40, 40)

    setContentView(texto)
}
```

}
