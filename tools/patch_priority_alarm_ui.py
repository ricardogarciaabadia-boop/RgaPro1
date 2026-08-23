from pathlib import Path
import re

p = Path("app/src/main/assets/prototype/index.html")
s = p.read_text(encoding="utf-8")

priority_css = r'''
.priority-list{display:grid;gap:8px;margin-top:10px}.priority-item{display:flex;align-items:center;gap:10px;padding:10px 11px;border-radius:12px;background:#ffffff12;border:1px solid #ffffff20}.priority-item .p-icon{width:34px;height:34px;border-radius:10px;display:grid;place-items:center;background:#ffffff18;font-weight:950}.priority-item .p-main{min-width:0;flex:1}.priority-item .p-name{font-weight:900;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.priority-item .p-meta{font-size:11px;color:#d7e4f5;margin-top:2px}.priority-item .p-days{font-weight:950;white-space:nowrap}.priority-item.critical{background:#8b1e2d33;border-color:#ff9eaa55}.priority-item.soon{background:#ffffff10}.priority-empty{font-size:12px;color:#d7e4f5;padding:7px 0}
'''
if ".priority-list{" not in s:
    s = s.replace(".alarm-card{", priority_css + ".alarm-card{", 1)

old = re.compile(
    r'<div class="card alarm-card">\s*'
    r'<div class="eyebrow">ALARMAS ACTIVAS</div>\s*'
    r'<div class="alarm-title">60 · 40 · 30 · 7 · 1 día</div>\s*'
    r'<div class="alarm-sub">Cada vencimiento guardado genera avisos independientes en esos cinco momentos\.</div>\s*'
    r'</div>', re.S)
new = '''<div class="card alarm-card">
  <div class="eyebrow">ALARMAS · PRIORIDADES</div>
  <div class="alarm-title">Las más importantes primero</div>
  <div class="alarm-sub">Se muestran aquí los vencimientos que requieren atención antes. Las alarmas completas siguen programadas a 60, 40, 30, 7 y 1 día.</div>
  <div id="priorityList" class="priority-list"></div>
</div>'''
s, n = old.subn(new, s, count=1)
if n == 0 and 'id="priorityList"' not in s:
    marker = '<div class="notice">La búsqueda se hace sobre los datos guardados'
    if marker in s:
        s = s.replace(marker, new + '\n' + marker, 1)

js = r'''
function priorityKind(days){
  if(days==null) return ["",""];
  if(days<=1) return ["critical","URGENTE"];
  if(days<=7) return ["soon","7 días"];
  if(days<=30) return ["soon","30 días"];
  if(days<=40) return ["soon","40 días"];
  return ["soon","60 días"];
}
function renderPriorityAlarms(){
  const box=document.getElementById('priorityList');
  if(!box) return;
  const rows=storedRecords().map(r=>({r,days:daysTo(r.expiry)}))
    .filter(x=>x.days!=null && x.days>=0 && x.days<=60)
    .sort((a,b)=>a.days-b.days);
  if(!rows.length){
    box.innerHTML='<div class="priority-empty">No hay alertas prioritarias dentro de los próximos 60 días.</div>';
    return;
  }
  box.innerHTML=rows.slice(0,6).map(x=>{
    const when=x.days===0?'HOY':(x.days===1?'MAÑANA':('EN '+x.days+' DÍAS'));
    const icon=x.days<=1?'!':(x.days<=7?'⚠':'•');
    return '<div class="priority-item '+priorityKind(x.days)[0]+'"><div class="p-icon">'+icon+'</div><div class="p-main"><div class="p-name">'+esc(x.r.holder||x.r.name||'Sin titular')+'</div><div class="p-meta">'+esc([x.r.type||'Póliza',x.r.number||'Sin número',x.r.expiry||''].filter(Boolean).join(' · '))+'</div></div><div class="p-days">'+when+'</div></div>';
  }).join('');
}
'''
if "function renderPriorityAlarms()" not in s:
    if "function refreshHome()" in s:
        s = s.replace("function refreshHome()", js + "\nfunction refreshHome()", 1)
    else:
        s = s.replace("</script>", js + "\n</script>", 1)

if "renderPriorityAlarms();" not in s:
    s = s.replace("function refreshHome(){renderGlobalSearch();renderUpcoming()}", "function refreshHome(){renderGlobalSearch();renderUpcoming();renderPriorityAlarms()}", 1)

p.write_text(s, encoding="utf-8")

icon = Path("app/src/main/res/drawable/ic_rgapro.xml")
icon.write_text('''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/rgapro_logo"
    android:gravity="fill" />
''', encoding="utf-8")
