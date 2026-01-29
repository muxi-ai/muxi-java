# MUXI Java SDK User Guide

## Installation

### Gradle

```kotlin
dependencies {
    implementation("dev.muxi:muxi-java:0.1.0")
}
```

### Maven

```xml
<dependency>
    <groupId>dev.muxi</groupId>
    <artifactId>muxi-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quickstart

```java
import dev.muxi.sdk.ServerClient;
import dev.muxi.sdk.FormationClient;

// Server client (management, HMAC auth)
ServerClient server = new ServerClient(
    "https://server.example.com",
    "<key_id>",
    "<secret_key>"
);
System.out.println(server.status());

// Formation client (runtime, key auth)
FormationClient formation = new FormationClient(
    "https://server.example.com",
    "<formation_id>",
    "<client_key>",
    "<admin_key>"
);
System.out.println(formation.health());
```

## Clients

- **ServerClient** (management, HMAC): deploy/list/update formations, server health/status, server logs.
- **FormationClient** (runtime, client/admin keys): chat/audio (streaming), agents, secrets, MCP, memory, scheduler, sessions/requests, identifiers, credentials, triggers/SOPs/audit, async/A2A/logging config, overlord/LLM settings, events/logs streaming.

## Streaming

```java
import dev.muxi.sdk.SseEvent;
import com.google.gson.JsonObject;

// Chat streaming
JsonObject request = new JsonObject();
request.addProperty("message", "Tell me a story");

formation.chatStream(request, "user-123", event -> {
    if ("message".equals(event.getEvent())) {
        System.out.println(event.getData());
    }
});

// Event streaming
formation.streamEvents("user-123", event -> {
    System.out.println(event.getData());
});

// Log streaming (admin)
formation.streamLogs("info", event -> {
    System.out.println(event.getData());
});
```

## Auth & Headers

- **ServerClient**: HMAC with `keyId`/`secretKey` on `/rpc` endpoints.
- **FormationClient**: `X-MUXI-CLIENT-KEY` or `X-MUXI-ADMIN-KEY` on `/api/{formation}/v1`. Override `baseUrl` for direct access (e.g., `http://localhost:9012/v1`).
- **Idempotency**: `X-Muxi-Idempotency-Key` auto-generated on every request.
- **SDK headers**: `X-Muxi-SDK`, `X-Muxi-Client` set automatically.

## Timeouts & Retries

- Default timeout: 30s (no timeout for streaming).
- Retries: `maxRetries` with exponential backoff on 429/5xx/connection errors; respects `Retry-After`.

## Error Handling

```java
import dev.muxi.sdk.Errors.*;

try {
    formation.chat(request, "user-123");
} catch (AuthenticationException e) {
    System.out.println("Auth failed: " + e.getMessage());
} catch (RateLimitException e) {
    System.out.println("Rate limited. Retry after: " + e.getRetryAfter() + "s");
} catch (NotFoundException e) {
    System.out.println("Not found: " + e.getMessage());
} catch (MuxiException e) {
    System.out.println(e.getErrorCode() + ": " + e.getMessage() + " (" + e.getStatusCode() + ")");
}
```

Error types: `AuthenticationException`, `AuthorizationException`, `NotFoundException`, `ValidationException`, `RateLimitException`, `ServerException`, `ConflictException`, `ConnectionException`.

## Notable Endpoints (FormationClient)

| Category | Methods |
|----------|---------|
| Chat/Audio | `chat`, `chatStream`, `audioChat`, `audioChatStream` |
| Memory | `getMemoryConfig`, `getMemories`, `addMemory`, `deleteMemory`, `getUserBuffer`, `clearUserBuffer`, `clearSessionBuffer`, `clearAllBuffers`, `getBufferStats` |
| Scheduler | `getSchedulerConfig`, `getSchedulerJobs`, `getSchedulerJob`, `createSchedulerJob`, `deleteSchedulerJob` |
| Sessions | `getSessions`, `getSession`, `getSessionMessages`, `restoreSession` |
| Requests | `getRequests`, `getRequestStatus`, `cancelRequest` |
| Agents/MCP | `getAgents`, `getAgent`, `getMcpServers`, `getMcpServer`, `getMcpTools` |
| Secrets | `getSecrets`, `getSecret`, `setSecret`, `deleteSecret` |
| Credentials | `listCredentialServices`, `listCredentials`, `getCredential`, `createCredential`, `deleteCredential` |
| Identifiers | `getUserIdentifiersForUser`, `linkUserIdentifier`, `unlinkUserIdentifier` |
| Triggers/SOP | `getTriggers`, `getTrigger`, `fireTrigger`, `getSops`, `getSop` |
| Audit | `getAuditLog`, `clearAuditLog` |
| Config | `getStatus`, `getConfig`, `getFormationInfo`, `getAsyncConfig`, `getA2aConfig`, `getLoggingConfig`, `getLoggingDestinations`, `getOverlordConfig`, `getOverlordPersona`, `getLlmSettings` |
| Streaming | `streamEvents`, `streamLogs`, `streamRequest` |
| User | `resolveUser` |

## Webhook Verification

```java
import dev.muxi.sdk.Webhook;
import dev.muxi.sdk.Webhook.WebhookEvent;

// In your HTTP handler
String payload = request.getBody();
String signature = request.getHeader("X-Muxi-Signature");
String secret = System.getenv("WEBHOOK_SECRET");

if (!Webhook.verifySignature(payload, signature, secret)) {
    response.setStatus(401);
    return;
}

WebhookEvent event = Webhook.parse(payload);

switch (event.getStatus()) {
    case "completed":
        for (WebhookEvent.ContentItem item : event.getContent()) {
            if ("text".equals(item.getType())) {
                System.out.println(item.getText());
            }
        }
        break;
    case "failed":
        System.out.println("Error: " + event.getError().getMessage());
        break;
    case "awaiting_clarification":
        System.out.println("Question: " + event.getClarification().getQuestion());
        break;
}
```

## Testing Locally

```bash
cd java
gradle test
```
