private static void enhanceSavedImages(Context context, JSONArray source) {
    int processed = 0;

    for (int i = 0; i < source.length() && processed < 8; i++) {
        JSONObject client = source.optJSONObject(i);
        if (client == null) continue;

        JSONArray docs = client.optJSONArray("documentPhotos");
        if (docs == null) continue;

        for (int j = 0; j < docs.length() && processed < 8; j++) {
            JSONObject doc = docs.optJSONObject(j);
            if (doc == null || doc.optBoolean("ocrEnhanced", false)) continue;

            String path = doc.optString("path", "");

            try {
                if (path.isEmpty()
                        || path.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    doc.put("ocrEnhanced", true);
                    continue;
                }

                File f = new File(path);

                if (!f.exists()) {
                    doc.put("ocrEnhanced", true);
                    continue;
                }

                InputImage image =
                        InputImage.fromFilePath(context, Uri.fromFile(f));

                TextRecognizer recognizer =
                        TextRecognition.getClient(
                                TextRecognizerOptions.DEFAULT_OPTIONS
                        );

                String text;

                try {
                    text = Tasks.await(
                            recognizer.process(image)
                    ).getText();
                } finally {
                    recognizer.close();
                }

                if (text != null && !text.trim().isEmpty()) {
                    String old = client.optString("ocrText", "");

                    if (!old.contains(text.trim())) {
                        client.put(
                                "ocrText",
                                (old.isEmpty()
                                        ? ""
                                        : old
                                                + "\n\n--- OCR SEGUNDA PASADA ---\n")
                                        + text.trim()
                        );
                    }
                }

                doc.put("ocrEnhanced", true);
                processed++;

            } catch (Exception ignored) {
                // Leave it pending so the next cycle can retry.
            }
        }
    }
}
