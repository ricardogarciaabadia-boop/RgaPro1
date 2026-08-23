from pathlib import Path

# Build trigger / safety check for OCR v2.
p=Path('app/src/main/java/com/rgapro1/ocaso/PolicyOcrParser.java')
s=p.read_text(encoding='utf-8')
if 'Robust policy OCR parser' not in s:
    raise SystemExit('PolicyOcrParser.java was not updated before the build')
a=Path('app/src/main/java/com/rgapro1/ocaso/RgaProActivity.java')
t=a.read_text(encoding='utf-8')
if 'private JSONObject parse(String raw)throws Exception{return PolicyOcrParser.parse(raw);}' not in t:
    raise SystemExit('RgaProActivity parser hook missing')
print('RgaPro OCR v2 ready')
