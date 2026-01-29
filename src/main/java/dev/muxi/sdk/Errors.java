package dev.muxi.sdk;

public class Errors {
    public static class MuxiException extends RuntimeException {
        private final String errorCode;
        private final int statusCode;
        private final Integer retryAfter;
        
        public MuxiException(String errorCode, String message, int statusCode, Integer retryAfter) {
            super(errorCode != null && !errorCode.isEmpty() ? errorCode + ": " + message : message);
            this.errorCode = errorCode;
            this.statusCode = statusCode;
            this.retryAfter = retryAfter;
        }
        
        public String getErrorCode() { return errorCode; }
        public int getStatusCode() { return statusCode; }
        public Integer getRetryAfter() { return retryAfter; }
    }
    
    public static class AuthenticationException extends MuxiException {
        public AuthenticationException(String code, String message, int status) {
            super(code != null ? code : "UNAUTHORIZED", message, status, null);
        }
    }
    
    public static class AuthorizationException extends MuxiException {
        public AuthorizationException(String code, String message, int status) {
            super(code != null ? code : "FORBIDDEN", message, status, null);
        }
    }
    
    public static class NotFoundException extends MuxiException {
        public NotFoundException(String code, String message, int status) {
            super(code != null ? code : "NOT_FOUND", message, status, null);
        }
    }
    
    public static class ConflictException extends MuxiException {
        public ConflictException(String code, String message, int status) {
            super(code != null ? code : "CONFLICT", message, status, null);
        }
    }
    
    public static class ValidationException extends MuxiException {
        public ValidationException(String code, String message, int status) {
            super(code != null ? code : "VALIDATION_ERROR", message, status, null);
        }
    }
    
    public static class RateLimitException extends MuxiException {
        public RateLimitException(String message, int status, Integer retryAfter) {
            super("RATE_LIMITED", message != null ? message : "Too Many Requests", status, retryAfter);
        }
    }
    
    public static class ServerException extends MuxiException {
        public ServerException(String code, String message, int status) {
            super(code != null ? code : "SERVER_ERROR", message, status, null);
        }
    }
    
    public static class ConnectionException extends MuxiException {
        public ConnectionException(String message) {
            super("CONNECTION_ERROR", message, 0, null);
        }
    }
    
    public static MuxiException map(int status, String code, String message, Integer retryAfter) {
        return switch (status) {
            case 401 -> new AuthenticationException(code, message, status);
            case 403 -> new AuthorizationException(code, message, status);
            case 404 -> new NotFoundException(code, message, status);
            case 409 -> new ConflictException(code, message, status);
            case 422 -> new ValidationException(code, message, status);
            case 429 -> new RateLimitException(message, status, retryAfter);
            case 500, 501, 502, 503, 504 -> new ServerException(code, message, status);
            default -> new MuxiException(code, message, status, null);
        };
    }
}
