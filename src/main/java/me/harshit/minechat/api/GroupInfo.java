package me.harshit.minechat.api;

import org.bson.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// represents a chat group with its details
// This class encapsulates all the necessary information about a group, including its members, settings, and ownership

public class GroupInfo {

    private final UUID groupId;
    private final String groupName;
    private final String description;
    private final UUID ownerId;
    private final String ownerName;
    private final List<GroupMember> members;
    private final LocalDateTime createdDate;
    private final boolean isPrivate;
    private final int maxMembers;
    private final GroupSettings settings;

    public GroupInfo(UUID groupId, String groupName, String description, UUID ownerId, String ownerName,
                    List<GroupMember> members, LocalDateTime createdDate, boolean isPrivate,
                    int maxMembers, GroupSettings settings) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.members = members;
        this.createdDate = createdDate;
        this.isPrivate = isPrivate;
        this.maxMembers = maxMembers;
        this.settings = settings;
    }

    public static GroupInfo fromDocument(Document groupDoc) {
        if (groupDoc == null) return null;

        try {
            UUID groupId = UUID.fromString(groupDoc.getString("groupId"));
            String groupName = groupDoc.getString("groupName");
            String description = groupDoc.getString("description");
            UUID ownerId = UUID.fromString(groupDoc.getString("ownerId"));
            String ownerName = groupDoc.getString("ownerName");
            boolean isPrivate = groupDoc.getBoolean("isPrivate", false);
            int maxMembers = groupDoc.getInteger("maxMembers", 25);
            long createdDate = groupDoc.getLong("createdDate");

            List<Document> memberDocs = groupDoc.getList("members", Document.class);
            List<GroupMember> members = memberDocs.stream()
                .map(GroupMember::fromDocument)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

            Document settingsDoc = groupDoc.get("settings", Document.class);
            GroupSettings settings = GroupSettings.fromDocument(settingsDoc);

            LocalDateTime createdDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(createdDate),
                java.time.ZoneId.systemDefault()
            );

            return new GroupInfo(groupId, groupName, description, ownerId, ownerName,
                               members, createdDateTime, isPrivate, maxMembers, settings);
        } catch (Exception e) {
            return null;
        }
    }

    public UUID getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
    public UUID getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public List<GroupMember> getMembers() { return members; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isPrivate() { return isPrivate; }
    public int getMaxMembers() { return maxMembers; }
    public GroupSettings getSettings() { return settings; }


    public int getMemberCount() { return members.size(); }
    public boolean isFull() { return members.size() >= maxMembers; }
    public boolean isOwner(UUID playerId) { return ownerId.equals(playerId); }

    public boolean hasMember(UUID playerId) {
        return members.stream().anyMatch(member -> member.getPlayerId().equals(playerId));
    }

    public GroupMember getMember(UUID playerId) {
        return members.stream()
                .filter(member -> member.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return String.format("GroupInfo{name='%s', members=%d/%d, owner='%s'}",
            groupName, getMemberCount(), maxMembers, ownerName);
    }
}
