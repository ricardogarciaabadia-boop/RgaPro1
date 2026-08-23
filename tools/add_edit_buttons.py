from pathlib import Path

p = Path('app/src/main/assets/prototype/index.html')
s = p.read_text(encoding='utf-8')

css_anchor = '.fieldhead{display:flex;justify-content:space-between;align-items:center}'
css_insert = '.editBtn{border:0;border-radius:9px;padding:5px 9px;background:#e9f1ff;color:#0d55c7;font-weight:850;font-size:11px}.editBtn:active{transform:scale(.98)}.editBtn.saved{background:#e8f7ef;color:#16834a}'
if css_insert not in s:
    s = s.replace(css_anchor, css_anchor + css_insert)

first_field = '<div class="field"><div class="fieldhead"><label>Nº documento</label><span class="pill warn">Revisión</span></div>'
if first_field in s and 'id="editAll"' not in s:
    s = s.replace(first_field, '<div style="display:flex;justify-content:flex-end;margin:0 0 6px"><button id="editAll" class="secondary" type="button" onclick="editAllFields()">✏️ Editar datos</button></div>' + first_field, 1)

# Add an edit button to every OCR field header, only once.
needle = '<span class="pill warn">Revisión</span></div><input'
replacement = '<span class="pill warn">Revisión</span><button class="editBtn" type="button" onclick="editField(this)">✏️ Editar</button></div><input'
s = s.replace(needle, replacement)
needle2 = '<span class="pill warn">Pendiente</span></div><input'
replacement2 = '<span class="pill warn">Pendiente</span><button class="editBtn" type="button" onclick="editField(this)">✏️ Editar</button></div><input'
s = s.replace(needle2, replacement2)

js_anchor = 'function validateSave(){'
js_insert = '''function editField(btn){\n const field=btn.closest('.field');\n const input=field.querySelector('input');\n const editing=input.hasAttribute('data-editing');\n if(!editing){\n   input.removeAttribute('readonly'); input.setAttribute('data-editing','1'); input.focus();\n   btn.textContent='✓ Guardar'; btn.classList.add('saved');\n }else{\n   input.setAttribute('readonly','readonly'); input.removeAttribute('data-editing');\n   btn.textContent='✏️ Editar'; btn.classList.remove('saved');\n }\n}\nfunction editAllFields(){\n const fields=document.querySelectorAll('#ocr .field');\n const anyLocked=[...fields].some(f=>f.querySelector('input')?.hasAttribute('readonly'));
 fields.forEach(f=>{\n   const input=f.querySelector('input'); const btn=f.querySelector('.editBtn');\n   if(anyLocked){ input.removeAttribute('readonly'); input.setAttribute('data-editing','1'); if(btn){btn.textContent='✓ Guardar';btn.classList.add('saved');}}\n   else{ input.setAttribute('readonly','readonly'); input.removeAttribute('data-editing'); if(btn){btn.textContent='✏️ Editar';btn.classList.remove('saved');}}\n });\n const all=document.getElementById('editAll');\n if(all) all.textContent=anyLocked?'✓ Terminar edición':'✏️ Editar datos';\n}\n\n'''
if 'function editField(btn)' not in s:
    s = s.replace(js_anchor, js_insert + js_anchor)

# Keep edited values in the final confirmation text instead of losing them immediately.
s = s.replace("alert('Revisa los campos y confirma manualmente antes de guardar.')", "const vals=[...document.querySelectorAll('#ocr .field input')].map(x=>x.value).join('\\n'); alert('Revisa los campos y confirma manualmente antes de guardar.\\n\\nDatos actuales:\\n'+vals);", 1)

p.write_text(s, encoding='utf-8')
