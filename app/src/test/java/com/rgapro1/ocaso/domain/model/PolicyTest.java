package com.rgapro1.ocaso.domain.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PolicyTest {
    @Test public void nullValuesBecomeEmpty() {
        Policy p = new Policy(null, null, null, null, null, null, 0L);
        assertEquals("", p.getHolder());
        assertEquals("", p.getType());
        assertEquals("", p.getNumber());
        assertEquals("", p.getIdentityNumber());
        assertEquals("", p.getExpiry());
        assertEquals("", p.getOcrText());
    }
}
