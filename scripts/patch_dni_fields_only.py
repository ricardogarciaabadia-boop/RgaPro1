from pathlib import Path
import re

h=Path('app/src/main/assets/prototype/index_v3.html')
s=h.read_text(encoding='utf-8')
pat=r'(?s)function renderOcrFields\(\)\{.*?\nfunction field'
repl='''function renderOcrFields(){let b=document.getElementById('ocrFields');if(ocrType==='DNI'){b.innerHTML=field('NOMBRE Y APELLIDOS','fullName',((ocrData.name||'')+' '+(ocrData.surname||'')).trim())+field('Nº DNI / NIE','documentNumber',ocrData.documentNumber)+field('FECHA DE NACIMIENTO','birthDate',ocrData.birthDate)+field('DIRECCIÓN','address',ocrData.address)+field('TELÉFONO','phone',ocrData.phone)+field('EMAIL','email',ocrData.email)}else if(ocrType==='POLIZA'){let t=norm(ocrData.policyType),z=field('Nº PÓLIZA','policyNumber',ocrData.policyNumber)+field('PRODUCTO','policyType',ocrData.policyType)+field('NOMBRE Y APELLIDOS','holder',ocrData.holder)+field('DNI/NIE','documentNumber',ocrData.documentNumber)+field('FECHA DE EFECTO','effectiveDate',ocrData.effectiveDate)+field('VENCIMIENTO','policyExpiry',ocrData.policyExpiry);if(t.includes('vida')||t.includes('ahorro'))z+=field('PRIMAS','premiums',ocrData.premiums);if(t.includes('deces'))z+='<div class="info">Decesos: los asegurados se vincularán con personas existentes por DNI/NIE y, si no existe, por coincidencia única de nombre.</div>';b.innerHTML=z+'<div class="notice">Solo se guardan los campos necesarios para el producto identificado.</div>'}else b.innerHTML='<div class="info">Documento general: se guardarán únicamente los datos que puedan identificarse con seguridad y se asociarán al cliente correcto.</div>'}
function field'''
s2,n=re.subn(pat,repl,s,count=1)
if n:s=s2
old="function readOcr(){document.querySelectorAll('#ocrFields input').forEach(i=>{ocrData[i.dataset.k]=i.value.trim()})}"
new="function readOcr(){document.querySelectorAll('#ocrFields input').forEach(i=>{ocrData[i.dataset.k]=i.value.trim()});if(ocrType==='DNI'&&ocrData.fullName){let parts=ocrData.fullName.trim().split(/\\s+/);ocrData.name=parts.shift()||'';ocrData.surname=parts.join(' ')||''}}"
s=s.replace(old,new)
h.write_text(s,encoding='utf-8')

p=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
j=p.read_text(encoding='utf-8')
if 'dniIgnoredFields' not in j and 'o.put("confidence",r.confidence);' in j:
    j=j.replace('o.put("confidence",r.confidence);','o.put("confidence",r.confidence);o.put("dniIgnoredFields","nationality,sex,birth place,parents,support number,issue date,validity,company CIF,policy,type,other metadata");')
p.write_text(j,encoding='utf-8')
print('DNI fields limited to required data')