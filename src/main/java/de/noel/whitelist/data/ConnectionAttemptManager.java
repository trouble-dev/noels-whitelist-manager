package de.noel.whitelist.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionAttemptManager {
    private static final Path FILE_PATH = Paths.get("whitelist_pending.json");
    private static final int MAX_ENTRIES = 50; // Keep last 50 attempts
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Store by UUID to avoid duplicates - keeps most recent attempt per player
    private final Map<UUID, ConnectionAttempt> pendingAttempts = new ConcurrentHashMap<>();

    public ConnectionAttemptManager() {
        load();
    }

    public void addAttempt(ConnectionAttempt attempt) {
        // Update or add the attempt (overwrites old attempt from same player)
        pendingAttempts.put(attempt.getUuid(), attempt);

        // Trim if too many entries
        if (pendingAttempts.size() > MAX_ENTRIES) {
            trimOldest();
        }

        save();
    }

    public void removeAttempt(UUID uuid) {
        pendingAttempts.remove(uuid);
        save();
    }

    public Collection<ConnectionAttempt> getPendingAttempts() {
        // Return sorted by timestamp (newest first)
        List<ConnectionAttempt> sorted = new ArrayList<>(pendingAttempts.values());
        sorted.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return sorted;
    }

    public ConnectionAttempt getAttempt(UUID uuid) {
        return pendingAttempts.get(uuid);
    }

    public int getCount() {
        return pendingAttempts.size();
    }

    public void clear() {
        pendingAttempts.clear();
        save();
    }

    private void trimOldest() {
        if (pendingAttempts.size() <= MAX_ENTRIES) return;

        // Find and remove oldest entries
        List<Map.Entry<UUID, ConnectionAttempt>> sorted = new ArrayList<>(pendingAttempts.entrySet());
        sorted.sort((a, b) -> Long.compare(a.getValue().getTimestamp(), b.getValue().getTimestamp()));

        int toRemove = pendingAttempts.size() - MAX_ENTRIES;
        for (int i = 0; i < toRemove && i < sorted.size(); i++) {
            pendingAttempts.remove(sorted.get(i).getKey());
        }
    }

    private void load() {
        File file = FILE_PATH.toFile();
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(FILE_PATH)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                String username = obj.get("username").getAsString();
                long timestamp = obj.get("timestamp").getAsLong();
                String ipAddress = obj.has("ip") ? obj.get("ip").getAsString() : "unknown";

                pendingAttempts.put(uuid, new ConnectionAttempt(uuid, username, timestamp, ipAddress));
            }
        } catch (Exception e) {
            System.err.println("Failed to load whitelist_pending.json: " + e.getMessage());
        }
    }

    private void save() {
        JsonArray array = new JsonArray();

        for (ConnectionAttempt attempt : pendingAttempts.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", attempt.getUuid().toString());
            obj.addProperty("username", attempt.getUsername());
            obj.addProperty("timestamp", attempt.getTimestamp());
            obj.addProperty("ip", attempt.getIpAddress());
            array.add(obj);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(FILE_PATH)) {
            writer.write(GSON.toJson(array));
        } catch (Exception e) {
            System.err.println("Failed to save whitelist_pending.json: " + e.getMessage());
        }
    }
}
