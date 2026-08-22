package com.rgapro1.ocaso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

public class PolicyRelationshipServiceTest {
    @Test
    public void holderCanAlsoBeInsuredAndRolesAreMerged() throws Exception {
        JSONArray clients = new JSONArray();
        JSONObject client = new JSONObject();
        client.put("holder", "CRISTINA RODRIGUEZ JIMENEZ");
        client.put("identityNumber", "48920227D");
        clients.put(client);

        assertTrue(PolicyRelationshipService.linkExistingClient(
                clients, "48920227D", "4064289", "Decesos", "TOMADOR", ""));
        assertTrue(PolicyRelationshipService.linkExistingClient(
                clients, "48920227D", "4064289", "Decesos", "ASEGURADO", "25000 EUR"));

        JSONArray links = client.getJSONArray("policyRelationships");
        assertEquals(1, links.length());
        JSONArray roles = links.getJSONObject(0).getJSONArray("roles");
        assertEquals(2, roles.length());
        assertEquals("25000 EUR", links.getJSONObject(0).getString("capital"));
    }

    @Test
    public void unknownIdentityDoesNotCreateClient() throws Exception {
        JSONArray clients = new JSONArray();
        JSONObject client = new JSONObject();
        client.put("identityNumber", "12345678Z");
        clients.put(client);

        assertFalse(PolicyRelationshipService.linkExistingClient(
                clients, "87654321X", "P-1", "Vida", "ASEGURADO", "10000 EUR"));
        assertEquals(1, clients.length());
    }

    @Test
    public void policyRecordStoresAllInsuredFields() throws Exception {
        JSONObject policy = new JSONObject();
        InsuredPerson person = new InsuredPerson(
                "EDUARDO GOMEZ RODRIGUEZ", "02/03/1980", "12345678Z", "20000 EUR", false);

        PolicyRelationshipService.applyPolicy(
                policy, "4064289", "Decesos", Arrays.asList(person));

        assertEquals("4064289", policy.getString("number"));
        assertEquals(1, policy.getJSONArray("insureds").length());
        JSONObject insured = policy.getJSONArray("insureds").getJSONObject(0);
        assertEquals("12345678Z", insured.getString("identityNumber"));
        assertEquals("02/03/1980", insured.getString("birthDate"));
        assertEquals("20000 EUR", insured.getString("capital"));
    }
}
