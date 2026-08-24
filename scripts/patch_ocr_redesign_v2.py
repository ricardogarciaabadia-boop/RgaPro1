from pathlib import Path
import re

p=Path('app/src/main/assets/prototype/index_v3.html')
s=p.read_text(encoding='utf-8')

# Replace OCR section with a simple two-entry document model: Camera OCR and Digital documents.
pattern=r'<section id="ocr" class="screen">.*?</section>\n<section id="alarms"'
replacement=r'''<section id="ocr" class="screen"><div class="title2">OCR</div>
<div class="card"><div class="eyebrow">ESCANEAR DOCUMENTO</div>
<div class="grid" style="margin-top:10px">
<button class="primary" onclick="setOcrType('DNI');show('ocr')">📷 DNI / NIE<br><span class="small" style="color:#fff">Cámara: anverso + reverso</span></button>
<button class="secondary" onclick="setOcrType('DOCUMENTO');show('ocr')">📄 Documento<br><span class="small">1 o muchas páginas</span></button>
</div>
<div class="info" style="margin-top:12px">DNI: usa la cámara para anverso y reverso. Los demás documentos pueden tener una o muchas páginas.</div>
<div class="ocrbox" style="margin-top:12px">
<img id="docimg" class="docimg" alt="Documento">
<div class="tools" style="margin-top:10px">
<button id="btnFront" class="primary" onclick="takeFront()">📷 Cámara · anverso</button>
<button id="btnReverse" class="secondary" onclick="takeReverse()">↩️ Cámara · reverso</button>
<button id="btnPage" class="secondary" onclick="takePage()">📷 Tomar página</button>
<button class="secondary" onclick="openFile()">📁 Añadir PDF / JPG</button>
<button class="secondary" onclick="rotate()">↻ Rotar</button>
<button class="secondary" onclick="toggleEnhance()">✨ Mejorar</button>
<button class="primary" onclick="finishDocument()">✅ Finalizar y clasificar</button>
</div>
<input id="file" type="file" accept="image/*,.pdf,application/pdf" multiple style="display:none" onchange="loadFile(event)">
</div></div>
<div class="card"><div id="ocrStatus" class="notice">El documento se clasificará y se extraerán solo los datos necesarios.</div><div id="ocrFields"></div>
<div class="tools" style="margin-top:10px"><button class="secondary" onclick="enableOcrEdit()">✏️ Editar datos</button><button class="primary" onclick="saveOcr()">💾 Guardar en cliente</button><button class="danger" onclick="clearOcr()">Descartar</button></div></div></section>
<section id="documents" class="screen"><div class="title2">Documentos</div><div class="card"><div class="eyebrow">DOCUMENTOS DIGITALIZADOS</div><div class="info">Sube cualquier PDF o JPEG ya digitalizado. Se clasificará, se leerán los datos necesarios y se buscará automáticamente el cliente correcto.</div><button class="primary full" style="margin-top:10px" onclick="setOcrType('DOCUMENTO');show('ocr');openFile()">📁 Subir PDF / JPEG</button></div><div class="card"><div id="documentList" class="list"></div></div></section>
<section id="alarms"'''
s,n=re.subn(pattern,replacement,s,flags=re.S)
if n!=1: raise SystemExit(f'OCR section replacement failed: {n}')

# Add Documents tab in navigation.
s=s.replace('<button id="n-alarms" onclick="show(\'alarms\')">Alarmas</button>','<button id="n-documents" onclick="show(\'documents\')">Documentos</button><button id="n-alarms" onclick="show(\'alarms\')">Alarmas</button>')

# Remove app-level black bottom line caused by footer padding; never draw a horizontal bar ourselves.
s=s.replace('.footer{text-align:center;color:var(--muted);font-size:11px;padding:10px 0 30px}', '.footer{text-align:center;color:var(--muted);font-size:11px;padding:10px 0 24px;border:0;box-shadow:none}')
s=s.replace('border-bottom:1px solid var(--line)', 'border-bottom:1px solid var(--line)')

# Add helper JS to show correct controls by mode, page accumulation, and classify/save.
insert=r'''
<script>
let ocrPages=[];
function setOcrType(t){ocrType=t; if(t==='DNI'){frontRaw='';reverseRaw='';ocrPages=[];setTimeout(updateOcrMode,0)}else{frontRaw='';reverseRaw='';ocrPages=[];setTimeout(updateOcrMode,0)}}
function updateOcrMode(){
 const d=ocrType==='DNI';
 const fr=document.getElementById('btnFront'),rv=document.getElementById('btnReverse'),pg=document.getElementById('btnPage');
 if(fr)fr.textContent=d?'📷 Cámara · anverso':'📷 Tomar página';
 if(rv)rv.style.display=d?'':'none';
 if(pg)pg.style.display=d?'none':'';
 const st=document.getElementById('ocrStatus');
 if(st)st.textContent=d?'DNI/NIE: captura anverso y después reverso. Solo se guardan los datos necesarios.':'Documento: añade una o muchas páginas, o carga PDF/JPG. Se clasifica por producto y se guarda en el cliente correcto.';
}
function takePage(){RgaProCamera&&RgaProCamera.capture('page')}
function finishDocument(){if(ocrType==='DNI'){if(!frontRaw||!reverseRaw)return alert('Captura primero anverso y reverso del DNI.')} else if(!ocrPages.length && !ocrData.sourceData)return alert('Añade al menos una página o un PDF/JPG.'); classifyAndRender();}
function classifyAndRender(){let raw=ocrData.raw||'';if(ocrType!=='DNI'){let u=norm(raw);if(/\b(decesos|asistencia familiar|funer|fallecimiento)\b/.test(u))ocrData.policyType='Decesos';else if(/\b(vida|fallecimiento)\b/.test(u))ocrData.policyType='Vida';else if(/\b(ahorro|inversi[oó]n|rentabilidad)\b/.test(u))ocrData.policyType='Ahorro';else if(/\b(auto|autom[oó]vil|veh[ií]culo)\b/.test(u))ocrData.policyType='Auto';else if(/\b(hogar|vivienda)\b/.test(u))ocrData.policyType='Hogar';else if(/\b(salud|m[eé]dico)\b/.test(u))ocrData.policyType='Salud';else ocrData.policyType=ocrData.policyType||'Póliza';}
renderOcrFields();
}
</script>'''
s=s.replace('</body></html>',insert+'</body></html>')
p.write_text(s,encoding='utf-8')
print('OCR redesign v2 applied')
