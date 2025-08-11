package me.harshit.minechat.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class GroupInvite {
    private final String inviteId;
    private final UUID groupId;
    private final String groupName;
    private final UUID inviterUUID;
    private final String inviterName;
    private final UUID inviteeUUID;
    private final String inviteeName;
    private final long timestamp;
    private final String status; // pending | accepted | rejected

    public GroupInvite(String inviteId, UUID groupId, String groupName,
                       UUID inviterUUID, String inviterName,
                       UUID inviteeUUID, String inviteeName,
                       long timestamp, String status) {
        this.inviteId = inviteId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.inviterUUID = inviterUUID;
        this.inviterName = inviterName;
        this.inviteeUUID = inviteeUUID;
        this.inviteeName = inviteeName;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getInviteId() { return inviteId; }
    public UUID getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public UUID getInviterUUID() { return inviterUUID; }
    public String getInviterName() { return inviterName; }
    public UUID getInviteeUUID() { return inviteeUUID; }
    public String getInviteeName() { return inviteeName; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    public LocalDateTime getTimestampAsLocalDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
