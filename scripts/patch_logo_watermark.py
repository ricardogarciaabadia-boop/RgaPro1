from pathlib import Path

path = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = path.read_text(encoding='utf-8')

helper = '''
    /** Adds the RgaPro logo as a subtle, centered watermark behind each main screen. */
    private View withWatermark(View page) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_rgapro);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setAlpha(0.075f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(250), dp(250), Gravity.CENTER);
        frame.addView(logo, lp);
        frame.addView(page, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }
'''

if 'private View withWatermark(View page)' not in s:
    marker = '    @Override public void onCreate(Bundle b)'
    if marker not in s:
        raise SystemExit('No se encontró el punto de inserción de MainActivity')
    s = s.replace(marker, helper + '\n' + marker, 1)

s = s.replace('setContentView(l);}', 'setContentView(withWatermark(l));}', 2)
s = s.replace('setContentView(root);\n    }', 'setContentView(withWatermark(root));\n    }', 1)

path.write_text(s, encoding='utf-8')
print('Logo RgaPro aplicado como fondo/marca de agua en acceso y pantalla principal')
