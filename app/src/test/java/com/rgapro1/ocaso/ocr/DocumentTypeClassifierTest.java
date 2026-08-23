package com.rgapro1.ocaso.ocr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DocumentTypeClassifierTest {
    @Test public void recognizesDni() {
        DocumentTypeClassifier.Result r = DocumentTypeClassifier.classify("DNI 12345678Z\nAPELLIDOS GARCIA\nNOMBRE ANA\nNACIONALIDAD ESP");
        assertEquals(DocumentTypeClassifier.Type.DNI, r.type);
        assertEquals("12345678Z", r.identityNumber);
        assertTrue(r.confidence >= 80);
    }

    @Test public void recognizesNie() {
        DocumentTypeClassifier.Result r = DocumentTypeClassifier.classify("NIE X1234567L\nAPELLIDOS GARCIA\nNOMBRE ANA");
        assertEquals(DocumentTypeClassifier.Type.NIE, r.type);
        assertEquals("X1234567L", r.identityNumber);
    }

    @Test public void recognizesPolicyWithoutMistakingPolicyNumberForDni() {
        DocumentTypeClassifier.Result r = DocumentTypeClassifier.classify("POLIZA Nº 12345678Z\nASEGURADO ANA GARCIA\nFECHA DE EFECTO 01/01/2027\nPRIMA 300 EUR");
        assertEquals(DocumentTypeClassifier.Type.POLICY, r.type);
        assertEquals("", r.identityNumber);
        assertEquals("12345678Z", r.policyNumber);
    }

    @Test public void prefersIdentityWhenIdentitySignalsArePresent() {
        DocumentTypeClassifier.Result r = DocumentTypeClassifier.classify("DNI 12345678Z\nAPELLIDOS GARCIA\nNOMBRE ANA\nPOLIZA 12345");
        assertEquals(DocumentTypeClassifier.Type.DNI, r.type);
    }
}
