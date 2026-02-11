package org.muxi.sdk;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Auth {
    private Auth() {}
    
    public static String[] generateHmacSignature(String secretKey, String method, String path) {
        String cleanPath = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String message = timestamp + ";" + method + ";" + cleanPath;
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            return new String[]{signature, timestamp};
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
    
    public static String buildAuthHeader(String keyId, String secretKey, String method, String path) {
        String[] result = generateHmacSignature(secretKey, method, path);
        return "MUXI-HMAC key=" + keyId + ", timestamp=" + result[1] + ", signature=" + result[0];
    }
}
