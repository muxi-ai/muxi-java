package org.muxi.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SseParserTest {
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
