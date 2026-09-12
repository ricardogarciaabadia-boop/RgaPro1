from pathlib import Path

# The launcher renders the complete application shell, not the reduced OCR-only v3 screen.
p = Path('app/src/main/assets/prototype/index.html')
s = p.read_text(encoding='utf-8')
required = [
    'onclick="show(\'home\')">Inicio</button>',
    'onclick="show(\'clients\')">Clientes</button>',
    'id="n-ocr"',
    'id="btnDocumentCamera"',
    'RgaProCamera.capture(\'front\')',
    'RgaProCamera.capture(\'reverse\')',
    'RgaProCamera.pickPdf()',
    'function setOcrResult(data)',
    "const KEY='rgapro_clients_v2'",
]
for x in required:
    assert x in s, f'Missing required application/OCR UI: {x}'

# The obsolete reduced renderer must not be the launcher target.
rga = Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java').read_text(encoding='utf-8')
assert 'file:///android_asset/prototype/index.html' in rga
assert 'file:///android_asset/prototype/index_v3.html' not in rga

print('Visible RgaPro/OCR source verification OK')
