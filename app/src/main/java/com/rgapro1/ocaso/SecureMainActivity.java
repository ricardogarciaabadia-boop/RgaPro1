package com.rgapro1.ocaso;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Production launcher that keeps the legacy MainActivity UI intact while routing
 * sensitive local state through Android Keystore-backed stores.
 *
 * The same approved RGA PRO brand mark is also injected as the application
 * header so the launcher icon and in-app identity cannot drift apart.
 */
public class SecureMainActivity extends MainActivity {
    private SharedPreferences secureLocalPreferences;
    private boolean brandingInProgress;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (!"rgapro_local".equals(name)) {
            return super.getSharedPreferences(name, mode);
        }
        if (secureLocalPreferences == null) {
            SharedPreferences delegate = super.getSharedPreferences(name, mode);
            secureLocalPreferences = new SecurePinPreferences(
                    delegate,
                    new SecurePinStore(getApplicationContext()),
                    new SecureDataStore(getApplicationContext())
            );
        }
        return secureLocalPreferences;
    }

    @Override
    public void setContentView(View view) {
        if (brandingInProgress) {
            super.setContentView(view);
            return;
        }

        brandingInProgress = true;
        try {
            LinearLayout shell = new LinearLayout(this);
            shell.setOrientation(LinearLayout.VERTICAL);
            shell.setBackgroundColor(Color.rgb(247, 249, 252));

            LinearLayout brand = new LinearLayout(this);
            brand.setOrientation(LinearLayout.HORIZONTAL);
            brand.setGravity(Gravity.CENTER_VERTICAL);
            brand.setPadding(dp(14), dp(6), dp(14), dp(6));
            brand.setBackgroundColor(Color.rgb(7, 26, 58));

            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.ic_rgapro);
            logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            brand.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));

            TextView name = new TextView(this);
            name.setText("RGA PRO");
            name.setTextColor(Color.WHITE);
            name.setTextSize(18);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setGravity(Gravity.CENTER_VERTICAL);
            name.setPadding(dp(8), 0, 0, 0);
            brand.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1));

            shell.addView(brand, new LinearLayout.LayoutParams(-1, dp(56)));
            shell.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));
            super.setContentView(shell);
        } finally {
            brandingInProgress = false;
        }
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        setContentView(view);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClientAutoLinker.start(this);
    }
}
