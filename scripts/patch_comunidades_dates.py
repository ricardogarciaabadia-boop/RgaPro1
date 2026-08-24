from pathlib import Path
import re

h=Path('app/src/main/assets/prototype/index_v3.html')
s=h.read_text(encoding='utf-8')
# For Comunidades, omit issue/emission date. Keep only effective and expiry dates.
s=s.replace("field('FECHA DE EMISIÓN / EFECTO','effectiveDate',ocrData.effectiveDate||ocrData.issueDate)+field('VENCIMIENTO','policyExpiry',ocrData.policyExpiry)", "(t.includes('comun') ? '' : field('FECHA DE EMISIÓN / EFECTO','effectiveDate',ocrData.effectiveDate||ocrData.issueDate)) + field('FECHA DE EFECTO','effectiveDate',ocrData.effectiveDate) + field('VENCIMIENTO','policyExpiry',ocrData.policyExpiry)")
# Avoid duplicate effective-date label if the source text was already adapted.
s=s.replace("(t.includes('comun') ? '' : field('FECHA DE EMISIÓN / EFECTO','effectiveDate',ocrData.effectiveDate||ocrData.issueDate)) + field('FECHA DE EFECTO','effectiveDate',ocrData.effectiveDate)", "field('FECHA DE EFECTO','effectiveDate',ocrData.effectiveDate)")
h.write_text(s,encoding='utf-8')
print('Comunidades: fecha de emisión eliminada; solo efecto y vencimiento')
