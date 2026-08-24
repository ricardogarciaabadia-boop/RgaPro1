from pathlib import Path
import re

# Ensure real biometric access is available in the launcher/WebView flow.
# The prototype login uses a native bridge so Android BiometricPrompt, not JS-only state, unlocks the app.

p = Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s = p.read_text(encoding='utf-8')
if 'BiometricGate.prompt' not in s:
    s = s.replace('web.addJavascriptInterface(new Bridge(),"RgaProCamera");', 'web.addJavascriptInterface(new Bridge(),"RgaProCamera");\n        web.addJavascriptInterface(new SecurityBridge(),"RgaProSecurity");')
    marker = '    private class Bridge{'
    security = '''    private class SecurityBridge {\n        @JavascriptInterface public void biometric() {\n            runOnUiThread(() -> BiometricGate.prompt(RgaProActivity.this,\n                    () -> web.evaluateJavascript("window.biometricUnlocked && window.biometricUnlocked();", null),\n                    () -> web.evaluateJavascript("window.biometricFailed && window.biometricFailed();", null)));\n        }\n        @JavascriptInterface public boolean available() { return BiometricGate.canUse(RgaProActivity.this); }\n    }\n'''
    s = s.replace(marker, security + marker)
p.write_text(s, encoding='utf-8')

h = Path('app/src/main/assets/prototype/index_v3.html')
s = h.read_text(encoding='utf-8')
# Add biometric button if not present.
needle = '<button class="primary" onclick="loginUser()">Entrar</button>'
if 'biometricLogin' not in s and needle in s:
    s = s.replace(needle, needle + '<button id="biometricLogin" class="secondary" onclick="biometricLogin()" style="display:none">🔐 Acceso biométrico</button>')
# Add JS bridge and unlock wiring before body closes.
insert = '''\n<script>\nfunction biometricLogin(){ if(window.RgaProSecurity) RgaProSecurity.biometric(); }\nwindow.biometricUnlocked=function(){ localStorage.setItem('rgapro_session', document.getElementById('authUser').value || 'biometric'); document.body.classList.add('auth-ok'); };\nwindow.biometricFailed=function(){ alert('No se pudo validar la biometría. Usa tu PIN.'); };\n(function(){ try { if(window.RgaProSecurity && RgaProSecurity.available()) document.addEventListener('DOMContentLoaded',()=>{let b=document.getElementById('biometricLogin'); if(b)b.style.display='block';}); } catch(e){} })();\n</script>\n'''
if 'window.biometricUnlocked' not in s:
    s = s.replace('</body></html>', insert + '</body></html>')
h.write_text(s, encoding='utf-8')

# Add an explicit workflow step for the biometric feature.
w = Path('.github/workflows/build-apk.yml')
ws = w.read_text(encoding='utf-8')
step = '      - name: Activar acceso biométrico\n        run: python3 scripts/patch_biometric_access.py\n'
if 'patch_biometric_access.py' not in ws:
    ws = ws.replace('      - name: Compilar APK Debug\n', step + '      - name: Compilar APK Debug\n')
w.write_text(ws, encoding='utf-8')
print('Biometric access patch applied')
