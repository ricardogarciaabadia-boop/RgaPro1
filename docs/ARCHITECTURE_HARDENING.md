# RgaPro architecture hardening

This branch is the first stabilization step before new product features.

## Target dependency direction

```text
UI / Activity
    -> ViewModel / UI state
        -> Use cases
            -> domain repository interfaces
                -> data implementations
                    -> SharedPreferences / Room / ML Kit / files
```

## Rules

1. Activities and Fragments must not become persistence owners.
2. Domain code must not depend on Android UI classes.
3. OCR/ML Kit must be behind a domain-facing service boundary.
4. Storage implementations remain under `data/`.
5. Existing behavior is migrated incrementally; no big-bang rewrite.
6. Every migrated flow must preserve current APK behavior before the next flow is moved.

## First migration boundary

`PolicyRepository` and `SharedPreferencesPolicyRepository` isolate the current JSON-backed policy store. This is intentionally transitional: the existing UI can continue working while callers are migrated one by one.

## Next migration steps

- Introduce a ViewModel for the home/client flows.
- Replace `MainActivity.data()` / `save()` with `PolicyRepository` calls.
- Move OCR orchestration behind an `OcrEngine` interface.
- Move policy/client validation into use cases.
- Replace the JSON storage implementation with Room once the domain boundary is fully exercised by tests.
- Add unit tests for repository behavior before removing legacy storage helpers.
