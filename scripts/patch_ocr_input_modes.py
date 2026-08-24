from pathlib import Path
import re

# Native OCR: DNI has front/reverse; every other document is one-or-many pages.
p = Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
s = p.read_text(encoding='utf-8')
s = s.replace('private String cameraSide="front"; private String frontRaw="",reverseRaw="";', 'private String cameraSide="front"; private String frontRaw="",reverseRaw="",documentRaw=""; private int documentPageCount=0;')
s = s.replace('cameraSide="reverse".equals(side)?"reverse":"front";', 'cameraSide="reverse".equals(side)?"reverse":("document".equals(side)||"page".equals(side)?"document":"front");')
old = re.search(r'private void deliver\(String raw,String side,String preview\)\{.*?\n    private void scanPdf', s, re.S)
if old:
    new = r'''private void deliver(String raw,String side,String preview){try{if("reverse".equals(side))reverseRaw=raw;else if("front".equals(side))frontRaw=raw;else if("document".equals(side)){documentPageCount++;documentRaw=(documentRaw+"\n"+raw).trim();}String combined; if("document".equals(side)) combined=documentRaw; else combined=(frontRaw+"\n"+reverseRaw).trim();JSONObject o=parse(combined.isEmpty()?raw:combined);o.put("side",side);o.put("pageCount",documentPageCount);o.put("preview",preview==null?"":preview);o.put("pagePreview",preview==null?"":preview);o.put("sourceData",preview==null||preview.isEmpty()?"":"data:image/jpeg;base64,"+preview);o.put("sourceName", "document".equals(side)?("captura_documento_"+documentPageCount+"_paginas.jpg"):"captura_"+(side==null?"documento":side)+".jpg");o.put("sourceType", "document".equals(side)?"image/jpeg-multipage":"image/jpeg");o.put("frontRead",!frontRaw.isEmpty());o.put("reverseRead",!reverseRaw.isEmpty());web.evaluateJavascript("window.setOcrResult("+JSONObject.quote(o.toString())+");",null);Toast.makeText(this,"document".equals(side)?("Página "+documentPageCount+" leída. Puedes añadir otra página o finalizar."):("reverse".equals(side)?"Reverso leído: revisa MRZ, DNI y fecha":"Anverso leído: ahora toma el reverso"),Toast.LENGTH_LONG).show();}catch(Exception e){err();}}
    private void scanPdf'''
    s = s[:old.start()] + new + s[old.end():]
p.write_text(s, encoding='utf-8')

# Web UI: conditional camera controls and multi-page document mode.
h = Path('app/src/main/assets/prototype/index_v3.html')
s = h.read_text(encoding='utf-8')
# Replace the OCR action row wherever the build currently has the generic controls.
pattern = r'<div class="ocrbox"><img id="docimg" class="docimg" alt="Documento"><div class="tools" style="margin-top:10px">.*?</div><input id="file"'
replacement = '''<div class="ocrbox"><img id="docimg" class="docimg" alt="Documento"><div id="dniCameraTools" class="tools" style="margin-top:10px"><button class="primary" onclick="takeFront()">📷 Tomar anverso</button><button class="secondary" onclick="takeReverse()">↩️ Tomar reverso</button></div><div id="documentTools" class="tools" style="margin-top:10px;display:none"><button class="primary" onclick="takeDocumentPage()">📷 Tomar página</button><button class="secondary" onclick="finishDocument()">✅ Finalizar documento</button></div><div class="tools" style="margin-top:10px"><button class="secondary" onclick="rotate()">↻ Rotar</button><button class="secondary" onclick="toggleEnhance()">✨ Mejorar</button><button class="secondary" onclick="openFile()">📁 Cargar PDF / JPG</button></div></div><input id="file"'''
s2, n = re.subn(pattern, replacement, s, count=1, flags=re.S)
if n:
    s = s2
# Make file selection and camera behavior depend on mode.
s = re.sub(r'function setOcrType\(t\)\{.*?\nfunction takeFront', '''function setOcrType(t){ocrType=t;document.getElementById('ocrTypeDni').className=t==='DNI'?'primary':'secondary';let pol=document.getElementById('ocrTypePol');if(pol)pol.className=t==='POLIZA'?'primary':'secondary';let doc=document.getElementById('ocrTypeDoc');if(doc)doc.className=t==='DOCUMENTO'?'primary':'secondary';let auto=document.getElementById('ocrTypeAuto');if(auto)auto.className=t==='AUTO'?'primary':'secondary';let isDni=t==='DNI';document.getElementById('dniCameraTools').style.display=isDni?'flex':'none';document.getElementById('documentTools').style.display=isDni?'none':'flex';document.getElementById('step').textContent=isDni?'DNI/NIE: es el único documento con ANVERSO y REVERSO.':'Documento: PDF/JPG y fotografías. Puede tener una o muchas páginas; añade páginas con “Tomar página” y finaliza cuando termines.';renderOcrFields()}
function takeFront''', s, count=1, flags=re.S)
s = s.replace("function takeFront(){if(window.RgaProCamera)RgaProCamera.capture('front');else alert('Cámara nativa no disponible')}function takeReverse(){if(window.RgaProCamera)RgaProCamera.capture('reverse');else alert('Cámara nativa no disponible')}function openFile(){", "function takeFront(){if(ocrType!=='DNI')return;if(window.RgaProCamera)RgaProCamera.capture('front');else alert('Cámara nativa no disponible')}function takeReverse(){if(ocrType!=='DNI')return;if(window.RgaProCamera)RgaProCamera.capture('reverse');else alert('Cámara nativa no disponible')}function takeDocumentPage(){if(ocrType==='DNI')return;if(window.RgaProCamera)RgaProCamera.capture('document');else alert('Cámara nativa no disponible')}function finishDocument(){if(ocrType==='DNI')return;document.getElementById('ocrStatus').textContent='Documento finalizado. Revisa los datos y guarda cuando estén correctos.'}function openFile(){")
# Keep source pages in the browser model.
s = s.replace("sourceName:'',sourceType:'',sourceData:''", "sourceName:'',sourceType:'',sourceData:'',sourcePages:[]")
s = s.replace("ocrData={...ocrData,...x};", "ocrData={...ocrData,...x};if(x.pagePreview){ocrData.sourcePages=ocrData.sourcePages||[];ocrData.sourcePages.push(x.pagePreview);}")
# Show page count for multi-page documents.
s = s.replace("document.getElementById('ocrStatus').textContent=(ocrType==='DNI'?'DNI/NIE':'Documento')+' leído. Confianza estimada: '+(x.confidence||0)+'%. Revisa antes de guardar.'", "document.getElementById('ocrStatus').textContent=(ocrType==='DNI'?'DNI/NIE':'Documento')+' leído. '+(x.pageCount&&x.pageCount>1?('Páginas: '+x.pageCount+'. '):'')+'Confianza estimada: '+(x.confidence||0)+'%. Revisa antes de guardar.'")
# Persist all captured page previews when available.
s = s.replace("type:ocrData.sourceType||'text/plain',kind,data:ocrData.sourceData||'',createdAt:Date.now()", "type:ocrData.sourceType||'text/plain',kind,data:ocrData.sourceData||'',pages:ocrData.sourcePages||[],createdAt:Date.now()")
# Reset page collection when clearing.
s = s.replace("function clearOcr(){ocrData=emptyOcr();", "function clearOcr(){ocrData=emptyOcr();ocrData.sourcePages=[];")
h.write_text(s, encoding='utf-8')
print('OCR input modes patched')
