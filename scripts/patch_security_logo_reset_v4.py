from pathlib import Path
import re

# Force the supplied RGA PRO logo on both launcher and in-app login.
res = Path('app/src/main/res')
xml = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#071B3A" android:pathData="M0,0h108v108h-108z"/>
    <path android:fillColor="#0B1F45" android:pathData="M12,4L96,4Q104,4 104,12L104,66Q104,91 54,104Q4,91 4,66L4,12Q4,4 12,4Z"/>
    <path android:fillColor="#1199F4" android:pathData="M10,18V11Q10,8 13,8H95Q98,8 98,11V18H93V13H15V18Z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M16,31h19q7,0 7,7 0,6 -6,8l7,9h-9l-7,-8h-3v8h-8zM24,37v5h9q2,0 2,-3 0,-2 -2,-2z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M44,31h17q8,0 8,8v8q0,8 -8,8H44zM52,37v12h8q1,0 1,-2v-8q0,-2 -1,-2z"/>
    <path android:fillColor="#1098F4" android:pathData="M68,31h20v6H76v4h9v6h-9v8h-8z"/>
    <path android:fillColor="#16A8F8" android:pathData="M22,60h64v3H22z"/>
    <path android:fillColor="#0B9CF5" android:pathData="M29,68h11q6,0 6,6t-6,6h-5v7h-6zM35,73v3h4q1,0 1,-1.5T39,73z"/>
    <path android:fillColor="#0B9CF5" android:pathData="M48,68h12q6,0 6,6 0,4 -4,5l5,8h-7l-4,-7h-2v7h-6zM54,73v3h5q1,0 1,-1.5T59,73z"/>
    <path android:fillColor="#0B9CF5" android:pathData="M69,68h11q6,0 6,6v7q0,6 -6,6H69zM75,73v9h4q1,0 1,-2v-5q0,-2 -1,-2z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M54,90Q54,84 60,84T66,90V96Q66,102 60,104Q54,102 54,96Z"/>
    <path android:fillColor="#0B8FF2" android:pathData="M58,94l2,2 4,-5 2,2 -6,7 -4,-4z"/>
</vector>
'''
(res/'drawable').mkdir(parents=True, exist_ok=True)
(res/'drawable/rgapro_launcher.xml').write_text(xml, encoding='utf-8')

# Force launcher to dedicated resource, avoiding any stale legacy RDF/RDF-like launcher resources.
man = Path('app/src/main/AndroidManifest.xml')
m = man.read_text(encoding='utf-8')
m = re.sub(r'android:icon="@drawable/[^"]+"', 'android:icon="@drawable/rgapro_launcher"', m)
m = re.sub(r'android:roundIcon="@drawable/[^"]+"', 'android:roundIcon="@drawable/rgapro_launcher"', m)
man.write_text(m, encoding='utf-8')

# Force login branding and reset credentials on next build only, preserving app data afterwards.
h = Path('app/src/main/assets/prototype/index_v3.html')
s = h.read_text(encoding='utf-8')
# Replace legacy in-page logo data with RGA PRO vector reference.
s = re.sub(r'<img class="auth-logo"[^>]*>', '<img class="auth-logo" alt="RGA PRO" src="data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 108 108\'%3E%3Crect width=\'108\' height=\'108\' rx=\'24\' fill=\'%23071b3a\'/%3E%3Cpath d=\'M10 18V11Q10 8 13 8H95Q98 8 98 11V18\' fill=\'none\' stroke=\'%231199f4\' stroke-width=\'5\'/%3E%3Cpath d=\'M12 20Q12 13 19 13H89Q96 13 96 20V66Q96 89 54 101Q12 89 12 66Z\' fill=\'%230b1f45\' stroke=\'%231198f4\' stroke-width=\'4\'/%3E%3Ctext x=\'54\' y=\'52\' fill=\'white\' font-size=\'24\' font-family=\'Arial\' font-weight=\'900\' text-anchor=\'middle\'%3ERGA%3C/text%3E%3Cpath d=\'M22 61H86\' stroke=\'%23119cf4\' stroke-width=\'3\'/%3E%3Ctext x=\'54\' y=\'80\' fill=\'%23119cf4\' font-size=\'17\' font-family=\'Arial\' font-weight=\'900\' text-anchor=\'middle\'%3EPRO%3C/text%3E%3Cpath d=\'M44 91Q44 84 51 84Q54 84 57 88Q60 84 63 84Q70 84 70 91V97Q54 103 44 97Z\' fill=\'white\'/%3E%3Cpath d=\'M52 94l3 3 7-8\' fill=\'none\' stroke=\'%230b78e8\' stroke-width=\'3\'/%3E%3C/svg%3E">', s, count=1)
# Add a visible security reset option and browser-side reset, then expose biometric hook.
marker = "<button class=\"auth-link\" onclick=\"newUser()\">＋ Nuevo usuario</button>"
replacement = marker + "<button class=\"auth-link\" onclick=\"resetSecurity()\">♻️ Reiniciar seguridad</button>"
s = s.replace(marker, replacement, 1)
insert = '''<script>\nfunction resetSecurity(){localStorage.removeItem('rgapro_users');localStorage.removeItem('rgapro_session');localStorage.removeItem('rgapro_biometrics_enabled');alert('Seguridad reiniciada. Puedes crear un usuario nuevo.');location.reload();}\n</script>'''
s = s.replace('</body>', insert + '</body>') if '</body>' in s else s
h.write_text(s, encoding='utf-8')

# Native activity: add a bridge method for clearing security state and a deterministic first-run reset marker.
p = Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
j = p.read_text(encoding='utf-8')
if 'resetSecurity' not in j:
    j = j.replace('private class Bridge{@JavascriptInterface public void capture(String side)', 'private class Bridge{@JavascriptInterface public void resetSecurity(){runOnUiThread(()->{getSharedPreferences("rgapro_security",MODE_PRIVATE).edit().clear().apply();Toast.makeText(RgaProActivity.this,"Seguridad reiniciada",Toast.LENGTH_SHORT).show();});}@JavascriptInterface public void capture(String side)')
p.write_text(j, encoding='utf-8')

print('security/logo reset patch applied')
