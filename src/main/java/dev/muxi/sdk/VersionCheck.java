package dev.muxi.sdk;

import com.google.gson.*;
import com.google.gson.reflect.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VersionCheck {
    private static final String SDK_NAME = "java";
    private static final long TWELVE_HOURS_MILLIS = 12 * 60 * 60 * 1000;
    private static final AtomicBoolean checked = new AtomicBoolean(false);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static void checkForUpdates(Map<String, String> headers) {
        if (!checked.compareAndSet(false, true)) return;
        if (notificationsDisabled()) return;
        
        String latest = headers.getOrDefault("X-Muxi-SDK-Latest", 
                        headers.get("x-muxi-sdk-latest"));
        if (latest == null) return;
        
        if (!isNewerVersion(latest, MuxiVersion.VERSION)) return;
        
        updateLatestVersion(latest);
        
        if (!notifiedRecently()) {
            System.err.println("[muxi] SDK update available: " + latest + " (current: " + MuxiVersion.VERSION + ")");
            System.err.println("[muxi] Update via Maven/Gradle: implementation(\"dev.muxi:muxi-java:" + latest + "\")");
            markNotified();
        }
    }
    
    private static boolean notificationsDisabled() {
        return "0".equals(System.getenv("MUXI_SDK_VERSION_NOTIFICATION"));
    }
    
    private static Path getCachePath() {
        String home = System.getProperty("user.home");
        if (home == null) return null;
        return Paths.get(home, ".muxi", "sdk-versions.json");
    }
    
    private static Map<String, VersionEntry> loadCache() {
        try {
            Path path = getCachePath();
            if (path == null || !Files.exists(path)) return new HashMap<>();
            String content = Files.readString(path);
            java.lang.reflect.Type type = new TypeToken<Map<String, VersionEntry>>(){}.getType();
            return gson.fromJson(content, type);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private static void saveCache(Map<String, VersionEntry> cache) {
        try {
            Path path = getCachePath();
            if (path == null) return;
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(cache));
        } catch (Exception e) {
            // Ignore cache errors
        }
    }
    
    private static boolean isNewerVersion(String latest, String current) {
        return latest.compareTo(current) > 0;
    }
    
    private static boolean notifiedRecently() {
        try {
            Map<String, VersionEntry> cache = loadCache();
            VersionEntry entry = cache.get(SDK_NAME);
            if (entry == null || entry.lastNotified == null) return false;
            Instant lastTime = Instant.parse(entry.lastNotified);
            return Duration.between(lastTime, Instant.now()).toMillis() < TWELVE_HOURS_MILLIS;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void updateLatestVersion(String latest) {
        Map<String, VersionEntry> cache = loadCache();
        VersionEntry entry = cache.getOrDefault(SDK_NAME, new VersionEntry());
        entry.current = MuxiVersion.VERSION;
        entry.latest = latest;
        cache.put(SDK_NAME, entry);
        saveCache(cache);
    }
    
    private static void markNotified() {
        Map<String, VersionEntry> cache = loadCache();
        VersionEntry entry = cache.get(SDK_NAME);
        if (entry != null) {
            entry.lastNotified = Instant.now().toString();
            saveCache(cache);
        }
    }
    
    private static class VersionEntry {
        String current;
        String latest;
        String lastNotified;
    }
}
