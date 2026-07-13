package org.muxi.sdk;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SseParserTest {
    @Test
    void parseUiWidgetsDecodesUiFrame() {
        var event = new SseEvent("ui",
            "{\"ui\":[{\"type\":\"options\",\"id\":\"w1\",\"prompt\":\"Which?\","
            + "\"options\":[{\"value\":\"us\",\"label\":\"United States\"}]},"
            + "{\"type\":\"action_link\",\"id\":\"w2\",\"label\":\"Dash\",\"url\":\"https://x.io\"}]}");

        var widgets = FormationClient.parseUiWidgets(event);

        assertEquals(2, widgets.size());
        assertEquals("options", widgets.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("United States", widgets.get(0).getAsJsonObject()
            .getAsJsonArray("options").get(0).getAsJsonObject().get("label").getAsString());
        assertEquals("https://x.io", widgets.get(1).getAsJsonObject().get("url").getAsString());
    }

    @Test
    void parseUiWidgetsIgnoresOtherFrames() {
        assertTrue(FormationClient.parseUiWidgets(new SseEvent("message", "hi")).isEmpty());
        assertTrue(FormationClient.parseUiWidgets(new SseEvent("ui", "not json")).isEmpty());
        assertTrue(FormationClient.parseUiWidgets(new SseEvent("ui", "{\"ui\":{}}")).isEmpty());
    }

    @Test
    void unwrapEnvelopeSurfacesIdempotencyKey() {
        var env = JsonParser.parseString(
            "{\"object\":\"api_response\",\"timestamp\":123,"
            + "\"request\":{\"id\":\"req-1\",\"idempotency_key\":\"idem-42\"},"
            + "\"data\":{\"foo\":\"bar\"},\"success\":true}");

        var out = FormationClient.unwrapEnvelope(env);

        assertEquals("bar", out.get("foo").getAsString());
        assertEquals("req-1", out.get("request_id").getAsString());
        assertEquals("idem-42", out.get("idempotency_key").getAsString());
    }

    @Test
    void unwrapEnvelopeOmitsIdempotencyKeyWhenAbsent() {
        var env = JsonParser.parseString(
            "{\"object\":\"api_response\",\"request\":{\"id\":\"req-1\"},"
            + "\"data\":{\"foo\":\"bar\"},\"success\":true}");

        var out = FormationClient.unwrapEnvelope(env);

        assertFalse(out.has("idempotency_key"));
    }

    @Test
    void parsesEventOnlyDoneFrame() {
        var events = SseParser.parseLines(List.of(
            ": keepalive",
            "",
            "event: done",
            ""
        ));

        assertEquals(1, events.size());
        assertEquals("done", events.get(0).event());
        assertEquals("", events.get(0).data());
    }

    @Test
    void preservesMultilineData() {
        var events = SseParser.parseLines(List.of(
            "event: planning",
            "data: one",
            "data: two",
            ""
        ));

        assertEquals(1, events.size());
        assertEquals("planning", events.get(0).event());
        assertEquals("one\ntwo", events.get(0).data());
    }

    @Test
    void routeLevelErrorsThrowMuxiException() {
        var ex = assertThrows(
            Errors.MuxiException.class,
            () -> SseParser.parseLines(List.of("event: error", "data: {\"error\":\"boom\",\"type\":\"RUNTIME_ERROR\"}", ""))
        );

        assertEquals("RUNTIME_ERROR", ex.getErrorCode());
        assertEquals(0, ex.getStatusCode());
    }
}
