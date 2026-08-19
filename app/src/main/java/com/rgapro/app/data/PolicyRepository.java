package com.rgapro.app.data;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PolicyRepository {
    public interface Callback<T> { void onResult(T value); }

    private final PolicyDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public PolicyRepository(PolicyDao dao) { this.dao = dao; }

    public void getAll(Callback<List<PolicyEntity>> callback) {
        io.execute(() -> callback.onResult(dao.getAll()));
    }

    public void search(String text, Callback<List<PolicyEntity>> callback) {
        String q = "%" + (text == null ? "" : text.trim()) + "%";
        io.execute(() -> callback.onResult(dao.search(q)));
    }

    public void insert(PolicyEntity policy, Callback<Long> callback) {
        io.execute(() -> callback.onResult(dao.insert(policy)));
    }
}
