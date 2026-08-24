from pathlib import Path

p=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Attach detected capitales to insured persons without depending on a fragile
# source-code marker. This patch is intentionally idempotent and must never
# fail the workflow merely because an earlier patch changed MainActivity.java.
helper='''    private void attachCapitalesToInsureds(JSONObject policy){
        try{
            JSONArray ins=policy.optJSONArray("insureds");
            JSONArray caps=policy.optJSONArray("capitales");
            if(ins==null||caps==null)return;
            for(int i=0;i<ins.length();i++){
                JSONObject person=ins.optJSONObject(i);
                if(person==null)continue;
                JSONObject own=new JSONObject();
                for(int c=0;c<caps.length();c++){
                    JSONObject row=caps.optJSONObject(c);
                    if(row==null)continue;
                    JSONArray values=row.optJSONArray("values");
                    if(values!=null&&i<values.length())own.put(row.optString("coverage","Cobertura"),values.optString(i,""));
                }
                person.put("capitales",own);
                String base=own.optString("TOTAL DECESOS",own.optString("DECESOS NIVELADA",""));
                if(!base.isEmpty())person.put("capitalDecesos",base);
            }
        }catch(Exception ignored){}
    }

'''
if 'private void attachCapitalesToInsureds' not in s:
    # Insert before the first rebuildCrossLinks-like method if it exists;
    # otherwise insert immediately before the final class closing brace.
    marker='private void rebuildCrossLinks('
    idx=s.find(marker)
    if idx>=0:
        start=s.rfind('    ',0,idx)
        s=s[:start]+helper+s[start:]
    else:
        close=s.rfind('\n}')
        if close<0:
            raise SystemExit('MainActivity class closing brace not found')
        s=s[:close]+'\n'+helper+s[close:]

needle='p.put("capitales",extractDecesoCapitalsAdvanced(raw,ins.length()));'
if needle in s and 'attachCapitalesToInsureds(p);' not in s:
    s=s.replace(needle,needle+'\n            attachCapitalesToInsureds(p);',1)

p.write_text(s,encoding='utf-8')
print('Linked detected capitales to each insured for pricing/reference')
