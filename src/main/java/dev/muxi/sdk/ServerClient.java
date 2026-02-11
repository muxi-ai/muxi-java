package dev.muxi.sdk;

import com.google.gson.*;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ServerClient {
    private final String baseUrl;
    private final String keyId;
    private final String secretKey;
    private final String app;  // Internal: for Console telemetry
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    
    public ServerClient(String url, String keyId, String secretKey) {
        this(url, keyId, secretKey, 30, 0, null);
    }
    
    public ServerClient(String url, String keyId, String secretKey, int timeout, int maxRetries) {
        this(url, keyId, secretKey, timeout, maxRetries, null);
    }
    
    ServerClient(String url, String keyId, String secretKey, int timeout, int maxRetries, String app) {
        this.baseUrl = url.replaceAll("/+$", "");
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.app = app;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build();
    }
    
    public JsonObject health() throws IOException { return get("/health", false); }
    public JsonObject status() throws IOException { return rpcGet("/rpc/server/status"); }
    public JsonObject listFormations() throws IOException { return rpcGet("/rpc/formations"); }
    public JsonObject getFormation(String formationId) throws IOException { return rpcGet("/rpc/formations/" + formationId); }
    public JsonObject stopFormation(String formationId) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/stop", new JsonObject()); }
    public JsonObject startFormation(String formationId) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/start", new JsonObject()); }
    public JsonObject restartFormation(String formationId) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/restart", new JsonObject()); }
    public JsonObject rollbackFormation(String formationId) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/rollback", new JsonObject()); }
    public JsonObject deleteFormation(String formationId) throws IOException { return rpcDelete("/rpc/formations/" + formationId); }
    public JsonObject cancelUpdate(String formationId) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/cancel-update", new JsonObject()); }
    public JsonObject deployFormation(String formationId, JsonObject payload) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/deploy", payload); }
    public JsonObject updateFormation(String formationId, JsonObject payload) throws IOException { return rpcPost("/rpc/formations/" + formationId + "/update", payload); }
    public JsonArray getFormationLogs(String formationId, Integer limit) throws IOException {
        String path = "/rpc/formations/" + formationId + "/logs" + (limit != null ? "?limit=" + limit : "");
        return rpcGet(path).getAsJsonArray();
    }
    public JsonArray getServerLogs(Integer limit) throws IOException {
        String path = "/rpc/server/logs" + (limit != null ? "?limit=" + limit : "");
        return rpcGet(path).getAsJsonArray();
    }
    
    public void deployFormationStream(String formationId, JsonObject payload, Consumer<SseEvent> handler) throws IOException {
        streamSse("/rpc/formations/" + formationId + "/deploy/stream", payload, handler);
    }
    public void updateFormationStream(String formationId, JsonObject payload, Consumer<SseEvent> handler) throws IOException {
        streamSse("/rpc/formations/" + formationId + "/update/stream", payload, handler);
    }
    public void startFormationStream(String formationId, Consumer<SseEvent> handler) throws IOException {
        streamSse("/rpc/formations/" + formationId + "/start/stream", new JsonObject(), handler);
    }
    public void restartFormationStream(String formationId, Consumer<SseEvent> handler) throws IOException {
        streamSse("/rpc/formations/" + formationId + "/restart/stream", new JsonObject(), handler);
    }
    public void streamFormationLogs(String formationId, Consumer<SseEvent> handler) throws IOException {
        streamSseGet("/rpc/formations/" + formationId + "/logs/stream", handler);
    }
    
    private JsonObject get(String path, boolean auth) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + path).get();
        addHeaders(builder, "GET", path, auth, false);
        return execute(builder.build());
    }
    
    private JsonObject rpcGet(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + path).get();
        addHeaders(builder, "GET", path, true, false);
        return execute(builder.build());
    }
    
    private JsonObject rpcPost(String path, JsonObject body) throws IOException {
        RequestBody reqBody = RequestBody.create(gson.toJson(body), MediaType.parse("application/json"));
        Request.Builder builder = new Request.Builder().url(baseUrl + path).post(reqBody);
        addHeaders(builder, "POST", path, true, true);
        return execute(builder.build());
    }
    
    private JsonObject rpcDelete(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + path).delete();
        addHeaders(builder, "DELETE", path, true, false);
        return execute(builder.build());
    }
    
    private void addHeaders(Request.Builder builder, String method, String path, boolean auth, boolean hasBody) {
        builder.header("X-Muxi-SDK", "java/" + MuxiVersion.VERSION);
        builder.header("X-Muxi-Client", "java/" + MuxiVersion.VERSION);
        builder.header("X-Muxi-Idempotency-Key", UUID.randomUUID().toString());
        builder.header("Accept", "application/json");
        if (app != null && !app.isEmpty()) builder.header("X-Muxi-App", app);
        if (hasBody) builder.header("Content-Type", "application/json");
        if (auth) {
            builder.header("Authorization", Auth.buildAuthHeader(keyId, secretKey, method, path));
        }
    }
    
    private JsonObject execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            // Check for SDK updates (non-blocking, once per process)
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            response.headers().forEach(p -> headers.put(p.getFirst(), p.getSecond()));
            VersionCheck.checkForUpdates(headers);
            
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String code = null, message = "Unknown error";
                try {
                    JsonObject err = gson.fromJson(body, JsonObject.class);
                    code = err.has("code") ? err.get("code").getAsString() : err.has("error") ? err.get("error").getAsString() : null;
                    message = err.has("message") ? err.get("message").getAsString() : message;
                } catch (Exception ignored) {}
                Integer retryAfter = response.header("Retry-After") != null ? Integer.parseInt(response.header("Retry-After")) : null;
                throw Errors.map(response.code(), code, message, retryAfter);
            }
            return body.isEmpty() ? new JsonObject() : gson.fromJson(body, JsonObject.class);
        }
    }
    
    private void streamSse(String path, JsonObject body, Consumer<SseEvent> handler) throws IOException {
        RequestBody reqBody = RequestBody.create(gson.toJson(body), MediaType.parse("application/json"));
        Request.Builder builder = new Request.Builder().url(baseUrl + path).post(reqBody);
        addHeaders(builder, "POST", path, true, true);
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
    
    private void streamSseGet(String path, Consumer<SseEvent> handler) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + path).get();
        addHeaders(builder, "GET", path, true, false);
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
}
