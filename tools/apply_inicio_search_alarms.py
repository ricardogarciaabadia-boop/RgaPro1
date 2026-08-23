from pathlib import Path
import re

p = Path('app/src/main/assets/prototype/index.html')
s = p.read_text(encoding='utf-8')

start = s.index('<section id="home"')
end = s.index('<section id="policy"', start)
home = '''<section id="home" class="screen active">
<div class="section-title">Inicio</div>
<div class="card search-card">
  <div class="eyebrow">BÚSQUEDA GLOBAL</div>
  <div class="search-title">Buscar cualquier dato</div>
  <div class="search-sub">Nombre, apellidos, DNI/NIE, teléfono, dirección, email, CIF, póliza o cualquier dato guardado.</div>
  <input id="globalSearch" class="global-search" type="search" autocomplete="off" placeholder="🔎  Escribe un dato para buscar…" oninput="renderGlobalSearch()">
  <div id="searchResults" class="search-results"></div>
</div>
<div class="section-title">Próximos vencimientos</div>
<div id="upcomingList"></div>
<div class="card alarm-card">
  <div class="eyebrow">ALARMAS ACTIVAS</div>
  <div class="alarm-title">60 · 40 · 30 · 7 · 1 día</div>
  <div class="alarm-sub">Cada vencimiento guardado genera avisos independientes en esos cinco momentos.</div>
</div>
<div class="notice">La búsqueda se hace sobre los datos guardados en este dispositivo. Pulsa un resultado para abrir su ficha.</div>
</section>
'''
s = s[:start] + home + s[end:]

css_anchor = '.footer{text-align:center;color:var(--muted);font-size:11px;padding:10px 0 30px}'
css_insert = '''
.search-title{font-size:20px;font-weight:950;margin:5px 0 3px}.search-sub{font-size:12px;color:var(--muted);line-height:1.45;margin-bottom:11px}.global-search{width:100%;border:2px solid var(--line);border-radius:13px;padding:13px 14px;font-size:16px;outline:none;background:#fff}.global-search:focus{border-color:var(--blue)}.search-results{margin-top:10px}.search-result{display:flex;align-items:center;gap:10px;width:100%;border:1px solid var(--line);background:#fff;border-radius:13px;padding:11px;text-align:left;margin:7px 0;cursor:pointer}.search-result .sr-main{font-weight:900}.search-result .sr-meta{font-size:11px;color:var(--muted);margin-top:3px}.search-empty{font-size:13px;color:var(--muted);padding:8px 2px}.expiry-card{background:#fff;border:1px solid var(--line);border-radius:15px;padding:13px 14px;margin-bottom:9px;cursor:pointer}.expiry-card.urgent{border-color:#f0b0b0;background:#fff8f8}.expiry-card .ec-top{display:flex;justify-content:space-between;gap:10px}.expiry-card .ec-name{font-weight:900}.expiry-card .ec-days{font-weight:950;color:var(--blue2);white-space:nowrap}.expiry-card.urgent .ec-days{color:#b42318}.expiry-card .ec-meta{font-size:12px;color:var(--muted);margin-top:4px}.expiry-card .ec-alarm{display:inline-block;margin-top:7px;padding:4px 8px;border-radius:999px;background:#eaf3ff;color:#124f9d;font-size:10px;font-weight:900}.alarm-card{background:linear-gradient(135deg,#071b3a,#0b3f8d);color:#fff;border:0}.alarm-card .eyebrow{color:#91c8ff}.alarm-title{font-size:23px;font-weight:950;margin:6px 0}.alarm-sub{font-size:12px;color:#d7e4f5;line-height:1.45}
'''
if css_anchor in s and '.global-search' not in s:
    s = s.replace(css_anchor, css_anchor + css_insert)

if 'let currentOcrData=null;' not in s:
    s = s.replace('<script>\n', '<script>\nlet currentOcrData=null;\n', 1)

