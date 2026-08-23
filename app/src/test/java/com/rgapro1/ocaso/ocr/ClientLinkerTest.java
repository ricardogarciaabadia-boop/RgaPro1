package com.rgapro1.ocaso.ocr;

import static org.junit.Assert.assertEquals;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ClientLinkerTest {
    @Test public void linksPolicyDniToExistingClient() throws Exception {
        JSONArray records = new JSONArray();
        records.put(new JSONObject().put("holder", "Ana Garcia").put("identityNumber", "12345678Z"));
        records.put(new JSONObject().put("holder", "Luis Perez").put("identityNumber", "87654321X"));
        assertEquals(0, new ClientLinker().findClientIndex(records, "12345678-Z"));
    }

    @Test public void unknownDniDoesNotCreateAnIndex() throws Exception {
        JSONArray records = new JSONArray();
        records.put(new JSONObject().put("holder", "Ana Garcia").put("identityNumber", "12345678Z"));
        assertEquals(-1, new ClientLinker().findClientIndex(records, "99999999R"));
    }
}
