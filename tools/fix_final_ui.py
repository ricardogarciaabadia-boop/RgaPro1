from pathlib import Path
import re

html_path=Path('app/src/main/assets/prototype/index.html')
s=html_path.read_text(encoding='utf-8')
# Fix OCR preview proportions: fixed frame, contained image, no overflow when rotated.
s=s.replace('.ocrbox{background:#f7f9fc;border:1px solid var(--line);border-radius:14px;padding:10px}.ocrbox img{width:100%;max-height:320px;object-fit:contain;border-radius:10px;background:#071b3a}', '.ocrbox{background:#f7f9fc;border:1px solid var(--line);border-radius:14px;padding:10px;overflow:hidden}.preview-frame{width:100%;height:270px;display:flex;align-items:center;justify-content:center;overflow:hidden;border-radius:10px;background:#071b3a}.ocrbox img{display:block;max-width:100%;max-height:100%;width:auto;height:auto;object-fit:contain;border-radius:8px}')
s=s.replace('<div class="ocrbox"><img id="docimg"', '<div class="ocrbox"><div class="preview-frame"><img id="docimg"')
s=s.replace('><div class="tools"><button class="primaryTool"', '></div><div class="tools"><button class="primaryTool"', 1)
# Use the supplied RgaPro logo in the in-app header.
s=re.sub(r'<img class="logo" src="[^"]*">', '<img class="logo" src="file:///android_res/drawable/rgapro_logo.jpg" onerror="this.style.display=\'none\'">', s, count=1)
s=s.replace('.logo{width:48px;height:48px;border-radius:12px;object-fit:cover}', '.logo{width:50px;height:50px;border-radius:12px;object-fit:cover;background:#071b3a}')
# Expose edit buttons for OCR fields.
s=s.replace('<div class="fieldhead"><label>Nombre y apellidos</label><span class="pill warn">Revisión</span></div><input value="Esperando lectura" readonly>', '<div class="fieldhead"><label>Nombre y apellidos</label><button class="edit" onclick="editField(this.parentElement.nextElementSibling)">Editar</button></div><input value="Esperando lectura">')
s=s.replace('<div class="fieldhead"><label>Fecha de nacimiento</label><span class="pill warn">Revisión</span></div><input value="Esperando lectura" readonly>', '<div class="fieldhead"><label>Fecha de nacimiento</label><button class="edit" onclick="editField(this.parentElement.nextElementSibling)">Editar</button></div><input value="Esperando lectura">')
s=s.replace('<div class="fieldhead"><label>Fecha de caducidad</label><span class="pill warn">Revisión</span></div><input value="Esperando lectura" readonly>', '<div class="fieldhead"><label>Fecha de caducidad</label><button class="edit" onclick="editField(this.parentElement.nextElementSibling)">Editar</button></div><input value="Esperando lectura">')
s=s.replace('.empty{padding:25px;text-align:center;color:var(--muted);background:white;border:1px dashed var(--line);border-radius:14px}', '.empty{padding:25px;text-align:center;color:var(--muted);background:white;border:1px dashed var(--line);border-radius:14px}.edit{border:0;border-radius:9px;padding:6px 9px;background:#eef4ff;color:var(--blue2);font-size:11px;font-weight:900}.field input{color:var(--ink)}')
# Keep rotated images inside the fixed preview frame.
s=s.replace("function rotate(){deg=(deg+90)%360;document.getElementById('docimg').style.transform='rotate('+deg+'deg')}", "function rotate(){deg=(deg+90)%360;const i=document.getElementById('docimg');i.style.transform='rotate('+deg+'deg)';i.style.maxWidth='100%';i.style.maxHeight='100%'}")
if 'function editField(' not in s:
    s=s.replace('function toggleEnhance(){', "function editField(input){input.readOnly=!input.readOnly;if(!input.readOnly)input.focus()}\nfunction toggleEnhance(){", 1)
html_path.write_text(s,encoding='utf-8')

# Harden surname extraction so multi-word surnames such as DEL BARRIO GARCIA are retained.
java_path=Path('app/src/main/java/com/rgapro1/ocaso/PrototypeActivity.java')
j=java_path.read_text(encoding='utf-8')
pat=r'private String findSurname\(String\[\] ls\)\{.*?\}\n private String findExpiry'
m=re.search(pat,j,re.S)
if m:
    new='''private String findSurname(String[] ls){for(int i=0;i<ls.length;i++){String u=ls[i].trim().toUpperCase(Locale.ROOT);if(u.startsWith("APELLIDOS")||u.startsWith("APELLIDO")){String rest=u.substring(u.startsWith("APELLIDOS")?9:8).replaceFirst("^[ :.-]+","").trim();StringBuilder b=new StringBuilder(rest);for(int k=i+1;k<Math.min(ls.length,i+6);k++){String x=ls[k].trim().toUpperCase(Locale.ROOT);if(x.isEmpty()||x.startsWith("NOMBRE")||x.startsWith("SEXO")||x.contains("NACIONALIDAD")||x.contains("NACIMIENTO")||x.contains("DOMICILIO")||x.contains("VALIDEZ")||x.contains("CADUCIDAD")||x.contains("SOPORTE"))break;if(x.matches("[A-ZÁÉÍÓÚÑ]+(?:[ -][A-ZÁÉÍÓÚÑ]+)*")&&x.length()>1){if(b.length()>0)b.append(' ');b.append(x);}else break;}if(b.length()>0)return b.toString().replaceAll("\\\\s+"," ").trim();}}String all=String.join(" ",ls).toUpperCase(Locale.ROOT);Matcher z=Pattern.compile("(?:APELLIDOS|APELLIDO)\\\\s*[:.-]?\\\\s*([A-ZÁÉÍÓÚÑ]+(?:[ -][A-ZÁÉÍÓÚÑ]+){1,4})").matcher(all);return z.find()?z.group(1).trim():"";}\n private String findExpiry'''
    j=j[:m.start()]+new+j[m.end():]
else:
    raise SystemExit('findSurname not found')
java_path.write_text(j,encoding='utf-8')
