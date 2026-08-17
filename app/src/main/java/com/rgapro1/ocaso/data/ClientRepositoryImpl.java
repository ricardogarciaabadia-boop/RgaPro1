package com.rgapro1.ocaso.data;

import android.content.Context;
import com.rgapro1.ocaso.data.local.ClientDao;
import com.rgapro1.ocaso.data.local.ClientEntity;
import com.rgapro1.ocaso.data.local.DatabaseProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientRepositoryImpl {
    private final ClientDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public ClientRepositoryImpl(Context context) {
        dao = DatabaseProvider.get(context).clientDao();
    }

    public void upsert(ClientEntity client) { io.execute(() -> dao.upsert(client)); }

    public void search(String query, String identity, ResultCallback callback) {
        io.execute(() -> {
            List<ClientEntity> result = dao.search(query == null ? "" : query, identity == null ? "" : identity);
            callback.onResult(result);
        });
    }

    public interface ResultCallback { void onResult(List<ClientEntity> result); }
}
