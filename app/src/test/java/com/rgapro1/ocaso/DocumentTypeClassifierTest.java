package com.rgapro1.ocaso;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DocumentTypeClassifierTest {
    @Test public void deathPolicyWinsOverEmbeddedDni() {
        assertEquals(DocumentTypeClassifier.Type.POLICY_DEATH,
                DocumentTypeClassifier.classify("POLIZA DE SEGURO DECESOS DOC. ID. 48920227D RELACION DE ASEGURADOS"));
    }
    @Test public void lifePolicyIsNotDni() {
        assertEquals(DocumentTypeClassifier.Type.POLICY_LIFE,
                DocumentTypeClassifier.classify("POLIZA DE SEGURO DE VIDA TOMADOR DEL SEGURO DOC. ID. 54791014T"));
    }
    @Test public void dniDocumentIsIdentity() {
        assertEquals(DocumentTypeClassifier.Type.DNI_NIE,
                DocumentTypeClassifier.classify("DNI 54791014T NOMBRE RICARDO GARCIA"));
    }
}
