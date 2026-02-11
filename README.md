# MUXI Java SDK

Official Java SDK for [MUXI](https://muxi.ai) — infrastructure for AI agents.

**Highlights**
- Pure Java with `OkHttp` transport
- Built-in retries, idempotency, and typed errors
- Streaming helpers for chat/audio and deploy/log tails

> Need deeper usage notes? See the [User Guide](https://github.com/muxi-ai/muxi-java/blob/main/USER_GUIDE.md) for streaming, retries, and auth details.

## Requirements

- Java 17+

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

## Quick Start

### Server Management (Control Plane)

```java
import dev.muxi.sdk.*;

public class Main {
    public static void main(String[] args) throws Exception {
        var server = new ServerClient(new ServerConfig(
            System.getenv("MUXI_SERVER_URL"),
            System.getenv("MUXI_KEY_ID"),
            System.getenv("MUXI_SECRET_KEY")
        ));

        // List formations
        var formations = server.listFormations();
        System.out.println(formations);

        // Get server status
        var status = server.status();
        System.out.println("Status: " + status);
    }
}
```

### Formation Usage (Runtime API)

```java
import dev.muxi.sdk.*;
import com.google.gson.JsonObject;

public class Main {
    public static void main(String[] args) throws Exception {
        // Connect via server proxy
        var client = new FormationClient(new FormationConfig.Builder()
            .formationId("my-bot")
            .serverUrl(System.getenv("MUXI_SERVER_URL"))
            .clientKey(System.getenv("MUXI_CLIENT_KEY"))
            .adminKey(System.getenv("MUXI_ADMIN_KEY"))
            .build());

        // Or connect directly to formation
        var client = new FormationClient(new FormationConfig.Builder()
            .url("http://localhost:8001")
            .clientKey(System.getenv("MUXI_CLIENT_KEY"))
            .adminKey(System.getenv("MUXI_ADMIN_KEY"))
            .build());

        // Chat (non-streaming)
        var payload = new JsonObject();
        payload.addProperty("message", "Hello!");
        var response = client.chat(payload, "user123");
        System.out.println(response);

        // Health check
        var health = client.health();
        System.out.println("Status: " + health);
    }
}
```

## Auth & Headers

- **Server**: HMAC with `keyId`/`secretKey` on `/rpc/*` endpoints
- **Formation**: `X-MUXI-CLIENT-KEY` or `X-MUXI-ADMIN-KEY` headers
- **Idempotency**: `X-Muxi-Idempotency-Key` auto-generated on every request
- **SDK**: `X-Muxi-SDK`, `X-Muxi-Client` headers set automatically

## Error Handling

```java
import dev.muxi.sdk.exceptions.*;

try {
    server.getFormation("nonexistent");
} catch (NotFoundException e) {
    System.out.println("Not found: " + e.getMessage());
} catch (AuthenticationException e) {
    System.out.println("Auth failed: " + e.getMessage());
} catch (RateLimitException e) {
    System.out.println("Rate limited. Retry after: " + e.getRetryAfter() + "s");
} catch (MuxiException e) {
    System.out.println("Error: " + e.getMessage() + " (" + e.getStatusCode() + ")");
}
```

## Configuration

```java
var server = new ServerClient(new ServerConfig.Builder()
    .url("https://muxi.example.com:7890")
    .keyId("your-key-id")
    .secretKey("your-secret-key")
    .timeout(30)        // Request timeout in seconds
    .maxRetries(3)      // Retry on 429/5xx errors
    .debug(true)        // Enable debug logging
    .build());
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Links

- [MUXI SDKs](https://github.com/muxi-ai/sdks)
- [MUXI Documentation](https://docs.muxi.ai)
- [GitHub](https://github.com/muxi-ai/muxi-java)
