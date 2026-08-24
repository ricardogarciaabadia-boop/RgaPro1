from pathlib import Path
import re

# Finalize real biometric bridge after all legacy patches.
p=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s=p.read_text(encoding='utf-8')
bridge=r'''private class Bridge\{.*?\}\n    private void pickDocument'''
new='''private class Bridge{\n        @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}\n        @JavascriptInterface public void pickPdf(){runOnUiThread(RgaProActivity.this::pickDocument);}\n        @JavascriptInterface public boolean biometricAvailable(){return BiometricGate.canUse(RgaProActivity.this);}\n        @JavascriptInterface public void biometricUnlock(){runOnUiThread(()->BiometricGate.prompt(RgaProActivity.this,()->web.evaluateJavascript("window.onBiometricSuccess&&window.onBiometricSuccess();",null),()->web.evaluateJavascript("window.onBiometricError&&window.onBiometricError();",null)));}\n        @JavascriptInterface public void resetSecurity(){runOnUiThread(()->getSharedPreferences("rgapro_security",MODE_PRIVATE).edit().clear().apply());}\n    }\n    private void pickDocument'''
if re.search(bridge,s,re.S): s=re.sub(bridge,new,s,flags=re.S)
p.write_text(s,encoding='utf-8')

h=Path('app/src/main/assets/prototype/index_v3.html')
s=h.read_text(encoding='utf-8')
# Make in-app login overlay guaranteed, add biometric button, and ensure new-user/reset actions.
if 'id="rgBiometricFinal"' not in s:
    overlay='''<div id="rgFinalAuth" style="display:flex;position:fixed;inset:0;z-index:999999;background:#071b3a;align-items:center;justify-content:center;padding:18px"><div style="width:min(430px,100%);background:#0b1f45;border:1px solid #ffffff33;border-radius:24px;padding:22px;color:#fff"><div style="text-align:center;font-size:30px;font-weight:900">RgaPro</div><div style="text-align:center;color:#bfd0e8;margin:6px 0 14px">Acceso seguro</div><input id="rgFinalUser" placeholder="Usuario" style="width:100%;padding:13px;border-radius:12px;border:1px solid #ffffff33;background:#061633;color:#fff;margin:6px 0"><input id="rgFinalPin" type="password" inputmode="numeric" maxlength="6" placeholder="PIN de 6 dígitos" style="width:100%;padding:13px;border-radius:12px;border:1px solid #ffffff33;background:#061633;color:#fff;margin:6px 0"><button style="width:100%;padding:13px;border:0;border-radius:12px;background:#0d55c7;color:#fff;font-weight:900;margin-top:6px" onclick="rgFinalLogin()">Entrar</button><button id="rgBiometricFinal" style="display:none;width:100%;padding:13px;border:0;border-radius:12px;background:#e9f1ff;color:#0d55c7;font-weight:900;margin-top:8px" onclick="rgFinalBio()">🔐 Acceso biométrico</button><button style="width:100%;padding:13px;border:1px solid #168cff;border-radius:12px;background:transparent;color:#66b8ff;font-weight:900;margin-top:8px" onclick="rgFinalNewUser()">＋ Nuevo usuario</button><button style="width:100%;padding:13px;border:1px solid #ff9f9f;border-radius:12px;background:transparent;color:#ffd2d2;font-weight:900;margin-top:8px" onclick="rgFinalReset()">♻️ Reiniciar seguridad</button><div id="rgFinalMsg" style="text-align:center;color:#ffcf99;font-size:12px;margin-top:10px"></div></div></div><script>
function rgFinalUsers(){try{return JSON.parse(localStorage.getItem('rgapro_users')||'[]')}catch(e){return[]}}
function rgFinalOpen(){document.getElementById('rgFinalAuth').style.display='flex';document.querySelector('.app').style.display='none';}
function rgFinalClose(){document.getElementById('rgFinalAuth').style.display='none';document.querySelector('.app').style.display='block';}
function rgFinalLogin(){let u=(document.getElementById('rgFinalUser').value||'').trim(),p=(document.getElementById('rgFinalPin').value||'').trim(),a=rgFinalUsers(),ok=a.find(x=>x.user===u&&x.pin===p);if(!ok){document.getElementById('rgFinalMsg').textContent=a.length?'Usuario o PIN incorrectos.':'Crea primero un usuario.';return}localStorage.setItem('rgapro_session',u);rgFinalClose()}
function rgFinalNewUser(){let u=prompt('Nuevo usuario'),p=prompt('PIN de 6 dígitos');if(!u||!/^[0-9]{6}$/.test(p||'')){alert('Usuario y PIN válidos son obligatorios.');return}let a=rgFinalUsers();if(a.some(x=>x.user===u)){alert('Ese usuario ya existe.');return}a.push({user:u,pin:p});localStorage.setItem('rgapro_users',JSON.stringify(a));document.getElementById('rgFinalUser').value=u;document.getElementById('rgFinalPin').value=p;document.getElementById('rgFinalMsg').textContent='Usuario creado.'}
function rgFinalReset(){localStorage.removeItem('rgapro_users');localStorage.removeItem('rgapro_session');try{RgaProCamera.resetSecurity()}catch(e){}document.getElementById('rgFinalUser').value='';document.getElementById('rgFinalPin').value='';document.getElementById('rgFinalMsg').textContent='Seguridad reiniciada. Crea un usuario nuevo.'}
function rgFinalBio(){try{RgaProCamera.biometricUnlock()}catch(e){document.getElementById('rgFinalMsg').textContent='Biometría no disponible.'}}
window.onBiometricSuccess=function(){let u=(document.getElementById('rgFinalUser').value||'').trim()||localStorage.getItem('rgapro_session')||'';if(!u){let a=rgFinalUsers();if(a.length===1)u=a[0].user}if(u){localStorage.setItem('rgapro_session',u);rgFinalClose()}}
window.onBiometricError=function(){document.getElementById('rgFinalMsg').textContent='Biometría no disponible o cancelada. Usa el PIN.'}
setTimeout(function(){try{if(RgaProCamera.biometricAvailable())document.getElementById('rgBiometricFinal').style.display='block'}catch(e){};if(!localStorage.getItem('rgapro_session'))rgFinalOpen();else rgFinalOpen()},350)
</script>'''
    s=s.replace('</body>',overlay+'</body>',1)
h.write_text(s,encoding='utf-8')

# Remove legacy launcher references in manifest after all patches.
man=Path('app/src/main/AndroidManifest.xml')
m=man.read_text(encoding='utf-8')
m=re.sub(r'android:icon="@[^\"]+"','android:icon="@drawable/rgapro_launcher"',m)
m=re.sub(r'android:roundIcon="@[^\"]+"','android:roundIcon="@drawable/rgapro_launcher"',m)
man.write_text(m,encoding='utf-8')
print('final biometric/login/logo patch applied')
