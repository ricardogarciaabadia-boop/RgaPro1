package com.rgapro1.ocaso;

/** Orquestador: primero clasifica, después extrae solo los campos permitidos para ese documento. */
public final class SmartOcrProcessor {
    public static final class Result {
        public final DocumentClassification classification;
        public final DniOcrParser.Result dni;
        public final PolicyOcrParser.Result policy;

        private Result(DocumentClassification classification, DniOcrParser.Result dni, PolicyOcrParser.Result policy) {
            this.classification = classification;
            this.dni = dni;
            this.policy = policy;
        }
    }

    private SmartOcrProcessor() {}

    public static Result process(String rawText) {
        DocumentClassification classification = DocumentClassifier.classify(rawText);
        if (classification.type == DocumentType.DNI_NIE) {
            return new Result(classification, DniOcrParser.parse(rawText), null);
        }
        if (classification.type == DocumentType.POLICY) {
            return new Result(classification, null, PolicyOcrParser.parse(rawText));
        }
        return new Result(classification, null, null);
    }
}
