package com.rgapro1.ocaso;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InsuredPersonMinorIdentityTest {
    @Test
    public void under14WithoutDniIsMarkedAsOptional() {
        InsuredPerson person = new InsuredPerson(
                "MENOR GARCIA",
                "15/06/2014",
                "",
                "3.631,55",
                false
        ).withMinorIdentityStatus("01/01/2026");

        assertFalse(person.hasIdentityNumber());
        assertEquals(IdentityStatus.OPTIONAL_FOR_MINOR, person.getIdentityStatus());
    }

    @Test
    public void under14WithDniKeepsTheDni() {
        InsuredPerson person = new InsuredPerson(
                "MENOR GARCIA",
                "15/06/2014",
                "12345678Z",
                "3.631,55",
                false
        ).withMinorIdentityStatus("01/01/2026");

        assertTrue(person.hasIdentityNumber());
        assertEquals("12345678Z", person.getIdentityNumber());
        assertEquals(IdentityStatus.PRESENT, person.getIdentityStatus());
    }

    @Test
    public void fourteenOrOlderWithoutDniNeedsReview() {
        InsuredPerson person = new InsuredPerson(
                "ADULTO GARCIA",
                "15/06/2011",
                "",
                "3.631,55",
                false
        ).withMinorIdentityStatus("01/01/2026");

        assertEquals(IdentityStatus.MISSING_REVIEW, person.getIdentityStatus());
    }
}
