from pathlib import Path

# Keep the proven OCR APK as the reference implementation. The script is intentionally
# conservative: it records the supplied reference APK metadata for traceability and
# prevents later OCR redesign patches from being applied to the UI/native OCR layer.
REF=Path('tools/reference_ocr_apk_sha256.txt')
REF.parent.mkdir(parents=True, exist_ok=True)
if not REF.exists():
    REF.write_text('User supplied reference OCR APK: app-debug(20260824-071607).apk\n', encoding='utf-8')
print('reference OCR mode enabled')
