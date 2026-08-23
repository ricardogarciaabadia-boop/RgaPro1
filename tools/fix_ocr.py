from pathlib import Path
import re

p = Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s = p.read_text(encoding='utf-8')
start = s.index('    private OcrData parseOcr(String raw){')
end = s.index('    private String clean(String s){', start)

method = r'''    private OcrData parseOcr(String raw){
        OcrData d=new OcrData();
        String text=raw==null?"":raw.replace('\\r','\\n');
        String norm=text.toUpperCase(Locale.ROOT)
                .replace("APELLlDOS","APELLIDOS").replace("APELLlDO","APELLIDO")
                .replace("N0MBRE","NOMBRE").replace("N0MBRES","NOMBRES")
                .replace("NACIMlENTO","NACIMIENTO").replace("NACIMlENT0","NACIMIENTO")
                .replace("NAC10NALIDAD","NACIONALIDAD");

        Matcher cif=Pattern.compile("\\\\b[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]\\\\b").matcher(norm);
        if(cif.find()){d.cif=cif.group();d.identityType="CIF";}

        // DNI/NIE: OCR puede separar los 8 dígitos y la letra (p.ej. 07230092 L).
        Matcher dniLabel=Pattern.compile("(?:DNI|NIF)\\\\s*[:.-]?\\\\s*((?:[0-9]\\\\s*){8})\\\\s*([A-Z])\\\\b").matcher(norm);
        while(dniLabel.find()){
            String candidate=dniLabel.group(1).replaceAll("\\\\s","")+dniLabel.group(2);
            if(isValidSpanishId(candidate)){d.dni=candidate;d.identityType="DNI";break;}
            if(d.dni.isEmpty()){d.dni=candidate;d.identityType="DNI";}
        }
        if(d.dni.isEmpty()){
            Matcher md=Pattern.compile("(?<![0-9])((?:[0-9]\\\\s*){8})([A-Z])(?![A-Z0-9])").matcher(norm);
            while(md.find()){
                String candidate=md.group(1).replaceAll("\\\\s","")+md.group(2);
                if(isValidSpanishId(candidate)){d.dni=candidate;d.identityType="DNI";break;}
                if(d.dni.isEmpty()){d.dni=candidate;d.identityType="DNI";}
            }
        }
        if(d.dni.isEmpty()){
            Matcher nie=Pattern.compile("(?<![A-Z0-9])([XYZ]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[0-9]\\\\s*[A-Z])(?![A-Z0-9])").matcher(norm);
            if(nie.find()){d.dni=nie.group(1).replaceAll("\\\\s","");d.identityType="NIE";}
        }

        String[] lines=norm.split("\\\\n");
        for(int i=0;i<lines.length;i++){
            String line=clean(lines[i]); if(line.isEmpty())continue;
            if(line.startsWith("APELLIDOS")||line.startsWith("APELLIDO")){
                d.surname=collectSurname(lines,i);
            }else if(line.startsWith("NOMBRE")){
                d.name=valueAfterLabel(line,"NOMBRE");
                if(d.name.isEmpty()&&i+1<lines.length)d.name=clean(lines[i+1]);
            }else if(line.contains("NACIONALIDAD")) d.nationality=valueAfterLabel(line,"NACIONALIDAD");
            else if(line.startsWith("SEXO")) d.sex=valueAfterLabel(line,"SEXO");
            else if(line.contains("DOMICILIO")){d.address=valueAfterLabel(line,"DOMICILIO");if(d.address.isEmpty())d.address=nextValue(lines,i);}
            else if(line.contains("LUGAR DE NACIMIENTO")){d.birthPlace=valueAfterLabel(line,"LUGAR DE NACIMIENTO");if(d.birthPlace.isEmpty())d.birthPlace=nextValue(lines,i);}
            else if(line.contains("HIJO/A DE")||line.contains("HIJO DE")){String lab=line.contains("HIJO/A DE")?"HIJO/A DE":"HIJO DE";d.parents=valueAfterLabel(line,lab);if(d.parents.isEmpty())d.parents=nextValue(lines,i);}
            else if(line.contains("NUM SOPORTE")||line.contains("Nº SOPORTE")||line.contains("N° SOPORTE")){String lab=line.contains("NUM SOPORTE")?"NUM SOPORTE":line.contains("Nº SOPORTE")?"Nº SOPORTE":"N° SOPORTE";d.supportNumber=valueAfterLabel(line,lab);if(d.supportNumber.isEmpty())d.supportNumber=nextValue(lines,i);}
            if(line.contains("EMISION")){String z=firstDate(line);if(!z.isEmpty())d.issueDate=z;}
            if(line.contains("VALIDEZ")||line.contains("CADUCIDAD")){String z=firstDate(line);if(!z.isEmpty())d.validityDate=z;}
        }

        // MRZ del DNI: admite uno o varios apellidos y varios nombres.
        Matcher mrzNames=Pattern.compile("([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)+)<<([A-ZÁÉÍÓÚÑ]+(?:<[A-ZÁÉÍÓÚÑ]+)*)").matcher(norm);
        if(mrzNames.find()){
            d.surname=mrzNames.group(1).replace('<',' ').replaceAll("\\\\s+"," ").trim();
            d.name=mrzNames.group(2).replace('<',' ').replaceAll("\\\\s+"," ").trim();
        }
        Matcher mdate=Pattern.compile("(\\\\d{6})\\\\d[MF<](\\\\d{6})").matcher(norm.replace("<",""));
        if(mdate.find()){
            if(d.birthDate.isEmpty())d.birthDate=mrzDate(mdate.group(1));
            if(d.validityDate.isEmpty())d.validityDate=mrzDate(mdate.group(2));
        }
        if(d.birthDate.isEmpty()){
            Matcher bd=Pattern.compile("(?:NACIMIENTO|NAC)\\\\s*[:.-]?\\\\s*(\\\\d{2}[ /.-]\\\\d{2}[ /.-]\\\\d{4})").matcher(norm);
            if(bd.find())d.birthDate=bd.group(1).replace('-','/').replace('.','/');
        }
        if(d.surname.isEmpty()&&d.name.isEmpty()){
            Matcher f=Pattern.compile("\\\\b([A-ZÁÉÍÓÚÑ]{3,})\\\\s+([A-ZÁÉÍÓÚÑ]{3,})\\\\s+([A-ZÁÉÍÓÚÑ]{3,})\\\\b").matcher(norm);
            if(f.find()){d.surname=f.group(1)+" "+f.group(2);d.name=f.group(3);}
        }
        d.holder=(d.name+" "+d.surname).trim();
        int found=0;if(!d.dni.isEmpty()||!d.cif.isEmpty())found+=25;if(!d.surname.isEmpty())found+=15;if(!d.name.isEmpty())found+=15;if(!d.birthDate.isEmpty())found+=15;if(!d.nationality.isEmpty())found+=5;if(!d.address.isEmpty())found+=5;if(!d.validityDate.isEmpty())found+=10;if(!d.issueDate.isEmpty())found+=10;d.confidence=Math.min(100,found);return d;
    }

    private String collectSurname(String[] lines,int index){
        String first=valueAfterLabel(clean(lines[index]),lines[index].toUpperCase(Locale.ROOT).startsWith("APELLIDOS")?"APELLIDOS":"APELLIDO");
        StringBuilder out=new StringBuilder(first);
        for(int j=index+1;j<Math.min(lines.length,index+4);j++){
            String v=clean(lines[j]); if(v.isEmpty()||isDniLabel(v)||isFieldLabel(v))break;
            if(v.matches("[A-ZÁÉÍÓÚÑ]+(?:[ -][A-ZÁÉÍÓÚÑ]+)*")){if(out.length()>0)out.append(' ');out.append(v);}
            else break;
        }
        return out.toString().replaceAll("\\\\s+"," ").trim();
    }

    private boolean isFieldLabel(String s){return s.startsWith("NOMBRE")||s.startsWith("SEXO")||s.contains("NACIONALIDAD")||s.contains("NACIMIENTO")||s.contains("DOMICILIO")||s.contains("SOPORTE")||s.contains("VALIDEZ")||s.contains("CADUCIDAD")||s.contains("EMISION")||s.contains("FIRMA");}
    private boolean isDniLabel(String s){return s.startsWith("DNI")||s.startsWith("NIF")||s.startsWith("NIE");}
    private boolean isValidSpanishId(String value){if(value==null)return false;if(value.matches("\\\\d{8}[A-Z]")){String letters="TRWAGMYFPDXBNJZSQVHLCKE";try{return letters.charAt(Integer.parseInt(value.substring(0,8))%23)==value.charAt(8);}catch(Exception e){return false;}}return false;}
'''

s = s[:start] + method + s[end:]
p.write_text(s, encoding='utf-8')
