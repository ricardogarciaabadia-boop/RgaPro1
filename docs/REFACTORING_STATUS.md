# Refactoring status

## Current pass
- Domain model: added.
- Repository boundary: added.
- Storage adapter: added without changing the existing storage format.
- Save use case: added.
- Unit-test foundation: added.

## Next safe migrations
1. Route one existing MainActivity persistence flow through PolicyRepository.
2. Add regression tests for that flow.
3. Introduce a ViewModel only after the repository migration is green.
4. Isolate OCR behind an OcrEngine interface.
5. Audit background work and lifecycle ownership.
6. Harden release/R8 and run a release build.

## Safety rule
Do not merge architecture changes until the existing APK behavior is covered by regression tests and the CI build is green.
