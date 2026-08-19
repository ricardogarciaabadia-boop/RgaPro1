from pathlib import Path
import re

# -----------------------------------------------------------------------------
# DNI/OCR: keep only the requested identity/contact fields in the saved client.
# The DNI itself does not contain phone/email; if OCR text contains them they are
# extracted, otherwise they remain editable/empty.
# -----------------------------------------------------------------------------
main = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = main.read_text(encoding='utf-8')

helper = r'''    private void sanitizeDniClient(JSONObject p){
        if(p==null)return;
        try{
            String name=p.optString("name","").trim();
            String surname=p.optString("surname","").trim();
            String dni=p.optString("identityNumber",p.optString("holderDni","")).trim().toUpperCase(Locale.ROOT);
            String address=p.optString("address","").trim();
            String phone=p.optString("phone","").trim();
            String email=p.optString("email","").trim();
            String raw=p.optString("ocrText","");
            if(phone.isEmpty()){
                Matcher m=Pattern.compile("(?i)(?:TEL(?:ÉFONO|EFONO)?|M[ÓO]VIL|T[ÉE]L)\\s*[:.-]?\\s*(\\+?34\\s*)?[6789]\\d{8}").matcher(raw);
                if(m.find())phone=m.group().replaceAll("(?i)^(?:TEL(?:ÉFONO|EFONO)?|M[ÓO]VIL|T[ÉE]L)\\s*[:.-]?\\s*","").trim();
            }
            if(email.isEmpty()){
                Matcher m=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE).matcher(raw);
                if(m.find())email=m.group().trim();
            }
            if(address.isEmpty()){
                Matcher m=Pattern.compile("(?im)^(?:DOMICILIO|DIRECCI[ÓO]N)\\s*[:.-]?\\s*(.+)$").matcher(raw);
                if(m.find())address=m.group(1).trim();
            }
            if(!name.isEmpty())p.put("name",name); else p.remove("name");
            if(!surname.isEmpty())p.put("surname",surname); else p.remove("surname");
            if(!dni.isEmpty()){
                p.put("identityNumber",dni);p.put("holderDni",dni);
            }
            if(!address.isEmpty())p.put("address",address);
            if(!phone.isEmpty())p.put("phone",phone);
            if(!email.isEmpty())p.put("email",email);
            String full=(name+" "+surname).trim(); if(!full.isEmpty())p.put("holder",full);
            // DNI-specific personal fields are deliberately not stored in the client card.
            for(String k:new String[]{"birthDate","nationality","sex","birthPlace","parents","supportNumber","issueDate"})p.remove(k);
        }catch(Exception ignored){}
    }
'''
if 'private void sanitizeDniClient(JSONObject p)' not in s:
    marker='    private void save(JSONArray a){'
    if marker not in s: raise SystemExit('save marker not found in MainActivity.java')
    s=s.replace(marker,helper+marker,1)

# Apply only to the record just created/updated by the DNI flow. This avoids
# deleting unrelated policy fields from normal policy records.
old='    private void save(JSONArray a){prefs.edit().putString("policies",a.toString()).apply();}'
new='    private void save(JSONArray a){if(dniMode && a!=null && a.length()>0)sanitizeDniClient(a.optJSONObject(a.length()-1));prefs.edit().putString("policies",a.toString()).apply();}'
if old in s:
    s=s.replace(old,new,1)
elif 'sanitizeDniClient(a.optJSONObject(a.length()-1))' not in s:
    s=re.sub(r'    private void save\(JSONArray a\)\{[^\n]*\}',new,s,count=1)

main.write_text(s,encoding='utf-8')

# -----------------------------------------------------------------------------
# Client 360: replace the limited policy editor with a generic scalar-field
# editor. This means every existing policy field (including fields added later)
# can be edited without hard-coding a short list of fields.
# -----------------------------------------------------------------------------
client = Path('app/src/main/java/com/rgapro1/ocaso/Client360Activity.java')
c=client.read_text(encoding='utf-8')

if 'private void editAllPolicyFields()' not in c:
    method=r'''    private void editAllPolicyFields(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),0,dp(8),0);
        java.util.ArrayList<String> keys=new java.util.ArrayList<>();
        java.util.ArrayList<EditText> fields=new java.util.ArrayList<>();
        java.util.Iterator<String> it=client.keys();
        while(it.hasNext()){
            String k=it.next();
            Object v=client.opt(k);
            if(v instanceof JSONObject || v instanceof JSONArray)continue;
            if(k.equals("createdAt")||k.equals("updatedAt")||k.equals("ocrText")||k.equals("dniPhotoPath"))continue;
            keys.add(k);EditText e=field(labelForKey(k),client.optString(k,""));fields.add(e);form.addView(e,new LinearLayout.LayoutParams(-1,dp(54)));
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Editar todos los datos de la póliza / cliente").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            for(int i=0;i<keys.size();i++){String value=fields.get(i).getText().toString().trim();if(value.isEmpty())client.remove(keys.get(i));else client.put(keys.get(i),value);}
            String n=client.optString("name","").trim(),a=client.optString("surname","").trim();String full=(n+" "+a).trim();if(!full.isEmpty())client.put("holder",full);
            client.put("updatedAt",System.currentTimeMillis());
            if(saveClient(client)){dialog.dismiss();show();Toast.makeText(this,"✅ Todos los datos guardados",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"No se encontró la póliza original",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"No se pudieron guardar los cambios",Toast.LENGTH_LONG).show();}}));dialog.show();
    }
    private String labelForKey(String k){
        if("type".equals(k))return "Tipo de póliza";
        if("number".equals(k))return "Número de póliza / documento";
        if("expiry".equals(k)||"validityDate".equals(k))return "Fecha de vencimiento";
        if("holder".equals(k))return "Titular";
        if("identityNumber".equals(k)||"holderDni".equals(k))return "Número de DNI / NIE";
        if("name".equals(k))return "Nombre";
        if("surname".equals(k))return "Apellidos";
        if("address".equals(k))return "Dirección";
        if("phone".equals(k))return "Teléfono";
        if("email".equals(k))return "Email";
        String s=k.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' ');
        return s.length()==0?k:Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
'''
    marker='    private void documentMenu(String path){'
    if marker not in c: raise SystemExit('documentMenu marker not found in Client360Activity.java')
    c=c.replace(marker,method+marker,1)

# Make the existing EDITAR button open the complete editor rather than the
# restricted customer editor.
c=c.replace('edit.setTextSize(16);edit.setOnClickListener(v->editClient());','edit.setTextSize(16);edit.setOnClickListener(v->editAllPolicyFields());',1)

client.write_text(c,encoding='utf-8')
print('DNI requested fields + generic complete policy editor applied')
