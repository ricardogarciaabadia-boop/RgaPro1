from pathlib import Path

p = Path('.github/workflows/crear-apk-limpio.yml')
s = p.read_text(encoding='utf-8')
s = s.replace('          python3 scripts/patch_user_management_final.py || true\n','')
# The targeted fix operates on the resulting Java source after all other patches.
p.write_text(s, encoding='utf-8')
print('Disabled malformed legacy user-management patch; targeted fix remains active')
