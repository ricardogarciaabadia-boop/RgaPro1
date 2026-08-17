package com.rgapro1.ocaso.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DocumentSession {
    public enum Type { IDENTITY, MULTIPAGE }
    public enum Side { FRONT, BACK, PAGE }
    private final Type type;
    private final List<Page> pages = new ArrayList<>();
    private boolean finished;

    public DocumentSession(Type type) { this.type = type; }
    public Type getType() { return type; }
    public void add(String path, String ocrText, Side side) {
        if (finished) throw new IllegalStateException("Session already finished");
        pages.add(new Page(pages.size() + 1, side, path, ocrText));
    }
    public boolean requiresBackSide() { return type == Type.IDENTITY && pages.size() == 1 && pages.get(0).side == Side.FRONT; }
    public boolean canFinish() { return type == Type.MULTIPAGE ? !pages.isEmpty() : pages.size() >= 2; }
    public void finish() { if (!canFinish()) throw new IllegalStateException("Document incomplete"); finished = true; }
    public boolean isFinished() { return finished; }
    public List<Page> getPages() { return Collections.unmodifiableList(pages); }

    public static final class Page {
        public final int number;
        public final Side side;
        public final String path;
        public final String ocrText;
        Page(int number, Side side, String path, String ocrText) { this.number = number; this.side = side; this.path = path; this.ocrText = ocrText; }
    }
}
