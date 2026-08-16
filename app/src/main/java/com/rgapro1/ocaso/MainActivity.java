```java id="r9x4k2"
package com.rgapro1.ocaso;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 80, 40, 40);
        layout.setBackgroundColor(Color.WHITE);

        TextView titulo = new TextView(this);
        titulo.setText("RgaPro");
        titulo.setTextSize(32);
        titulo.setTextColor(Color.BLACK);
        titulo.setGravity(Gravity.CENTER);

        TextView subtitulo = new TextView(this);
        subtitulo.setText("Mi Cartera Ocaso");
        subtitulo.setTextSize(20);
        subtitulo.setTextColor(Color.DKGRAY);
        subtitulo.setGravity(Gravity.CENTER);
        subtitulo.setPadding(0, 20, 0, 0);

        layout.addView(titulo);
        layout.addView(subtitulo);

        setContentView(layout);
    }
}
```
