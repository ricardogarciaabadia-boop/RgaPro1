package com.rgapro1.ocaso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DeathPolicyInsuredParserRealDocumentTest {
    @Test
    public void parsesTheFiveInsuredRowsAndDeathCapitalFromLaterPage() {
        String ocr = ""
                + "POLIZA DE SEGURO DE OCASO DECESOS INTEGRAL\n"
                + "Nº de Póliza 4064289\n"
                + "Tomador del Seguro CRISTINA RODRIGUEZ JIMENEZ DOC. ID. 48920227D\n"
                + "RELACION DE ASEGURADOS QUE COMPONEN LA POLIZA\n"
                + "001 48920227D CRISTINA RODRIGUEZ JIMENEZ 28/03/1972 M 01/02/1984\n"
                + "002 27906367F ANGELES GIMENEZ CANTERO 10/12/1954 M 01/04/1957\n"
                + "003 49163555C EDUARDO GOMEZ RODRIGUEZ 03/09/1999 V 14/11/2003\n"
                + "004 JORGE RODRIGUEZ RODRIGUEZ 19/09/2014 V 01/05/2024\n"
                + "005 ALEJANDRI RODRIGUEZ RODRIGUEZ 30/05/2020 V 01/05/2024\n"
                + "GARANTIAS Y COBERTURAS POR ASEGURADO\n"
                + "TOTAL DECESOS: 3.631,55 3.631,55 3.631,55 3.631,55 3.631,55\n"
                + "MUERTE CIRCULACION 5.000,00"
                + "\n";

        List<InsuredPerson> people = DeathPolicyInsuredParser.parse(ocr, "48920227D", "CRISTINA RODRIGUEZ JIMENEZ");

        assertEquals(3, people.size());
        assertEquals("CRISTINA RODRIGUEZ JIMENEZ", people.get(0).getFullName());
        assertEquals("48920227D", people.get(0).getIdentityNumber());
        assertEquals("3.631,55", people.get(0).getCapital());
        assertEquals("5.000,00", people.get(0).getAccidentCapital());
        assertTrue(people.get(0).isHolder());
        assertEquals("3.631,55", people.get(1).getCapital());
    }
}
