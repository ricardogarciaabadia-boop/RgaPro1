package com.rgapro1.ocaso.ocr;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DocumentClassifierTest {
    private final DocumentClassifier classifier = new DocumentClassifier();

    @Test public void policyContainingDniRemainsPolicy() {
        String text = "POLIZA AUTO\nASEGURADO ANA GARCIA\nDNI 12345678Z\nPRIMA 300 EUR";
        assertEquals(DocumentType.POLICY, classifier.classify(text));
    }

    @Test public void identityDocumentIsDni() {
        String text = "DNI 12345678Z\nAPELLIDOS GARCIA\nNOMBRE ANA\nNACIONALIDAD ESP";
        assertEquals(DocumentType.DNI_NIE, classifier.classify(text));
    }
}
