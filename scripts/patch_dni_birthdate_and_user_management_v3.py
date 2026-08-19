from pathlib import Path

BASE = Path(__file__).with_name("patch_dni_birthdate_and_user_management_v2.py")
source = BASE.read_text(encoding="utf-8")

fallback = r'''raise SystemExit('home user menu anchor not found')'''
replacement = r'''# Último recurso: localizar home() por balanceo de llaves e insertar el botón
# justo antes de su llave de cierre. Esto no depende de nombres, espacios ni del
# orden concreto de los botones del menú.
users_line = 'Button users=sideButton("👥  Usuarios de la aplicación"); users.setOnClickListener(v->users()); side.addView(users,new LinearLayout.LayoutParams(-1,dp(68)));'
home_start = s.find('void home(){')
if home_start < 0:
    home_start = s.find('void home() {')
if home_start >= 0:
    brace = s.find('{', home_start)
    if brace >= 0:
        depth = 0
        close = -1
        for pos in range(brace, len(s)):
            ch = s[pos]
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    close = pos
                    break
        if close >= 0:
            s = s[:close] + users_line + '\n' + s[close:]
        else:
            raise SystemExit('home method closing brace not found')
    else:
        raise SystemExit('home method opening brace not found')
else:
    raise SystemExit('home method not found')'''

if fallback not in source:
    raise SystemExit("expected v2 fallback anchor not found")

source = source.replace(fallback, replacement, 1)
exec(compile(source, str(BASE), "exec"), {"__name__": "__main__", "__file__": str(BASE)})
