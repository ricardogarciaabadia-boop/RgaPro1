from pathlib import Path
import re

p=Path('app/src/main/assets/prototype/index_v3.html')
s=p.read_text(encoding='utf-8')

# Replace the OCR/document selector block with two explicit entry points.
pattern=r'<section id="ocr" class="screen">.*?</section>\n<section id="alarms"'
replacement='''<section id="ocr" class="screen"><div class="title2">OCR</div><div class="card"><div class="eyebrow">CAPTURA CON CÁMARA</div><div class="info">Usa este apartado para fotografiar un DNI/NIE. El DNI es el único documento con anverso y reverso.</div><div class="tools" style="margin-top:10px"><button class="primary" onclick="startDniOcr()">📷 Cámara anverso</button><button class="secondary" onclick="startDniReverse()">↩️ Cámara reverso</button></div></div><div class="card"><div class="eyebrow">LECTURA DE DOCUMENTOS</div><div class="info">Fotografía una página o añade varias páginas de una póliza. También admite PDF/JPEG. Se identifica el producto, se extraen solo los datos necesarios y se intenta asociar al cliente correcto.</div><div class="tools" style="margin-top:10px"><button class="primary" onclick="startPolicyPhoto()">📷 Documento · 1 o varias páginas</button><button class="secondary" onclick="openFile()">📁 Cargar PDF / JPG</button><button class="secondary" onclick="finishDocument()">✅ Finalizar documento</button></div></div><div class="card"><div id="ocrStatus" class="notice">Selecciona una de las dos funciones anteriores. El OCR no mezclará datos de DNI con pólizas.</div><div id="ocrFields"></div><div class="tools" style="margin-top:10px"><button class="secondary" onclick="enableOcrEdit()">✏️ Editar datos</button><button class="primary" onclick="saveOcr()">💾 Guardar</button><button class="danger" onclick="clearOcr()">Descartar</button></div></div></section>\n<section id="documents" class="screen"><div class="title2">Documentos</div><div class="card"><div class="eyebrow">DOCUMENTO DIGITALIZADO</div><div class="info">Aquí se suben PDF o JPEG que ya están digitalizados. RgaPro los clasifica, extrae la información necesaria y los guarda en el cliente correcto.</div><button class="primary full" onclick="openFile('DOCUMENTO')">📁 Subir PDF / JPEG</button></div><div class="card"><div id="documentStatus" class="notice">Sin documento seleccionado.</div><div id="documentFields"></div></div></section>\n<section id="alarms"'''
s2=re.sub(pattern,replacement,s,flags=re.S)
if s2==s:
    raise SystemExit('OCR section anchor not found')
s=s2

# Add Documents navigation entry once.
s=s.replace('<button id="n-ocr" onclick="show(\'ocr\')">OCR</button><button id="n-alarms"', '<button id="n-ocr" onclick="show(\'ocr\')">OCR</button><button id="n-documents" onclick="show(\'documents\')">Documentos</button><button id="n-alarms"',1)

# Remove black UI bottom indicator coming from our page styles; keep system gesture/navigation bar only.
s=s.replace('.footer{text-align:center;color:var(--muted);font-size:11px;padding:10px 0 30px}', '.footer{text-align:center;color:var(--muted);font-size:11px;padding:10px 0 18px}')
s=s.replace('body{margin:0;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:var(--bg);color:var(--ink)}', 'body{margin:0;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:var(--bg);color:var(--ink);padding-bottom:env(safe-area-inset-bottom,0);}')

# Explicit OCR modes and reset state.
s=s.replace("const DB='rgapro_v3';let state=loadState(),currentClientId=null,currentPolicyId=null,ocrType='DNI'", "const DB='rgapro_v3';let state=loadState(),currentClientId=null,currentPolicyId=null,ocrType='DNI',documentMode='OCR'",1)

# Add helpers before existing OCR functions if they exist.
insert='''\nfunction startDniOcr(){documentMode='OCR';setOcrType('DNI');ocrData=emptyOcr();ocrEditing=false;document.getElementById('ocrStatus').textContent='DNI/NIE: toma primero el anverso y después el reverso.';show('ocr');if(window.RgaProCamera)RgaProCamera.capture('front')}\nfunction startDniReverse(){documentMode='OCR';setOcrType('DNI');show('ocr');if(window.RgaProCamera)RgaProCamera.capture('reverse')}\nfunction startPolicyPhoto(){documentMode='POLIZA';setOcrType('POLIZA');ocrData=emptyOcr();ocrEditing=false;document.getElementById('ocrStatus').textContent='Póliza: añade 1 o varias páginas. Al finalizar se clasifica el producto y se extraen los datos necesarios.';show('ocr');if(window.RgaProCamera)RgaProCamera.capture('page')}\nfunction finishDocument(){document.getElementById('ocrStatus').textContent='Documento finalizado. Revisa la clasificación y los datos antes de guardar.'}\nfunction openDocuments(){show('documents')}\n'''
# insert before first function show
idx=s.find('function show(')
if idx<0: raise SystemExit('show function not found')
s=s[:idx]+insert+s[idx:]

p.write_text(s,encoding='utf-8')
print('OCR redesign applied')
