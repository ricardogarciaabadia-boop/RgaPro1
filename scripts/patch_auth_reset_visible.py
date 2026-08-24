from pathlib import Path

p = Path('app/src/main/assets/prototype/index_v3.html')
s = p.read_text(encoding='utf-8')

# Remove any legacy blocking login overlay that can prevent normal entry.
s = s.replace("body:not(.auth-ok) .app{display:none}", "body:not(.auth-ok) .app{display:block}")

# If an auth overlay exists, make it non-blocking until a user is created; add visible reset.
if '<div id="auth"' in s:
    s = s.replace('<button class="auth-link" onclick="newUser()">＋ Nuevo usuario</button>', '<button class="auth-link" onclick="newUser()">＋ Nuevo usuario</button><button class="auth-link" onclick="resetSecurity()">♻️ Reiniciar seguridad</button>')

# Ensure a deterministic local admin bootstrap and a login function that cannot leave the user trapped.
marker = "const DB='rgapro_v3';"
bootstrap = """function ensureAdmin(){try{let u=JSON.parse(localStorage.getItem('rgapro_users')||'[]');if(!Array.isArray(u))u=[];if(!u.length){u=[{user:'admin',pin:'123456'}];localStorage.setItem('rgapro_users',JSON.stringify(u));}return u;}catch(e){localStorage.setItem('rgapro_users',JSON.stringify([{user:'admin',pin:'123456'}]));return [{user:'admin',pin:'123456'}];}}\nfunction resetSecurity(){localStorage.removeItem('rgapro_users');localStorage.removeItem('rgapro_session');localStorage.removeItem('rgapro_biometrics_enabled');ensureAdmin();alert('Seguridad reiniciada. Usuario inicial: admin / PIN 123456');location.reload();}\n"""
if marker in s and 'function ensureAdmin()' not in s:
    s = s.replace(marker, bootstrap + marker, 1)

# Auto bootstrap at load.
if 'ensureAdmin();' not in s:
    s = s.replace('let state=loadState()', 'ensureAdmin();let state=loadState()', 1)

# Replace loginUser if it exists; otherwise add it.
login_impl = """function loginUser(){let u=(document.getElementById('authUser')?.value||'').trim(),p=(document.getElementById('authPin')?.value||'').trim();let ok=ensureAdmin().some(x=>x.user===u&&x.pin===p);if(!ok){alert('Usuario o PIN incorrectos. Usa admin / 123456 para la primera entrada o pulsa Reiniciar seguridad.');return false;}localStorage.setItem('rgapro_session',u);document.body.classList.add('auth-ok');document.getElementById('auth')?.remove();return true;}\nfunction newUser(){let u=prompt('Nuevo usuario');if(!u)return;let p=prompt('PIN de 6 dígitos');if(!/^\\d{6}$/.test(p||'')){alert('El PIN debe tener 6 dígitos.');return;}let users=ensureAdmin();if(users.some(x=>x.user===u)){alert('Ese usuario ya existe.');return;}users.push({user:u,pin:p});localStorage.setItem('rgapro_users',JSON.stringify(users));alert('Usuario creado. Ya puedes entrar.');}\n"""
import re
s = re.sub(r'function loginUser\(\)\{.*?\n', '', s, count=1, flags=re.S)
s = re.sub(r'function newUser\(\)\{.*?\n', '', s, count=1, flags=re.S)
insert_at = s.find('<script>')
if insert_at >= 0:
    pos = s.find('\n', insert_at) + 1
    s = s[:pos] + login_impl + s[pos:]

p.write_text(s, encoding='utf-8')
print('auth reset/login recovery patch applied')
