package org.muxi.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SseParser {
    private SseParser() {}

    static List<SseEvent> parse(BufferedReader reader) throws IOException {
        List<SseEvent> events = new ArrayList<>();
        String currentEvent = null;
        List<String> dataParts = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(":")) {
                continue;
            }
            if (line.isEmpty()) {
                flush(events, currentEvent, dataParts);
                currentEvent = null;
                dataParts.clear();
                continue;
            }

            String[] field = splitField(line);
            if ("event".equals(field[0])) {
                currentEvent = field[1];
            } else if ("data".equals(field[0])) {
                dataParts.add(field[1]);
            }
        }

        flush(events, currentEvent, dataParts);
        return events;
    }

    static List<SseEvent> parseLines(List<String> lines) {
        List<SseEvent> events = new ArrayList<>();
        String currentEvent = null;
        List<String> dataParts = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(":")) {
                continue;
            }
            if (line.isEmpty()) {
                flush(events, currentEvent, dataParts);
                currentEvent = null;
                dataParts.clear();
                continue;
            }

            String[] field = splitField(line);
            if ("event".equals(field[0])) {
                currentEvent = field[1];
            } else if ("data".equals(field[0])) {
                dataParts.add(field[1]);
            }
        }

        flush(events, currentEvent, dataParts);
        return events;
    }

    static void maybeThrowRouteError(SseEvent event) {
        if (!"error".equals(event.event())) {
            return;
        }

        String code = "STREAM_ERROR";
        String message = event.data() == null || event.data().isEmpty() ? "stream error" : event.data();
        try {
            JsonObject payload = JsonParser.parseString(event.data()).getAsJsonObject();
            if (payload.has("type")) code = payload.get("type").getAsString();
            else if (payload.has("code")) code = payload.get("code").getAsString();
            else if (payload.has("error")) code = payload.get("error").getAsString();

            if (payload.has("error")) message = payload.get("error").getAsString();
            else if (payload.has("message")) message = payload.get("message").getAsString();
        } catch (Exception ignored) {
        }

        throw new Errors.MuxiException(code, message, 0, null);
    }

    private static void flush(List<SseEvent> events, String currentEvent, List<String> dataParts) {
        if (currentEvent == null && dataParts.isEmpty()) {
            return;
        }

        SseEvent event = new SseEvent(currentEvent != null ? currentEvent : "message", String.join("\n", dataParts));
        maybeThrowRouteError(event);
        events.add(event);
    }

    private static String[] splitField(String line) {
        int idx = line.indexOf(':');
        if (idx < 0) return new String[] { line, "" };
        String value = line.substring(idx + 1);
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        return new String[] { line.substring(0, idx), value };
    }
}
