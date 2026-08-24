from pathlib import Path

p = Path('app/src/main/assets/prototype/index_v3.html')
s = p.read_text(encoding='utf-8')
required = [
    '🪪<br>DNI / NIE',
    '📄<br>DOCUMENTOS',
    'RgaProCamera.capture(\'front\')',
    'RgaProCamera.capture(\'reverse\')',
    'RgaProCamera.capture(\'document\')',
    'RgaProCamera.pickPdf()',
]
for x in required:
    assert x in s, f'Missing required OCR action/UI: {x}'
for x in [
    'Detectar automáticamente',
    'DNI / NIE</button>',
    'Póliza</button>',
    'Tomar reverso',
    'Rotar',
    'Mejorar',
    'Cargar PDF / JPG',
]:
    assert x not in s, f'Legacy OCR UI still present: {x}'
print('Visible OCR source verification OK')
