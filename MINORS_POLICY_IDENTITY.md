# Menores de 14 años en pólizas de decesos

La compañía puede emitir la póliza sin DNI/NIE para asegurados menores de 14 años. Por tanto, la ausencia de DNI en una fila de asegurados no es un error OCR por sí misma.

## Reglas

1. Un asegurado menor de 14 años sigue siendo una persona real de la cartera aunque no tenga DNI impreso.
2. Si el DNI/NIE aparece, se conserva y se usa como identificador principal.
3. Si no aparece y la fecha de nacimiento confirma que tenía menos de 14 años en la fecha de efecto de la póliza, el estado es `OPTIONAL_FOR_MINOR`.
4. Si falta DNI pero la edad no puede confirmarse como menor de 14, el estado es `MISSING_REVIEW`; no se inventa ni se descarta la persona.
5. Nombre + fecha de nacimiento sirven para mantener la persona y detectar candidatos, pero no deben fusionar automáticamente dos personas cuando exista ambigüedad.
6. Si posteriormente se escanea el DNI del menor, se debe vincular a la persona existente y completar su identidad, sin crear un cliente duplicado.
7. Una misma persona puede ser asegurado de una póliza y tomador/asegurado de otras pólizas de vida, ahorro, hogar, accidentes o decesos.

## Objetivo de UX

La ficha debe mostrar claramente, cuando corresponda:

- nombre y apellidos;
- fecha de nacimiento;
- DNI/NIE si existe;
- indicador de "DNI no exigido en póliza por ser menor de 14" cuando la regla esté confirmada;
- capitales de la póliza;
- relación con la póliza (tomador/asegurado);
- otras pólizas de la misma persona.
