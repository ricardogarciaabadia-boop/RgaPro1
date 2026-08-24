from pathlib import Path

p = Path('app/src/main/assets/prototype/index_v3.html')
s = p.read_text(encoding='utf-8')
required = [
    '<button id="btnDni"',
    '<button id="btnDocs"',
    'Cámara para documentos',
    'JPEG / PDF',
    'function openDni()',
    'function openDocuments()',
]
for x in required:
    assert x in s, f'Missing required OCR UI: {x}'
for x in ['Detectar automáticamente', 'ocrTypePol', 'onclick="rotate()"', 'toggleEnhance()', 'Tomar reverso', 'Cargar PDF / JPG']:
    assert x not in s, f'Legacy OCR UI still present: {x}'
print('Visible OCR source verification OK')
