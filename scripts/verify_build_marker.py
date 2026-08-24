from pathlib import Path
p=Path('app/build/outputs/apk/debug/app-debug.apk')
assert p.exists(), 'APK not generated'
print(f'APK ready: {p} ({p.stat().st_size} bytes)')
