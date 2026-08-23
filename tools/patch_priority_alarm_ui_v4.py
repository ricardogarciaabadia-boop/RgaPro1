from pathlib import Path
import base64

b64 = Path("branding/rgapro_logo.b64").read_text(encoding="utf-8").strip()
out = Path("app/src/main/res/drawable/rgapro_logo.jpg")
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(base64.b64decode(b64))

p = Path("app/src/main/assets/prototype/index.html")
s = p.read_text(encoding="utf-8")

css = ".priority-list{display:grid;gap:8px;margin-top:10px}.priority-item{display:flex;align-items:center;gap:10px;padding:10px 11px;border-radius:12px;background:#ffffff12;border:1px solid #ffffff20}.priority-item .p-icon{width:34px;height:34px;border-radius:10px;display:grid;place-items:center;background:#ffffff18;font-weight:950}.priority-item .p-main{min-width:0;flex:1}.priority-item .p-name{font-weight:900;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.priority-item .p-meta{font-size:11px;color:#d7e4f5;margin-top:2px}.priority-item .p-days{font-weight:950;white-space:nowrap}.priority-item.critical{background:#8b1e2d33;border-color:#ff9eaa55}.priority-empty{font-size:12px;color:#d7e4f5;padding:7px 0}"
if ".priority-list{" not in s:
    s=s.replace("</style>",css+"</style>",1)

home=s.find('<section id="home"')
if home>=0 and 'id="priorityList"' not in s:
    close=s.find("</section>",home)
    if close>0:
        card='''<div class="card alarm-card" style="background:linear-gradient(135deg,#071b3a,#0b3f8d);color:#fff;border:0"><div class="eyebrow" style="color:#91c8ff">ALARMAS · PRIORIDADES</div><div class="alarm-title">Las más importantes primero</div><div class="alarm-sub">Se muestran primero las que requieren atención. Las alarmas completas siguen en 60, 40, 30, 7 y 1 día.</div><div id="priorityList" class="priority-list"></div></div>'''
        s=s[:close]+card+s[close:]

js=r'''
function rgaproPriorityRecords(){
  let a=[]; try{a=JSON.parse(localStorage.getItem('rgapro_records')||'[]')}catch(e){}
  if(typeof users!=='undefined'&&Array.isArray(users)) users.forEach(u=>(u.policies||[]).forEach(p=>a.push({holder:p.holder||u.name,name:u.name,type:p.type,number:p.no,expiry:p.expiry})));
  return a;
}
function rgaproDays(v){let m=String(v||'').match(/^(\d{2})[\/.-](\d{2})[\/.-](\d{4})$/);if(!m)return null;let d=new Date(+m[3],+m[2]-1,+m[1]),t=new Date();t.setHours(0,0,0,0);return Math.ceil((d-t)/86400000)}
function renderRgaProPriority(){
 const b=document.getElementById('priorityList');if(!b)return;
 const e=rgaproPriorityRecords().map(r=>({r,d:rgaproDays(r.expiry)})).filter(x=>x.d!=null&&x.d>=0&&x.d<=60).sort((a,b)=>a.d-b.d);
 if(!e.length){b.innerHTML='<div class="priority-empty">No hay alertas prioritarias dentro de los próximos 60 días.</div>';return}
 b.innerHTML=e.slice(0,6).map(x=>{let when=x.d===0?'HOY':(x.d===1?'MAÑANA':'EN '+x.d+' DÍAS');let icon=x.d<=1?'!':(x.d<=7?'⚠':'•');return '<div class="priority-item '+(x.d<=1?'critical':'')+'"><div class="p-icon">'+icon+'</div><div class="p-main"><div class="p-name">'+(x.r.holder||x.r.name||'Sin titular')+'</div><div class="p-meta">'+[x.r.type||'Póliza',x.r.number||'Sin número',x.r.expiry||''].filter(Boolean).join(' · ')+'</div></div><div class="p-days">'+when+'</div></div>'}).join('');
}
renderRgaProPriority();
'''
if "function renderRgaProPriority()" not in s:
    s=s.replace("</script>",js+"</script>",1)

p.write_text(s,encoding="utf-8")
Path("app/src/main/res/drawable/ic_rgapro.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/rgapro_logo"
    android:gravity="fill" />
''',encoding="utf-8")
