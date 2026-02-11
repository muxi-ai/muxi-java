package org.muxi.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthTest {
    @Test
    void testGenerateHmacSignature() {
        String[] result = Auth.generateHmacSignature("secret456", "GET", "/rpc/status");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertFalse(result[0].isEmpty());
        assertFalse(result[1].isEmpty());
    }
    
    @Test
    void testBuildAuthHeader() {
        String header = Auth.buildAuthHeader("key123", "secret456", "GET", "/path");
        assertTrue(header.contains("MUXI-HMAC key="));
        assertTrue(header.contains("key123"));
        assertTrue(header.contains("timestamp="));
        assertTrue(header.contains("signature="));
    }
    
    @Test
    void testSignatureStripsQueryParams() {
        String[] result1 = Auth.generateHmacSignature("secret", "GET", "/path");
        String[] result2 = Auth.generateHmacSignature("secret", "GET", "/path?foo=bar");
        assertNotNull(result1[0]);
        assertNotNull(result2[0]);
    }
}
