package me.harshit.minechat.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupMessage {

    private final UUID messageId;
    private final UUID groupId;
    private final UUID senderUUID;
    private final String senderName;
    private final String content;
    private final LocalDateTime timestamp;
    private final MessageType messageType;
    private final LocalDateTime editedAt;
    private final Map<String, List<UUID>> reactions; // emoji -> list of player UUIDs

    public enum MessageType {
        TEXT("text"),
        SYSTEM("system"),      // system here means user events like join, leave etc not to be
        // confused with server messages
        ANNOUNCEMENT("announcement");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MessageType fromString(String value) {
            for (MessageType type : MessageType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return TEXT;
        }
    }

    public GroupMessage(UUID messageId, UUID groupId, UUID senderUUID, String senderName,
                       String content, LocalDateTime timestamp, MessageType messageType,
                       LocalDateTime editedAt, Map<String, List<UUID>> reactions) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.editedAt = editedAt;
        this.reactions = reactions != null ? new HashMap<>(reactions) : new HashMap<>();
    }

    public GroupMessage(UUID messageId, UUID groupId, UUID senderUUID, String senderName,
                       String content, LocalDateTime timestamp) {
        this(messageId, groupId, senderUUID, senderName, content, timestamp,
             MessageType.TEXT, null, new HashMap<>());
    }

    public static GroupMessage createSystemMessage(UUID groupId, String content) {
        return new GroupMessage(
            UUID.randomUUID(),
            groupId,
            null,
            "System",
            content,
            LocalDateTime.now(),
            MessageType.SYSTEM,
            null,
            new HashMap<>()
        );
    }

    public static GroupMessage createAnnouncement(UUID groupId, UUID senderUUID,
                                                 String senderName, String content) {
        return new GroupMessage(
            UUID.randomUUID(),
            groupId,
            senderUUID,
            senderName,
            content,
            LocalDateTime.now(),
            MessageType.ANNOUNCEMENT,
            null,
            new HashMap<>()
        );
    }

    public UUID getMessageId() { return messageId; }
    public UUID getGroupId() { return groupId; }
    public UUID getSenderUUID() { return senderUUID; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public MessageType getMessageType() { return messageType; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public Map<String, List<UUID>> getReactions() { return new HashMap<>(reactions); }

    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }

    public boolean isAnnouncement() {
        return messageType == MessageType.ANNOUNCEMENT;
    }

    public boolean isEdited() {
        return editedAt != null;
    }

    public boolean hasReactions() {
        return !reactions.isEmpty();
    }

    public int getTotalReactions() {
        return reactions.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public boolean hasUserReacted(UUID userUUID, String emoji) {
        List<UUID> reactors = reactions.get(emoji);
        return reactors != null && reactors.contains(userUUID);
    }

    public boolean hasUserReacted(UUID userUUID) {
        return reactions.values().stream()
                .anyMatch(reactors -> reactors.contains(userUUID));
    }

    public static GroupMessage fromDocument(org.bson.Document doc) {
        if (doc == null) return null;

        try {
            UUID messageId = UUID.fromString(doc.getString("messageId"));
            UUID groupId = UUID.fromString(doc.getString("groupId"));
            UUID senderUUID = doc.getString("senderUUID") != null ? UUID.fromString(doc.getString("senderUUID")) : null;
            String senderName = doc.getString("senderName");
            String content = doc.getString("content");

            // convert timestamp
            long timestampMillis = doc.getLong("timestamp");
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                java.time.ZoneId.systemDefault()
            );

            // convert message type
            String messageTypeStr = doc.getString("messageType");
            MessageType messageType = MessageType.fromString(messageTypeStr);

            LocalDateTime editedAt = null;
            if (doc.containsKey("editedAt") && doc.get("editedAt") != null) {
                long editedAtMillis = doc.getLong("editedAt");
                editedAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(editedAtMillis),
                    java.time.ZoneId.systemDefault()
                );
            }

            Map<String, List<UUID>> reactions = new HashMap<>();
            if (doc.containsKey("reactions") && doc.get("reactions") instanceof org.bson.Document) {
                org.bson.Document reactionsDoc = doc.get("reactions", org.bson.Document.class);
                for (String emoji : reactionsDoc.keySet()) {
                    List<String> userIds = reactionsDoc.getList(emoji, String.class);
                    if (userIds != null) {
                        List<UUID> userUUIDs = new ArrayList<>();
                        for (String userId : userIds) {
                            try {
                                userUUIDs.add(UUID.fromString(userId));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        reactions.put(emoji, userUUIDs);
                    }
                }
            }

            return new GroupMessage(messageId, groupId, senderUUID, senderName, content,
                                  timestamp, messageType, editedAt, reactions);
        } catch (Exception e) {
            return null;
        }
    }

    public GroupMessage createEditedVersion(String newContent) {
        return new GroupMessage(
            this.messageId,
            this.groupId,
            this.senderUUID,
            this.senderName,
            newContent,
            this.timestamp,
            this.messageType,
            LocalDateTime.now(), // Set edited timestamp
            this.reactions
        );
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s): %s",
            messageType.getValue().toUpperCase(),
            senderName,
            timestamp.toString(),
            content);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroupMessage that = (GroupMessage) obj;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId.hashCode();
    }
}
