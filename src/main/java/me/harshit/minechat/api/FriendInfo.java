package me.harshit.minechat.api;

import java.time.LocalDateTime;
import java.util.UUID;

// Represents a friend's information in the friend system


public class FriendInfo {

    private final UUID playerUUID;
    private final String playerName;
    private final UUID friendUUID;
    private final String friendName;
    private final LocalDateTime friendsSince;
    private final boolean isOnline;

    public FriendInfo(UUID playerUUID, String playerName, UUID friendUUID, String friendName,
                     LocalDateTime friendsSince, boolean isOnline) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.friendUUID = friendUUID;
        this.friendName = friendName;
        this.friendsSince = friendsSince;
        this.isOnline = isOnline;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public UUID getFriendUUID() { return friendUUID; }
    public String getFriendName() { return friendName; }
    public LocalDateTime getFriendsSince() { return friendsSince; }
    public boolean isOnline() { return isOnline; }

    @Override
    public String toString() {
        return String.format("FriendInfo{friendName='%s', online=%s, since=%s}",
            friendName, isOnline, friendsSince);
    }
}
