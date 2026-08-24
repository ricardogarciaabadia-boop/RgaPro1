from pathlib import Path

manifest = Path('app/src/main/AndroidManifest.xml')
text = manifest.read_text(encoding='utf-8')
text = text.replace('android:icon="@drawable/ic_rgapro"', 'android:icon="@drawable/rgapro_launcher"')
text = text.replace('android:roundIcon="@drawable/ic_rgapro"', 'android:roundIcon="@drawable/rgapro_launcher"')
manifest.write_text(text, encoding='utf-8')

icon = Path('app/src/main/res/drawable/rgapro_launcher.xml')
if not icon.exists():
    raise SystemExit('rgapro_launcher.xml missing')

print('Launcher RgaPro fijado al recurso rgapro_launcher.xml')
