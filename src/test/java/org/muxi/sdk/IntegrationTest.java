package org.muxi.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    
    private ServerClient serverClient;
    private FormationClient formationClient;
    private boolean configured = false;
    
    private static String env(String name) {
        return System.getenv(name);
    }
    
    private static String requireEnv(String name) {
        String value = env(name);
        assumeTrue(value != null && !value.isEmpty(), name + " not set");
        return value;
    }
    
    @BeforeAll
    void setup() {
        try {
            String serverUrl = requireEnv("MUXI_SDK_E2E_SERVER_URL");
            String keyId = requireEnv("MUXI_SDK_E2E_KEY_ID");
            String secretKey = requireEnv("MUXI_SDK_E2E_SECRET_KEY");
            String formationId = requireEnv("MUXI_SDK_E2E_FORMATION_ID");
            String clientKey = requireEnv("MUXI_SDK_E2E_CLIENT_KEY");
            String adminKey = requireEnv("MUXI_SDK_E2E_ADMIN_KEY");
            
            serverClient = new ServerClient(serverUrl, keyId, secretKey);
            formationClient = new FormationClient(serverUrl, formationId, clientKey, adminKey);
            
            configured = true;
        } catch (Exception e) {
            // Will skip tests
        }
    }
    
    // Java SDK doesn't have ping() - use health() instead
    
    @Test
    void testServerHealth() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = serverClient.health();
        assertNotNull(result);
    }
    
    @Test
    void testServerStatus() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = serverClient.status();
        assertNotNull(result);
    }
    
    @Test
    void testServerListFormations() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = serverClient.listFormations();
        assertNotNull(result);
        assertTrue(result.has("data"));
    }
    
    @Test
    void testFormationHealth() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = formationClient.health();
        assertNotNull(result);
    }
    
    @Test
    void testFormationGetStatus() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = formationClient.getStatus();
        assertNotNull(result);
    }
    
    @Test
    void testFormationGetConfig() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = formationClient.getConfig();
        assertNotNull(result);
    }
    
    @Test
    void testFormationGetAgents() throws Exception {
        assumeTrue(configured, "Not configured");
        var result = formationClient.getAgents();
        assertNotNull(result);
    }
}
