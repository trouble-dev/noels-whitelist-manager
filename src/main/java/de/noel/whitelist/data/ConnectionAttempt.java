package de.noel.whitelist.data;

import java.util.UUID;

public class ConnectionAttempt {
    private final UUID uuid;
    private final String username;
    private final long timestamp;
    private final String ipAddress;

    public ConnectionAttempt(UUID uuid, String username, String ipAddress) {
        this.uuid = uuid;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
        this.ipAddress = ipAddress;
    }

    public ConnectionAttempt(UUID uuid, String username, long timestamp, String ipAddress) {
        this.uuid = uuid;
        this.username = username;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getFormattedTime() {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
