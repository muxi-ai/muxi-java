# MUXI Java SDK User Guide

## Installation

### Gradle

```kotlin
implementation("dev.muxi:muxi-java:0.20260211.0")
```

### Maven

```xml
<dependency>
    <groupId>dev.muxi</groupId>
    <artifactId>muxi-java</artifactId>
    <version>0.20260211.0</version>
</dependency>
```

## Clients

- **ServerClient** (management, HMAC): deploy/list/update formations, server health/status, logs
- **FormationClient** (runtime, client/admin keys): chat/audio, agents, secrets, MCP, memory, scheduler, sessions, triggers, etc.

## Quick Start

### ServerClient

```java
import dev.muxi.sdk.*;

var server = new ServerClient(new ServerConfig(
    System.getenv("MUXI_SERVER_URL"),
    System.getenv("MUXI_KEY_ID"),
    System.getenv("MUXI_SECRET_KEY")
));

var status = server.status();
System.out.println(status);
```

### FormationClient

```java
import dev.muxi.sdk.*;
import com.google.gson.JsonObject;

var client = new FormationClient(new FormationConfig.Builder()
    .formationId("my-bot")
    .serverUrl(System.getenv("MUXI_SERVER_URL"))
    .clientKey(System.getenv("MUXI_CLIENT_KEY"))
    .adminKey(System.getenv("MUXI_ADMIN_KEY"))
    .build());

var payload = new JsonObject();
payload.addProperty("message", "Hello");
var response = client.chat(payload, "user123");
System.out.println(response);
```

## Auth & Headers

- **ServerClient**: HMAC signature (`MUXI-HMAC key=<id>, timestamp=<sec>, signature=<b64>`)
- **FormationClient**: `X-MUXI-CLIENT-KEY` required; `X-MUXI-ADMIN-KEY` for admin endpoints
- **Idempotency**: `X-Muxi-Idempotency-Key` auto-generated on every request
- **SDK Headers**: `X-Muxi-SDK: java/{version}`, `X-Muxi-Client: {os}/{arch}`

## Timeouts & Retries

- Default timeout: 30s (configurable)
- Retries on 429/5xx with exponential backoff
- Respects `Retry-After` header for rate limits

## Error Handling

```java
import dev.muxi.sdk.exceptions.*;

try {
    server.getFormation("nonexistent");
} catch (AuthenticationException e) { /* 401 */ }
  catch (AuthorizationException e) { /* 403 */ }
  catch (NotFoundException e) { /* 404 */ }
  catch (ValidationException e) { /* 422 */ }
  catch (RateLimitException e) { /* 429 - check e.getRetryAfter() */ }
  catch (ServerException e) { /* 5xx */ }
  catch (ConnectionException e) { /* network error */ }
  catch (MuxiException e) { /* base exception */ }
```

## Streaming

```java
// Chat streaming with callback
client.chatStream(payload, "user123", event -> {
    System.out.print(event.getData());
});

// Deploy with progress
server.deployFormationStream(formationId, payload, event -> {
    System.out.println(event.getEvent() + ": " + event.getData());
});
```

## Webhook Verification

```java
import dev.muxi.sdk.Webhook;

// In your webhook handler
String payload = request.getBody();
String signature = request.getHeader("X-Muxi-Signature");
String secret = System.getenv("WEBHOOK_SECRET");

if (!Webhook.verifySignature(payload, signature, secret)) {
    return ResponseEntity.status(401).body("Invalid signature");
}

var event = Webhook.parse(payload);

switch (event.getStatus()) {
    case "completed":
        for (var item : event.getContent()) {
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

## Testing

```bash
./gradlew test
```

## Contributing

- Format with your IDE's Java formatter
- Preserve idempotency header injection
