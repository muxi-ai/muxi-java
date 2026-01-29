package dev.muxi.sdk;

import com.google.gson.*;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FormationClient {
    private final String baseUrl;
    private final String adminKey;
    private final String clientKey;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    
    public FormationClient(String serverUrl, String formationId, String clientKey, String adminKey) {
        this(serverUrl, formationId, clientKey, adminKey, 30);
    }
    
    public FormationClient(String serverUrl, String formationId, String clientKey, String adminKey, int timeout) {
        this.baseUrl = serverUrl.replaceAll("/+$", "") + "/api/" + formationId + "/v1";
        this.adminKey = adminKey;
        this.clientKey = clientKey;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build();
    }
    
    public static FormationClient withBaseUrl(String baseUrl, String clientKey, String adminKey) {
        FormationClient fc = new FormationClient("http://placeholder", "x", clientKey, adminKey);
        try {
            var field = FormationClient.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(fc, baseUrl.replaceAll("/+$", ""));
        } catch (Exception e) { throw new RuntimeException(e); }
        return fc;
    }
    
    // Health / Status
    public JsonObject health() throws IOException { return request("GET", "/health", null, null, false, null); }
    public JsonObject getStatus() throws IOException { return request("GET", "/status", null, null, true, null); }
    public JsonObject getConfig() throws IOException { return request("GET", "/config", null, null, true, null); }
    public JsonObject getFormationInfo() throws IOException { return request("GET", "/formation", null, null, true, null); }
    
    // Agents / MCP
    public JsonArray getAgents() throws IOException { return request("GET", "/agents", null, null, true, null).getAsJsonArray(); }
    public JsonObject getAgent(String agentId) throws IOException { return request("GET", "/agents/" + agentId, null, null, true, null); }
    public JsonArray getMcpServers() throws IOException { return request("GET", "/mcp/servers", null, null, true, null).getAsJsonArray(); }
    public JsonObject getMcpServer(String serverId) throws IOException { return request("GET", "/mcp/servers/" + serverId, null, null, true, null); }
    public JsonArray getMcpTools() throws IOException { return request("GET", "/mcp/tools", null, null, true, null).getAsJsonArray(); }
    
    // Secrets
    public JsonArray getSecrets() throws IOException { return request("GET", "/secrets", null, null, true, null).getAsJsonArray(); }
    public JsonObject getSecret(String key) throws IOException { return request("GET", "/secrets/" + key, null, null, true, null); }
    public void setSecret(String key, String value) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("value", value);
        request("PUT", "/secrets/" + key, null, body, true, null);
    }
    public void deleteSecret(String key) throws IOException { request("DELETE", "/secrets/" + key, null, null, true, null); }
    
    // Chat
    public JsonObject chat(JsonObject payload, String userId) throws IOException { return request("POST", "/chat", null, payload, false, userId); }
    public void chatStream(JsonObject payload, String userId, Consumer<SseEvent> handler) throws IOException {
        payload.addProperty("stream", true);
        streamSse("POST", "/chat", payload, false, userId, handler);
    }
    public JsonObject audioChat(JsonObject payload, String userId) throws IOException { return request("POST", "/audiochat", null, payload, false, userId); }
    public void audioChatStream(JsonObject payload, String userId, Consumer<SseEvent> handler) throws IOException {
        payload.addProperty("stream", true);
        streamSse("POST", "/audiochat", payload, false, userId, handler);
    }
    
    // Sessions
    public JsonArray getSessions(String userId, Integer limit) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        if (limit != null) params.put("limit", limit);
        return request("GET", "/sessions", params, null, false, userId).getAsJsonArray();
    }
    public JsonObject getSession(String sessionId, String userId) throws IOException { return request("GET", "/sessions/" + sessionId, null, null, false, userId); }
    public JsonArray getSessionMessages(String sessionId, String userId) throws IOException { return request("GET", "/sessions/" + sessionId + "/messages", null, null, false, userId).getAsJsonArray(); }
    public void restoreSession(String sessionId, String userId, JsonArray messages) throws IOException {
        JsonObject body = new JsonObject(); body.add("messages", messages);
        request("POST", "/sessions/" + sessionId + "/restore", null, body, false, userId);
    }
    
    // Requests
    public JsonArray getRequests(String userId) throws IOException { return request("GET", "/requests", null, null, false, userId).getAsJsonArray(); }
    public JsonObject getRequestStatus(String requestId, String userId) throws IOException { return request("GET", "/requests/" + requestId, null, null, false, userId); }
    public void cancelRequest(String requestId, String userId) throws IOException { request("DELETE", "/requests/" + requestId, null, null, false, userId); }
    
    // Memory
    public JsonObject getMemoryConfig() throws IOException { return request("GET", "/memory", null, null, true, null); }
    public JsonArray getMemories(String userId, Integer limit) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        if (limit != null) params.put("limit", limit);
        return request("GET", "/memories", params, null, false, userId).getAsJsonArray();
    }
    public JsonObject addMemory(String userId, String type, String detail) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("type", type);
        body.addProperty("detail", detail);
        return request("POST", "/memories", null, body, false, userId);
    }
    public void deleteMemory(String userId, String memoryId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        request("DELETE", "/memories/" + memoryId, params, null, false, userId);
    }
    public JsonObject getUserBuffer(String userId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        return request("GET", "/memory/buffer", params, null, false, userId);
    }
    public JsonObject clearUserBuffer(String userId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        return request("DELETE", "/memory/buffer", params, null, false, userId);
    }
    public JsonObject clearSessionBuffer(String userId, String sessionId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        return request("DELETE", "/memory/buffer/" + sessionId, params, null, false, userId);
    }
    public JsonObject clearAllBuffers() throws IOException { return request("DELETE", "/memory/buffer", null, null, true, null); }
    public JsonObject getBufferStats() throws IOException { return request("GET", "/memory/stats", null, null, true, null); }
    
    // Scheduler
    public JsonObject getSchedulerConfig() throws IOException { return request("GET", "/scheduler", null, null, true, null); }
    public JsonArray getSchedulerJobs(String userId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        return request("GET", "/scheduler/jobs", params, null, true, null).getAsJsonArray();
    }
    public JsonObject getSchedulerJob(String jobId) throws IOException { return request("GET", "/scheduler/jobs/" + jobId, null, null, true, null); }
    public JsonObject createSchedulerJob(String type, String schedule, String message, String userId) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("type", type);
        body.addProperty("schedule", schedule);
        body.addProperty("message", message);
        body.addProperty("user_id", userId);
        return request("POST", "/scheduler/jobs", null, body, true, null);
    }
    public void deleteSchedulerJob(String jobId) throws IOException { request("DELETE", "/scheduler/jobs/" + jobId, null, null, true, null); }
    
    // Config endpoints
    public JsonObject getAsyncConfig() throws IOException { return request("GET", "/async", null, null, true, null); }
    public JsonObject getA2aConfig() throws IOException { return request("GET", "/a2a", null, null, true, null); }
    public JsonObject getLoggingConfig() throws IOException { return request("GET", "/logging", null, null, true, null); }
    public JsonArray getLoggingDestinations() throws IOException { return request("GET", "/logging/destinations", null, null, true, null).getAsJsonArray(); }
    
    // Credentials
    public JsonArray listCredentialServices() throws IOException { return request("GET", "/credentials/services", null, null, true, null).getAsJsonArray(); }
    public JsonArray listCredentials(String userId) throws IOException { return request("GET", "/credentials", null, null, false, userId).getAsJsonArray(); }
    public JsonObject getCredential(String credentialId, String userId) throws IOException { return request("GET", "/credentials/" + credentialId, null, null, false, userId); }
    public JsonObject createCredential(String userId, JsonObject payload) throws IOException { return request("POST", "/credentials", null, payload, false, userId); }
    public JsonObject deleteCredential(String credentialId, String userId) throws IOException { return request("DELETE", "/credentials/" + credentialId, null, null, false, userId); }
    
    // User identifiers
    public JsonArray getUserIdentifiersForUser(String userId) throws IOException { return request("GET", "/users/identifiers/" + userId, null, null, true, null).getAsJsonArray(); }
    public JsonObject linkUserIdentifier(String muxiUserId, JsonArray identifiers) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("muxi_user_id", muxiUserId);
        body.add("identifiers", identifiers);
        return request("POST", "/users/identifiers", null, body, true, null);
    }
    public void unlinkUserIdentifier(String identifier) throws IOException { request("DELETE", "/users/identifiers/" + identifier, null, null, true, null); }
    
    // Overlord / LLM
    public JsonObject getOverlordConfig() throws IOException { return request("GET", "/overlord", null, null, true, null); }
    public JsonObject getOverlordPersona() throws IOException { return request("GET", "/overlord/persona", null, null, true, null); }
    public JsonObject getLlmSettings() throws IOException { return request("GET", "/llm/settings", null, null, true, null); }
    
    // Triggers / SOP / Audit
    public JsonArray getTriggers() throws IOException { return request("GET", "/triggers", null, null, false, null).getAsJsonArray(); }
    public JsonObject getTrigger(String name) throws IOException { return request("GET", "/triggers/" + name, null, null, false, null); }
    public JsonObject fireTrigger(String name, JsonObject data, boolean async, String userId) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("async", async ? "true" : "false");
        return request("POST", "/triggers/" + name, params, data, false, userId);
    }
    public JsonArray getSops() throws IOException { return request("GET", "/sops", null, null, false, null).getAsJsonArray(); }
    public JsonObject getSop(String name) throws IOException { return request("GET", "/sops/" + name, null, null, false, null); }
    public JsonArray getAuditLog() throws IOException { return request("GET", "/audit", null, null, true, null).getAsJsonArray(); }
    public void clearAuditLog() throws IOException { request("DELETE", "/audit?confirm=clear-audit-log", null, null, true, null); }
    
    // Streaming
    public void streamEvents(String userId, Consumer<SseEvent> handler) throws IOException {
        Map<String, Object> params = new HashMap<>(); params.put("user_id", userId);
        streamSseGet("/events", params, false, userId, handler);
    }
    public void streamRequest(String userId, String sessionId, String requestId, Consumer<SseEvent> handler) throws IOException {
        streamSseGet("/events/" + sessionId + "/" + requestId, null, false, userId, handler);
    }
    public void streamLogs(Map<String, Object> filters, Consumer<SseEvent> handler) throws IOException {
        streamSseGet("/logs", filters, true, null, handler);
    }
    
    // Resolve user
    public JsonObject resolveUser(String identifier, boolean createUser) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("identifier", identifier);
        body.addProperty("create_user", createUser);
        return request("POST", "/users/resolve", null, body, false, null);
    }
    
    private JsonObject request(String method, String path, Map<String, Object> params, JsonObject body, boolean useAdmin, String userId) throws IOException {
        String url = buildUrl(path, params);
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, useAdmin, userId, body != null);
        
        RequestBody reqBody = body != null ? RequestBody.create(gson.toJson(body), MediaType.parse("application/json")) : null;
        switch (method) {
            case "GET" -> builder.get();
            case "POST" -> builder.post(reqBody != null ? reqBody : RequestBody.create("", null));
            case "PUT" -> builder.put(reqBody != null ? reqBody : RequestBody.create("", null));
            case "DELETE" -> { if (reqBody != null) builder.delete(reqBody); else builder.delete(); }
        }
        
        try (Response response = client.newCall(builder.build()).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String code = null, message = "Unknown error";
                try {
                    JsonObject err = gson.fromJson(respBody, JsonObject.class);
                    code = err.has("code") ? err.get("code").getAsString() : err.has("error") ? err.get("error").getAsString() : null;
                    message = err.has("message") ? err.get("message").getAsString() : message;
                } catch (Exception ignored) {}
                Integer retryAfter = response.header("Retry-After") != null ? Integer.parseInt(response.header("Retry-After")) : null;
                throw Errors.map(response.code(), code, message, retryAfter);
            }
            if (respBody.isEmpty()) return new JsonObject();
            JsonElement parsed = gson.fromJson(respBody, JsonElement.class);
            return unwrapEnvelope(parsed);
        }
    }
    
    private void streamSse(String method, String path, JsonObject body, boolean useAdmin, String userId, Consumer<SseEvent> handler) throws IOException {
        String url = buildUrl(path, null);
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, useAdmin, userId, true);
        builder.header("Accept", "text/event-stream");
        
        RequestBody reqBody = RequestBody.create(gson.toJson(body), MediaType.parse("application/json"));
        if (method.equals("POST")) builder.post(reqBody);
        
        OkHttpClient streamClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        try (Response response = streamClient.newCall(builder.build()).execute()) {
            if (response.body() == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                String currentEvent = null;
                List<String> dataParts = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(":")) continue;
                    if (line.isEmpty()) {
                        if (!dataParts.isEmpty()) handler.accept(new SseEvent(currentEvent != null ? currentEvent : "message", String.join("\n", dataParts)));
                        currentEvent = null; dataParts.clear(); continue;
                    }
                    if (line.startsWith("event:")) currentEvent = line.substring(6).trim();
                    else if (line.startsWith("data:")) dataParts.add(line.substring(5).trim());
                }
            }
        }
    }
    
    private void streamSseGet(String path, Map<String, Object> params, boolean useAdmin, String userId, Consumer<SseEvent> handler) throws IOException {
        String url = buildUrl(path, params);
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, useAdmin, userId, false);
        builder.header("Accept", "text/event-stream");
        
        OkHttpClient streamClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        try (Response response = streamClient.newCall(builder.build()).execute()) {
            if (response.body() == null) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                String currentEvent = null;
                List<String> dataParts = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(":")) continue;
                    if (line.isEmpty()) {
                        if (!dataParts.isEmpty()) handler.accept(new SseEvent(currentEvent != null ? currentEvent : "message", String.join("\n", dataParts)));
                        currentEvent = null; dataParts.clear(); continue;
                    }
                    if (line.startsWith("event:")) currentEvent = line.substring(6).trim();
                    else if (line.startsWith("data:")) dataParts.add(line.substring(5).trim());
                }
            }
        }
    }
    
    private String buildUrl(String path, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (!path.startsWith("/")) url.append("/");
        url.append(path);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((k, v) -> { if (v != null) url.append(k).append("=").append(v).append("&"); });
            url.setLength(url.length() - 1);
        }
        return url.toString();
    }
    
    private void addHeaders(Request.Builder builder, boolean useAdmin, String userId, boolean hasBody) {
        builder.header("X-Muxi-SDK", "java/" + MuxiVersion.VERSION);
        builder.header("X-Muxi-Client", "java/" + MuxiVersion.VERSION);
        builder.header("X-Muxi-Idempotency-Key", UUID.randomUUID().toString());
        builder.header("Accept", "application/json");
        if (useAdmin) {
            if (adminKey == null) throw new IllegalArgumentException("admin key required");
            builder.header("X-MUXI-ADMIN-KEY", adminKey);
        } else {
            if (clientKey == null) throw new IllegalArgumentException("client key required");
            builder.header("X-MUXI-CLIENT-KEY", clientKey);
        }
        if (userId != null && !userId.isEmpty()) builder.header("X-Muxi-User-ID", userId);
        if (hasBody) builder.header("Content-Type", "application/json");
    }
    
    private JsonObject unwrapEnvelope(JsonElement elem) {
        if (!elem.isJsonObject()) return elem.getAsJsonObject();
        JsonObject obj = elem.getAsJsonObject();
        if (!obj.has("data")) return obj;
        JsonElement data = obj.get("data");
        if (data.isJsonObject()) {
            JsonObject result = data.getAsJsonObject().deepCopy();
            if (obj.has("request") && obj.get("request").isJsonObject()) {
                JsonObject req = obj.get("request").getAsJsonObject();
                if (req.has("id") && !result.has("request_id")) result.addProperty("request_id", req.get("id").getAsString());
            } else if (obj.has("request_id") && !result.has("request_id")) {
                result.add("request_id", obj.get("request_id"));
            }
            if (obj.has("timestamp") && !result.has("timestamp")) result.add("timestamp", obj.get("timestamp"));
            return result;
        }
        return data.isJsonObject() ? data.getAsJsonObject() : obj;
    }
}
