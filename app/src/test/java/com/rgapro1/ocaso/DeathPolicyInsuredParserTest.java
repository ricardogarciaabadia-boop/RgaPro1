package com.rgapro1.ocaso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DeathPolicyInsuredParserTest {
    @Test public void parsesHolderAsInsuredAndKeepsCapital() {
        String text = "RELACION DE ASEGURADOS\n" +
                "CRISTINA RODRIGUEZ JIMENEZ 48920227D 14/02/1975 CAPITAL 30.000 EUR\n" +
                "EDUARDO GOMEZ RODRIGUEZ 12345678Z 01/03/1970 CAPITAL 20.000 EUR";

        List<InsuredPerson> people = DeathPolicyInsuredParser.parse(text, "48920227D", "CRISTINA RODRIGUEZ JIMENEZ");

        assertEquals(2, people.size());
        assertTrue(people.get(0).isHolder());
        assertEquals("48920227D", people.get(0).getIdentityNumber());
        assertEquals("30.000 EUR", people.get(0).getCapital());
        assertEquals("01/03/1970", people.get(1).getBirthDate());
    }

    @Test public void doesNotCreateDuplicatePersonForRepeatedIdentity() {
        String text = "RELACION DE ASEGURADOS\n" +
                "ANA GARCIA 12345678Z 01/01/1980 10.000 EUR\n" +
                "ANA GARCIA 12345678Z 01/01/1980 10.000 EUR";

        List<InsuredPerson> people = DeathPolicyInsuredParser.parse(text, "", "");
        assertEquals(1, people.size());
    }
}
