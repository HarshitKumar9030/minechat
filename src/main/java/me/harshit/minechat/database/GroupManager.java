package me.harshit.minechat.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.api.GroupMember;
import me.harshit.minechat.api.GroupSettings;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// db ops for groups

public class GroupManager {

    private final MongoCollection<Document> groupsCollection;
    private final MongoCollection<Document> groupInvitesCollection;
    private final MongoCollection<Document> groupMessagesCollection;
    private final JavaPlugin plugin;

    public GroupManager(MongoDatabase database, JavaPlugin plugin) {
        this.groupsCollection = database.getCollection("chat_groups");
        this.groupInvitesCollection = database.getCollection("group_invites");
        this.groupMessagesCollection = database.getCollection("group_messages");
        this.plugin = plugin;
    }


    public boolean createGroup(UUID ownerId, String ownerName, String groupName, String description, boolean isPrivate) {
        try {
            if (groupExists(groupName)) {
                return false;
            }

            UUID groupId = UUID.randomUUID();
            long timestamp = System.currentTimeMillis();

            Document groupDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("groupName", groupName)
                    .append("description", description)
                    .append("ownerId", ownerId.toString())
                    .append("ownerName", ownerName)
                    .append("isPrivate", isPrivate)
                    .append("createdDate", timestamp)
                    .append("maxMembers", plugin.getConfig().getInt("chat-groups.max-members-per-group", 25))
                    .append("settings", getDefaultSettingsDocument())
                    .append("members", List.of(createOwnerMemberDocument(ownerId, ownerName, timestamp)));

            groupsCollection.insertOne(groupDoc);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create group: " + e.getMessage());
            return false;
        }
    }


    public boolean sendGroupInvite(UUID groupId, UUID inviterId, String inviterName, UUID targetId, String targetName) {
        try {
            Document existingInvite = groupInvitesCollection.find(
                new Document("groupId", groupId.toString())
                    .append("targetId", targetId.toString())
                    .append("status", "pending")
            ).first();

            if (existingInvite != null) {
                return false; // Invite already exists
            }

            Document inviteDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("inviterId", inviterId.toString())
                    .append("inviterName", inviterName)
                    .append("targetId", targetId.toString())
                    .append("targetName", targetName)
                    .append("timestamp", System.currentTimeMillis())
                    .append("status", "pending");

            groupInvitesCollection.insertOne(inviteDoc);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send group invite: " + e.getMessage());
            return false;
        }
    }


    public boolean acceptGroupInvite(UUID targetId, UUID groupId) {
        try {
            Document invite = groupInvitesCollection.find(
                new Document("groupId", groupId.toString())
                    .append("targetId", targetId.toString())
                    .append("status", "pending")
            ).first();

            if (invite == null) {
                return false;
            }

            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                return false;
            }

            List<Document> members = group.getList("members", Document.class);
            int maxMembers = group.getInteger("maxMembers", 25);

            if (members.size() >= maxMembers) {
                return false; // Group is full
            }

            Document newMember = createMemberDocument(targetId, invite.getString("targetName"), System.currentTimeMillis());
            groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$push", new Document("members", newMember))
            );

            groupInvitesCollection.deleteOne(
                new Document("groupId", groupId.toString())
                    .append("targetId", targetId.toString())
            );

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to accept group invite: " + e.getMessage());
            return false;
        }
    }


    public boolean leaveGroup(UUID playerId, UUID groupId) {
        try {
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                return false;
            }

            // Can't leave if you're the owner
            if (group.getString("ownerId").equals(playerId.toString())) {
                return false;
            }

            // Remove member from group
            groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$pull", new Document("members", new Document("playerId", playerId.toString())))
            );

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to leave group: " + e.getMessage());
            return false;
        }
    }


    public void storeGroupMessage(UUID groupId, UUID senderId, String senderName, String message, String source) {
        try {
            Document messageDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("senderId", senderId.toString())
                    .append("senderName", senderName)
                    .append("message", message)
                    .append("source", source) // "minecraft" or "web"
                    .append("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append("date", System.currentTimeMillis());

            groupMessagesCollection.insertOne(messageDoc);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to store group message: " + e.getMessage());
        }
    }


    public List<Document> getGroupMessages(UUID groupId, int limit) {
        try {
            List<Document> messages = new ArrayList<>();
            groupMessagesCollection.find(new Document("groupId", groupId.toString()))
                    .sort(new Document("date", -1))
                    .limit(limit)
                    .into(messages);
            return messages;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public List<Document> getPlayerGroups(UUID playerId) {
        try {
            List<Document> groups = new ArrayList<>();
            groupsCollection.find(new Document("members.playerId", playerId.toString()))
                    .into(groups);
            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public List<Document> getPendingGroupInvites(UUID playerId) {
        try {
            List<Document> invites = new ArrayList<>();
            groupInvitesCollection.find(
                new Document("targetId", playerId.toString())
                    .append("status", "pending")
            ).into(invites);
            return invites;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get pending group invites: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public boolean groupExists(String groupName) {
        try {
            return groupsCollection.find(new Document("groupName", groupName)).first() != null;
        } catch (Exception e) {
            return false;
        }
    }


    public Document getGroup(UUID groupId) {
        try {
            return groupsCollection.find(new Document("groupId", groupId.toString())).first();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group: " + e.getMessage());
            return null;
        }
    }


    private Document getDefaultSettingsDocument() {
        return new Document()
                .append("allowInvites", true)
                .append("friendsOnly", false)
                .append("muteNonMembers", false)
                .append("logMessages", true)
                .append("webAccessEnabled", true)
                .append("joinMessage", "Welcome to the group!")
                .append("leaveMessage", "Thanks for being part of the group!");
    }

    private Document createOwnerMemberDocument(UUID playerId, String playerName, long timestamp) {
        return new Document()
                .append("playerId", playerId.toString())
                .append("playerName", playerName)
                .append("role", "OWNER")
                .append("joinedDate", timestamp);
    }

    private Document createMemberDocument(UUID playerId, String playerName, long timestamp) {
        return new Document()
                .append("playerId", playerId.toString())
                .append("playerName", playerName)
                .append("role", "MEMBER")
                .append("joinedDate", timestamp);
    }

    // join group  directly, used by web interface

    public boolean joinGroup(UUID groupId, UUID playerUUID, String playerName) {
        try {
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                return false; // Group doesn't exist
            }

            // Check if player is already a member
            List<Document> members = group.getList("members", Document.class);
            boolean alreadyMember = members.stream()
                    .anyMatch(member -> member.getString("playerUUID").equals(playerUUID.toString()));

            if (alreadyMember) {
                return false; // Already a member
            }

            // Check member limit
            int maxMembers = group.getInteger("maxMembers", 25);
            if (members.size() >= maxMembers) {
                return false; // Group is full
            }

            // Add player to group
            Document newMember = new Document()
                    .append("playerUUID", playerUUID.toString())
                    .append("playerName", playerName)
                    .append("role", "member")
                    .append("joinedDate", System.currentTimeMillis());

            groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$push", new Document("members", newMember))
            );

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to join group: " + e.getMessage());
            return false;
        }
    }
}
