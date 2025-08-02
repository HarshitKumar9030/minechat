package me.harshit.minechat.api;

import java.time.LocalDateTime;
import java.util.UUID;

// represents a member of a chat group


public class GroupMember {

    private final UUID playerId;
    private final String playerName;
    private final GroupRole role;
    private final LocalDateTime joinedDate;
    private final boolean isOnline;

    public enum GroupRole {
        OWNER("Owner", 100),
        ADMIN("Admin", 75),
        MODERATOR("Moderator", 50),
        MEMBER("Member", 25);

        private final String displayName;
        private final int priority;

        GroupRole(String displayName, int priority) {
            this.displayName = displayName;
            this.priority = priority;
        }

        public String getDisplayName() { return displayName; }
        public int getPriority() { return priority; }

        public boolean canManageGroup() { return priority >= 75; }
        public boolean canModerate() { return priority >= 50; }
        public boolean canInvite() { return priority >= 25; }
    }

    public GroupMember(UUID playerId, String playerName, GroupRole role, LocalDateTime joinedDate, boolean isOnline) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
        this.joinedDate = joinedDate;
        this.isOnline = isOnline;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public GroupRole getRole() { return role; }
    public LocalDateTime getJoinedDate() { return joinedDate; }
    public boolean isOnline() { return isOnline; }


    public boolean hasPermission(GroupRole requiredRole) {
        return role.getPriority() >= requiredRole.getPriority();
    }

    public static GroupMember fromDocument(org.bson.Document memberDoc) {
        if (memberDoc == null) return null;

        try {
            UUID playerId = UUID.fromString(memberDoc.getString("playerId"));
            String playerName = memberDoc.getString("playerName");
            String roleString = memberDoc.getString("role");
            GroupRole role = GroupRole.valueOf(roleString.toUpperCase());
            long joinedTimestamp = memberDoc.getLong("joinedDate");
            boolean isOnline = memberDoc.getBoolean("isOnline", false);

            LocalDateTime joinedDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(joinedTimestamp),
                java.time.ZoneId.systemDefault()
            );

            return new GroupMember(playerId, playerName, role, joinedDate, isOnline);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("GroupMember{name='%s', role=%s, online=%s}",
            playerName, role.getDisplayName(), isOnline);
    }
}