anchor = 'function show(id){'
helpers = r'''function storedRecords(){try{return JSON.parse(localStorage.getItem('rgapro_records')||'[]')}catch(e){return[]}}
function saveStoredRecord(r){const a=storedRecords();const id=r.id||('r_'+Date.now());r.id=id;const same=a.findIndex(x=>x.id===id);if(same>=0)a[same]=r;else a.push(r);localStorage.setItem('rgapro_records',JSON.stringify(a));return r}
function norm(v){return String(v==null?'':v).toLocaleLowerCase('es-ES').normalize('NFD').replace(/[\u0300-\u036f]/g,'')}
function recordMatches(r,q){return norm(JSON.stringify(r)).includes(norm(q))}
function parseDateValue(v){const x=String(v||'').trim();let m=x.match(/^(\d{2})[\/.-](\d{2})[\/.-](\d{4})$/);if(m)return new Date(+m[3],+m[2]-1,+m[1]);m=x.match(/^(\d{4})-(\d{2})-(\d{2})$/);if(m)return new Date(+m[1],+m[2]-1,+m[3]);return null}
function daysTo(v){const d=parseDateValue(v);if(!d)return null;const today=new Date();today.setHours(0,0,0,0);return Math.ceil((d.getTime()-today.getTime())/86400000)}
function nextAlarm(days){for(const n of [60,40,30,7,1])if(days>=0&&days<=n)return n;return null}
function renderGlobalSearch(){const q=document.getElementById('globalSearch');const box=document.getElementById('searchResults');if(!q||!box)return;const text=q.value.trim();if(!text){box.innerHTML='<div class="search-empty">Introduce cualquier dato para empezar.</div>';return}const hits=storedRecords().filter(r=>recordMatches(r,text)).slice(0,20);if(!hits.length){box.innerHTML='<div class="search-empty">No se encontraron coincidencias.</div>';return}box.innerHTML=hits.map((r,i)=>'<div class="search-result" onclick="openStoredRecord('+i+')"><div class="avatar">'+(i+1)+'</div><div><div class="sr-main">'+esc(r.holder||r.name||'Sin titular')+'</div><div class="sr-meta">'+esc([r.identityNumber,r.phone,r.address,r.number,r.type].filter(Boolean).join(' · '))+'</div></div><div class="chev">›</div></div>').join('');window._searchHits=hits}
function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function openStoredRecord(i){const r=(window._searchHits||[])[i];if(!r)return;const d=daysTo(r.expiry);const msg=['Titular: '+(r.holder||'—'),'DNI/NIE/CIF: '+(r.identityNumber||'—'),'Teléfono: '+(r.phone||'—'),'Dirección: '+(r.address||'—'),'Email: '+(r.email||'—'),'Póliza: '+(r.number||'—'),'Tipo: '+(r.type||'—'),'Vencimiento: '+(r.expiry||'—')].join('\n');alert(msg+(d!=null?'\n\nQuedan '+d+' días.':'') )}
function renderUpcoming(){const box=document.getElementById('upcomingList');if(!box)return;const rows=storedRecords().map(r=>({r,days:daysTo(r.expiry)})).filter(x=>x.days!=null&&x.days>=0&&x.days<=60).sort((a,b)=>a.days-b.days);if(!rows.length){box.innerHTML='<div class="card"><div class="search-empty">No hay vencimientos guardados dentro de los próximos 60 días.</div></div>';return}box.innerHTML=rows.slice(0,30).map((x,i)=>{const a=nextAlarm(x.days);return '<div class="expiry-card '+(x.days<=7?'urgent':'')+'" onclick="openUpcoming('+i+')"><div class="ec-top"><div class="ec-name">'+esc(x.r.holder||x.r.name||'Sin titular')+'</div><div class="ec-days">'+(x.days===0?'HOY':x.days+' días')+'</div></div><div class="ec-meta">'+esc([x.r.type||'Póliza',x.r.number||'Sin número',x.r.expiry||''].filter(Boolean).join(' · '))+'</div>'+(a!=null?'<span class="ec-alarm">Próxima alarma: '+a+' días</span>':'')+'</div>'}).join('');window._upcoming=rows}
function openUpcoming(i){const x=(window._upcoming||[])[i];if(!x)return;alert((x.r.holder||'Sin titular')+'\n'+(x.r.type||'Póliza')+' · '+(x.r.number||'Sin número')+'\nVence: '+(x.r.expiry||'—')+'\n\nQuedan '+x.days+' días.\nPróxima alarma: '+(nextAlarm(x.days)||'—')+' días.')}
function refreshHome(){renderGlobalSearch();renderUpcoming()}
function normalizeExpiry(v){const d=parseDateValue(v);if(!d)return '';const dd=String(d.getDate()).padStart(2,'0'),mm=String(d.getMonth()+1).padStart(2,'0');return dd+'/'+mm+'/'+d.getFullYear()}

'''
if 'function storedRecords()' not in s:
    s = s.replace(anchor, helpers + anchor, 1)

s = s.replace("const d=typeof data==='string'?JSON.parse(data):data;", "const d=typeof data==='string'?JSON.parse(data):data;currentOcrData=d;", 1)

new_validate = r'''function validateSave(){
const step=document.getElementById('step').textContent;if(!document.getElementById('reverseBtn').disabled&&step.indexOf('revisa')<0){alert('Toma primero el reverso del DNI/NIE para completar la MRZ.');return}
const fields=[...document.querySelectorAll('#ocr .field')];const vals={};fields.forEach(f=>{const k=f.querySelector('label').textContent.trim();vals[k]=f.querySelector('input').value.trim()});
const expiry=normalizeExpiry(vals['Fecha de caducidad']);const record={id:(currentOcrData&&currentOcrData.documentNumber&&currentOcrData.documentNumber!=='No detectado')?currentOcrData.documentNumber:('ocr_'+Date.now()),holder:vals['Nombre y apellidos'],name:vals['Nombre y apellidos'],identityNumber:vals['Nº documento'],expiry:expiry,number:'',type:'Documento / cliente',raw:currentOcrData&&currentOcrData.raw||'',phone:'',address:'',email:'',savedAt:Date.now()};
saveStoredRecord(record);if(window.RgaProCamera&&expiry){try{RgaProCamera.scheduleExpiry(JSON.stringify(record))}catch(e){console.log(e)}}
alert('Guardado correctamente. Las alarmas quedan configuradas para 60, 40, 30, 7 y 1 día antes.');show('home');refreshHome()}
'''
s, count = re.subn(r'function validateSave\(\)\{.*?\}\s*</script>', new_validate + 'refreshHome();\n</script>', s, count=1, flags=re.S)
if count != 1:
    raise SystemExit('validateSave function not found')

if "if(id==='home')refreshHome();" not in s:
    s = s.replace('scrollTo(0,0)}', "if(id==='home')refreshHome();scrollTo(0,0)}", 1)

p.write_text(s, encoding='utf-8')
