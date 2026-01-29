package dev.muxi.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebhookTest {
    @Test
    void testVerifySignatureMissingSecret() {
        assertThrows(IllegalArgumentException.class, () -> {
            Webhook.verifySignature("payload", "t=123,v1=abc", "");
        });
    }
    
    @Test
    void testVerifySignatureNullHeader() {
        assertFalse(Webhook.verifySignature("payload", null, "secret"));
    }
    
    @Test
    void testVerifySignatureEmptyHeader() {
        assertFalse(Webhook.verifySignature("payload", "", "secret"));
    }
    
    @Test
    void testVerifySignatureInvalidSignature() {
        long ts = System.currentTimeMillis() / 1000;
        String header = "t=" + ts + ",v1=invalidsignature";
        assertFalse(Webhook.verifySignature("payload", header, "secret"));
    }
    
    @Test
    void testParseCompletedPayload() {
        String payload = "{\"status\":\"completed\",\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}";
        var event = Webhook.parse(payload);
        assertEquals("completed", event.status);
        assertNotNull(event.content);
        assertEquals(1, event.content.size());
    }
    
    @Test
    void testParseFailedPayload() {
        String payload = "{\"status\":\"failed\",\"error\":{\"code\":\"ERROR\",\"message\":\"Something went wrong\"}}";
        var event = Webhook.parse(payload);
        assertEquals("failed", event.status);
        assertNotNull(event.error);
        assertEquals("ERROR", event.error.code);
    }
    
    @Test
    void testParseClarificationPayload() {
        String payload = "{\"status\":\"awaiting_clarification\",\"clarification\":{\"question\":\"Which one?\",\"options\":[\"A\",\"B\"]}}";
        var event = Webhook.parse(payload);
        assertEquals("awaiting_clarification", event.status);
        assertNotNull(event.clarification);
        assertEquals("Which one?", event.clarification.question);
    }
}
