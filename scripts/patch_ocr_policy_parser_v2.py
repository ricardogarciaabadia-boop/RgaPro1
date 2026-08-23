from pathlib import Path
import subprocess

# Keep the PR build on the canonical OCR-v2 implementation from main.
canonical=subprocess.check_output(['git','show','origin/main:scripts/patch_ocr_policy_parser_v2.py'],text=True)
ns={'__name__':'__main__'}
exec(compile(canonical,'scripts/patch_ocr_policy_parser_v2.py','exec'),ns,ns)
print('OCR v2 canonical patch executed')
