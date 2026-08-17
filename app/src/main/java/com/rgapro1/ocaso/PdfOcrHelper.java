package com.rgapro1.ocaso;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class PdfOcrHelper {
    public interface Callback {
        void onSuccess(String text);
        void onError(Exception error);
    }

    private PdfOcrHelper() {}

    public static void process(Context context, Uri uri, Callback callback) {
        new Thread(() -> {
            ParcelFileDescriptor pfd = null;
            android.graphics.pdf.PdfRenderer renderer = null;
            try {
                pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd == null) throw new IOException("No se pudo abrir el PDF");
                renderer = new android.graphics.pdf.PdfRenderer(pfd);
                final int pages = renderer.getPageCount();
                if (pages == 0) throw new IOException("El PDF no contiene páginas");

                final String[] pageTexts = new String[pages];
                final Exception[] pageErrors = new Exception[pages];
                final AtomicInteger remaining = new AtomicInteger(pages);
                final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                for (int i = 0; i < pages; i++) {
                    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(i);
                    int width = Math.max(1200, page.getWidth() * 2);
                    int height = Math.max(1600, page.getHeight() * 2);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
                    final int index = i;
                    recognizer.process(image)
                            .addOnSuccessListener(result -> pageTexts[index] = result.getText())
                            .addOnFailureListener(error -> pageErrors[index] = error)
                            .addOnCompleteListener(task -> {
                                bitmap.recycle();
                                if (remaining.decrementAndGet() == 0) {
                                    recognizer.close();
                                    StringBuilder all = new StringBuilder();
                                    boolean hasError = false;
                                    for (int p = 0; p < pages; p++) {
                                        all.append("\n--- Página ").append(p + 1).append(" ---\n");
                                        if (pageTexts[p] != null) all.append(pageTexts[p]);
                                        if (pageErrors[p] != null) {
                                            hasError = true;
                                            all.append("[No se pudo leer esta página]");
                                        }
                                        all.append('\n');
                                    }
                                    if (hasError && all.toString().trim().isEmpty()) {
                                        callback.onError(new IOException("No se pudo leer ninguna página del PDF"));
                                    } else {
                                        callback.onSuccess(all.toString().trim());
                                    }
                                }
                            });
                }
                renderer.close();
                pfd.close();
            } catch (Exception e) {
                try { if (renderer != null) renderer.close(); } catch (Exception ignored) {}
                try { if (pfd != null) pfd.close(); } catch (Exception ignored) {}
                callback.onError(e);
            }
        }).start();
    }
}
