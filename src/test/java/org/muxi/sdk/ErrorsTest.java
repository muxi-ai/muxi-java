package org.muxi.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorsTest {
    @Test
    void testMap401ToAuthenticationException() {
        var ex = Errors.map(401, "INVALID_KEY", "Invalid API key", null);
        assertInstanceOf(Errors.AuthenticationException.class, ex);
        assertEquals(401, ex.getStatusCode());
    }
    
    @Test
    void testMap403ToAuthorizationException() {
        var ex = Errors.map(403, "FORBIDDEN", "Access denied", null);
        assertInstanceOf(Errors.AuthorizationException.class, ex);
    }
    
    @Test
    void testMap404ToNotFoundException() {
        var ex = Errors.map(404, "NOT_FOUND", "Resource not found", null);
        assertInstanceOf(Errors.NotFoundException.class, ex);
    }
    
    @Test
    void testMap429ToRateLimitException() {
        var ex = Errors.map(429, null, "Rate limited", 60);
        assertInstanceOf(Errors.RateLimitException.class, ex);
        assertEquals(60, ex.getRetryAfter());
    }
    
    @Test
    void testMap500ToServerException() {
        var ex = Errors.map(500, "INTERNAL", "Server error", null);
        assertInstanceOf(Errors.ServerException.class, ex);
    }
}
