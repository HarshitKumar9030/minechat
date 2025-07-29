package me.harshit.minechat.api;

import java.time.LocalDateTime;
import java.util.UUID;

// Represents a friend request in the friend system


public class FriendRequest {

    private final UUID senderUUID;
    private final String senderName;
    private final UUID targetUUID;
    private final String targetName;
    private final LocalDateTime requestTime;
    private final RequestStatus status;

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        DENIED,
        EXPIRED
    }

    public FriendRequest(UUID senderUUID, String senderName, UUID targetUUID, String targetName,
                        LocalDateTime requestTime, RequestStatus status) {
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.requestTime = requestTime;
        this.status = status;
    }

    public UUID getSenderUUID() { return senderUUID; }
    public String getSenderName() { return senderName; }
    public UUID getTargetUUID() { return targetUUID; }
    public String getTargetName() { return targetName; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public RequestStatus getStatus() { return status; }

    public boolean isPending() { return status == RequestStatus.PENDING; }
    public boolean isExpired() { return status == RequestStatus.EXPIRED; }

    @Override
    public String toString() {
        return String.format("FriendRequest{from='%s', to='%s', status=%s, time=%s}",
            senderName, targetName, status, requestTime);
    }
}
