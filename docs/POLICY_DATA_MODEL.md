# Policy data model — required OCR semantics

A death/funeral policy is not a single-person record.

## Required structure

- One policy number identifies the policy.
- The policy has a holder/tomador.
- The holder may also be an insured person; this must be represented explicitly, not inferred away.
- The policy may contain multiple insured people.
- Every insured person should retain:
  - full name
  - birth date
  - DNI/NIE
  - insured capital
  - role (holder/insured)
- An insured person is a global client candidate and can be holder/tomador of other policies such as life, savings or home insurance.

## Identity and linking rules

1. DNI/NIE is the primary identity key.
2. If the DNI/NIE already exists, reuse the existing client; never create a duplicate.
3. A person can be linked to many policies.
4. A policy can have many insured people.
5. Holder and insured are separate roles even when they refer to the same person.
6. Names alone must never silently create a second client when an identity number exists.
7. OCR uncertainty must be surfaced for review rather than silently overwriting trusted data.

## Example

```text
Cliente: Cristina Rodríguez Jiménez
DNI: 48920227D

Póliza: 4064289
  TOMADOR: Cristina Rodríguez Jiménez
  ASEGURADA: Cristina Rodríguez Jiménez — capital X
  ASEGURADO: Eduardo Gómez Rodríguez — DNI ... — capital Y
  ASEGURADO: Jorge Rodríguez Rodríguez — DNI ... — capital Z
```

The same people may later appear as holders of other policies. The data model must therefore be person-centric with many policy relationships, not one client record per scanned document.
