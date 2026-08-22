package com.rgapro1.ocaso;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeathPolicyMinorParsingIntegrationTest {
    @Test
    public void deathPolicyKeepsMinorWithoutDniAndDoesNotTreatItAsAnError() {
        String ocr = "" +
                "RELACIÓN DE ASEGURADOS\n" +
                "1 CRISTINA RODRIGUEZ JIMENEZ 48920227D 28/03/1972 3.631,55 EUR\n" +
                "2 MENOR RODRIGUEZ 15/06/2014 3.631,55 EUR\n";

        List<InsuredPerson> people = DeathPolicyInsuredParser.parse(
                ocr,
                "48920227D",
                "CRISTINA RODRIGUEZ JIMENEZ",
                "01/01/2026"
        );

        assertEquals(2, people.size());
        InsuredPerson minor = people.get(1);
        assertEquals("MENOR RODRIGUEZ", minor.getFullName());
        assertEquals("15/06/2014", minor.getBirthDate());
        assertTrue(minor.getIdentityNumber().isEmpty());
        assertEquals(IdentityStatus.OPTIONAL_FOR_MINOR, minor.getIdentityStatus());
    }
}
