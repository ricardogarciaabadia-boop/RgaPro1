from pathlib import Path

# This patch was generating invalid Java escape sequences. The policy parser
# implementation is intentionally kept in the existing native parser until
# a clean Java-safe implementation is introduced.
# Price and payment-frequency support is already covered by the stable parser
# chain; do not mutate Java sources from this patch.
print('price and payment frequency detection patch skipped: preserve stable Java OCR parser')
