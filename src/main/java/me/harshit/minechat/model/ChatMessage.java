package me.harshit.minechat.model;

import java.time.LocalDateTime;
import java.util.UUID;

// Represents a chat message in the plugin
public class ChatMessage {

    private final String playerName;
    private final UUID playerUUID;
    private final String message;
    private final String serverName;
    private final LocalDateTime timestamp;

    public ChatMessage(String playerName, UUID playerUUID, String message, String serverName) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.message = message;
        this.serverName = serverName;
        this.timestamp = LocalDateTime.now();
    }

    // Getters for all fields
    public String getPlayerName() { return playerName; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getMessage() { return message; }
    public String getServerName() { return serverName; }
    public LocalDateTime getTimestamp() { return timestamp; }

 // Converts the chat message to a formatted string
    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
            timestamp.toString(), playerName, message);
    }
}
