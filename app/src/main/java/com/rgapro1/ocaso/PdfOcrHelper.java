package com.rgapro1.ocaso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;

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

                StringBuilder all = new StringBuilder();
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                AtomicInteger remaining = new AtomicInteger(pages);
                for (int i = 0; i < pages; i++) {
                    android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(i);
                    int width = Math.max(1200, page.getWidth() * 2);
                    int height = Math.max(1600, page.getHeight() * 2);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
                    final int pageNumber = i + 1;
                    recognizer.process(image)
                            .addOnSuccessListener(result -> {
                                synchronized (all) {
                                    all.append("\n--- Página ").append(pageNumber).append(" ---\n").append(result.getText()).append('\n');
                                }
                                bitmap.recycle();
                                if (remaining.decrementAndGet() == 0) {
                                    recognizer.close();
                                    callback.onSuccess(all.toString().trim());
                                }
                            })
                            .addOnFailureListener(error -> {
                                bitmap.recycle();
                                if (remaining.decrementAndGet() == 0) recognizer.close();
                                callback.onError(error);
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
