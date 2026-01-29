package dev.muxi.sdk;

import com.google.gson.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Webhook {
    private static final int DEFAULT_TOLERANCE = 300;
    private static final Gson gson = new Gson();
    
    private Webhook() {}
    
    public static boolean verifySignature(String payload, String header, String secret) {
        return verifySignature(payload, header, secret, DEFAULT_TOLERANCE);
    }
    
    public static boolean verifySignature(String payload, String header, String secret, int tolerance) {
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("Webhook secret is required");
        if (header == null || header.isEmpty()) return false;
        
        Map<String, String> parts = new HashMap<>();
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) parts.put(kv[0].trim(), kv[1].trim());
        }
        
        String timestamp = parts.get("t");
        String signature = parts.get("v1");
        if (timestamp == null || signature == null) return false;
        
        long ts;
        try { ts = Long.parseLong(timestamp); } catch (NumberFormatException e) { return false; }
        if (Math.abs(System.currentTimeMillis() / 1000 - ts) > tolerance) return false;
        
        try {
            String message = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder expected = new StringBuilder();
            for (byte b : hash) expected.append(String.format("%02x", b));
            return expected.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    public static WebhookEvent parse(String payload) {
        return gson.fromJson(payload, WebhookEvent.class);
    }
    
    public static class WebhookEvent {
        public String requestId;
        public String sessionId;
        public String userId;
        public String status;
        public List<ContentItem> content;
        public ErrorInfo error;
        public ClarificationInfo clarification;
        public String timestamp;
        
        public static class ContentItem {
            public String type;
            public String text;
            public String url;
            public Map<String, Object> metadata;
        }
        
        public static class ErrorInfo {
            public String code;
            public String message;
        }
        
        public static class ClarificationInfo {
            public String question;
            public List<String> options;
        }
    }
}
