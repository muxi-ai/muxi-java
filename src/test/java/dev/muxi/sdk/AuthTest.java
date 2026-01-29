package dev.muxi.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthTest {
    @Test
    void testGenerateHmacSignature() {
        String[] result = Auth.generateHmacSignature("GET", "/rpc/status", "key123", "secret456");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertFalse(result[0].isEmpty());
        assertFalse(result[1].isEmpty());
    }
    
    @Test
    void testBuildAuthHeader() {
        String header = Auth.buildAuthHeader("key123", "sig456", "1234567890");
        assertTrue(header.contains("MUXI-HMAC-SHA256"));
        assertTrue(header.contains("key123"));
        assertTrue(header.contains("sig456"));
        assertTrue(header.contains("1234567890"));
    }
    
    @Test
    void testSignatureStripsQueryParams() {
        String[] result1 = Auth.generateHmacSignature("GET", "/path", "key", "secret");
        String[] result2 = Auth.generateHmacSignature("GET", "/path?foo=bar", "key", "secret");
        assertNotNull(result1[0]);
        assertNotNull(result2[0]);
    }
}
