from pathlib import Path
import re

p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Nombre/apellidos como dato principal en Clientes.
if 'private String displayClientName(JSONObject p)' not in s:
    marker='    private boolean match(JSONObject p,String q)'
    helper='''    private String displayClientName(JSONObject p){
        String n=p.optString("name","").trim(), a=p.optString("surname","").trim();
        String full=(n+" "+a).trim();
        if(!full.isEmpty() && !looksLikeOcr(full)) return full;
        String h=p.optString("holder","").trim();
        return (!h.isEmpty() && !looksLikeOcr(h)) ? h : "Cliente sin nombre";
    }
    private boolean looksLikeOcr(String x){
        String u=x.toUpperCase(Locale.ROOT);
        return u.contains("DOCUMENTO") || u.contains("NNAOONAL") || u.contains("NACIONAL ESP");
    }
'''
    if marker not in s: raise SystemExit('marker match not found')
    s=s.replace(marker,helper+marker,1)

# Sustituir la pantalla de clientes por una lista cuyo título es el nombre.
pat=r'    private void clients\(\)\{.*?\n    private boolean match'
rep='''    private void clients(){
        page("Clientes","Nombre y apellidos son el dato principal");
        EditText q=edit("Nombre, DNI, NIE, CIF, teléfono, email, póliza…");
        content.addView(q,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout list=col(); content.addView(list);
        Runnable refresh=()->{ list.removeAllViews(); JSONArray a=data();
            for(int i=0;i<a.length();i++){ JSONObject p=a.optJSONObject(i); if(p==null||!match(p,q.getText().toString())) continue;
                String name=displayClientName(p); String id=p.optString("identityNumber",p.optString("holderDni","—"));
                int docs=p.optJSONArray("documentPhotos")==null?0:p.optJSONArray("documentPhotos").length();
                int products=p.optJSONArray("products")==null?0:p.optJSONArray("products").length();
                Button x=action("👤  "+name+"\\nDNI/NIE: "+id+"  ·  "+products+" productos  ·  "+docs+" documentos",false);
                x.setTextSize(15); x.setOnClickListener(v->detail(p)); list.addView(x,new LinearLayout.LayoutParams(-1,dp(82)));
            }
            if(list.getChildCount()==0) list.addView(tv("No se encontraron clientes.",15,MUTED,false)); };
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh.run();} public void afterTextChanged(android.text.Editable e){}});
        refresh.run();
    }
    private boolean match'''
s,n=re.subn(pat,rep,s,flags=re.S)
if n!=1: raise SystemExit('clients replacement failed')
p.write_text(s,encoding='utf-8')

# Add a robust save/edit/document hub to Client360Activity without changing its entry point.
c=Path('app/src/main/java/com/rgapro1/ocaso/Client360Activity.java')
cs=c.read_text(encoding='utf-8')
if 'private void editClientFinal()' not in cs:
    anchor='    private void showProduct(JSONObject p){'
    add='''    private void editClientFinal(){
        LinearLayout l=col(); EditText n=edit("Nombre",client.optString("name","")); EditText a=edit("Apellidos",client.optString("surname",""));
        EditText id=edit("DNI / NIE",client.optString("identityNumber",client.optString("holderDni",""))); EditText ph=edit("Teléfono",client.optString("phone",""));
        EditText em=edit("Email",client.optString("email","")); EditText ad=edit("Dirección",client.optString("address",""));
        l.addView(n);l.addView(a);l.addView(id);l.addView(ph);l.addView(em);l.addView(ad);
        new AlertDialog.Builder(this).setTitle("Editar datos del cliente").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{
            try{client.put("name",n.getText().toString().trim());client.put("surname",a.getText().toString().trim());client.put("identityNumber",id.getText().toString().trim());client.put("holderDni",id.getText().toString().trim());client.put("holder",(n.getText().toString().trim()+" "+a.getText().toString().trim()).trim());client.put("phone",ph.getText().toString().trim());client.put("email",em.getText().toString().trim());client.put("address",ad.getText().toString().trim());addHistoryFinal("Datos corregidos manualmente");persistFinal();render();
            }catch(Exception e){toast("No se pudo guardar: "+e.getMessage());}}).show();
    }
    private void addDocumentFinal(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,441);}
    private void addProductFinal(){LinearLayout l=col();EditText t=edit("Tipo","");EditText n=edit("Número","");EditText e=edit("Vencimiento","");l.addView(t);l.addView(n);l.addView(e);new AlertDialog.Builder(this).setTitle("Añadir producto/póliza").setView(l).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{try{JSONArray a=client.optJSONArray("products");if(a==null){a=new JSONArray();client.put("products",a);}JSONObject x=new JSONObject();x.put("type",t.getText().toString().trim());x.put("number",n.getText().toString().trim());x.put("expiry",e.getText().toString().trim());a.put(x);addHistoryFinal("Producto/póliza añadido");persistFinal();render();}catch(Exception z){toast("No se pudo añadir");}}).show();}
    private void addHistoryFinal(String s){try{JSONArray h=client.optJSONArray("history");if(h==null){h=new JSONArray();client.put("history",h);}h.put(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ROOT).format(new java.util.Date())+" — "+s);}catch(Exception ignored){}}
    private void persistFinal(){try{JSONArray a=new JSONArray(prefs.getString("policies","[]"));for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String id=client.optString("identityNumber","");if((!id.isEmpty()&&id.equalsIgnoreCase(p.optString("identityNumber",p.optString("holderDni",""))))||client.toString().equals(p.toString())){a.put(i,client);prefs.edit().putString("policies",a.toString()).apply();return;}}toast("No se encontró el cliente original");}catch(Exception e){toast("No se pudo guardar: "+e.getMessage());}}
'''
    if anchor not in cs: raise SystemExit('showProduct anchor not found')
    cs=cs.replace(anchor,add+anchor,1)

# Add an edit/add button to the existing 360 header/body.
needle='        body.addView(tv("👤 "+p.optString("holder",p.optString("name","Cliente")),23,true));'
insert='''        String displayName=p.optString("name","").trim()+" "+p.optString("surname","").trim();
        if(displayName.trim().isEmpty()) displayName=p.optString("holder","Cliente");
        body.addView(t("👤 "+displayName.trim(),23,true));
        Button editFinal=btn("✏️ Editar datos del cliente"); editFinal.setOnClickListener(v->editClientFinal()); body.addView(editFinal,new LinearLayout.LayoutParams(-1,dp(58)));
        Button addFinal=btn("➕ Añadir documento / producto / información"); addFinal.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Añadir al cliente").setItems(new String[]{"Documento / foto / PDF","Producto / póliza"},(d,w)->{if(w==0)addDocumentFinal();else addProductFinal();}).show()); body.addView(addFinal,new LinearLayout.LayoutParams(-1,dp(58)));
'''
    if needle not in cs: raise SystemExit('client name needle not found')
    cs=cs.replace(needle,insert,1)

# Support document objects as well as legacy string paths.
cs=cs.replace('String path=docs.optString(i,"");','Object item=docs.opt(i); String path=item instanceof JSONObject?((JSONObject)item).optString("path",""):String.valueOf(item);',1)
c.write_text(cs,encoding='utf-8')
'''
GitHub.fetch_file... 
