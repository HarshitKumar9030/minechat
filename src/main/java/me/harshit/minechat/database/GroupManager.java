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

    public boolean isAdminOrOwner(UUID groupId, UUID playerUUID) {
        try {
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                return false;
            }

            if (group.getString("ownerId").equals(playerUUID.toString())) {
                return true;
            }

            List<Document> members = group.getList("members", Document.class);
            return members.stream()
                    .anyMatch(member ->
                            member.getString("playerUUID").equals(playerUUID.toString()) &&
                                    ("ADMIN".equals(member.getString("role")) || "OWNER".equals(member.getString("role")))
                    );

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check admin/owner status: " + e.getMessage());
            return false;
        }
    }

    public boolean kickMember(UUID groupId, UUID targetId, UUID adminId, String reason) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            Document group = getGroup(groupId);
            if (group == null || group.getString("ownerId").equals(targetId.toString())) {
                return false;
            }

            long removedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("members", new Document("playerId", targetId.toString())))
            ).getModifiedCount();

            if (removedCount > 0) {
                plugin.getLogger().info("Player " + targetId + " was kicked from group " + groupId + " by " + adminId + ". Reason: " + reason);
                return true;
            }

            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to kick member: " + e.getMessage());
            return false;
        }
    }


    public void broadcastToGroup(UUID groupId, me.harshit.minechat.api.GroupMessage message) {
        try {
            Document group = getGroup(groupId);
            if (group == null) return;

            List<Document> members = group.getList("members", Document.class);
            for (Document member : members) {
                String memberName = member.getString("playerName");
                org.bukkit.entity.Player onlineMember = org.bukkit.Bukkit.getPlayerExact(memberName);
                if (onlineMember != null && onlineMember.isOnline()) {
                    onlineMember.sendMessage(net.kyori.adventure.text.Component.text(message.getContent()));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to broadcast to group: " + e.getMessage());
        }
    }

    public UUID getPlayerUUID(String playerName) {
        try {
            // try to get from online players first
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayerExact(playerName);
            if (player != null) {
                return player.getUniqueId();
            }

            // try to get from offline players
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer.getUniqueId();
            }

            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player UUID: " + e.getMessage());
            return null;
        }
    }

    public boolean updateGroupAnnouncement(UUID groupId, String announcement) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$set", new Document("announcement", announcement))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group announcement: " + e.getMessage());
            return false;
        }
    }

    public boolean removePlayerFromGroup(UUID groupId, UUID playerId) {
        try {
            long removedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("members", new Document("playerId", playerId.toString())))
            ).getModifiedCount();
            return removedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove player from group: " + e.getMessage());
            return false;
        }
    }

    public boolean promoteGroupMember(UUID groupId, UUID playerId) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", playerId.toString()),
                    new Document("$set", new Document("members.$.role", "ADMIN"))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to promote group member: " + e.getMessage());
            return false;
        }
    }

    public boolean banPlayerFromGroup(UUID groupId, UUID playerId) {
        try {
            removePlayerFromGroup(groupId, playerId);

            Document banDoc = new Document()
                    .append("playerId", playerId.toString())
                    .append("bannedAt", System.currentTimeMillis());

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("bannedMembers", banDoc))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban player from group: " + e.getMessage());
            return false;
        }
    }

    public boolean muteGroupMember(UUID groupId, UUID playerId, long duration) {
        try {
            long muteUntil = System.currentTimeMillis() + duration;
            Document muteDoc = new Document()
                    .append("playerId", playerId.toString())
                    .append("muteUntil", muteUntil);

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", playerId.toString()),
                    new Document("$set", new Document("members.$.muteUntil", muteUntil))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to mute group member: " + e.getMessage());
            return false;
        }
    }

    public List<Document> searchPublicGroups(String query, int limit) {
        try {
            List<Document> groups = new ArrayList<>();
            Document filter = new Document("isPrivate", false);
            if (query != null && !query.trim().isEmpty()) {
                filter.append("groupName", new Document("$regex", query).append("$options", "i"));
            }

            groupsCollection.find(filter)
                    .limit(limit)
                    .into(groups);
            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to search public groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean joinGroupByInviteCode(UUID playerId, String playerName, String inviteCode) {
        try {
            // todo: for now just use simple system for this, later we store this with expiration
            Document group = groupsCollection.find(new Document("inviteCode", inviteCode)).first();
            if (group == null) {
                return false;
            }

            UUID groupId = UUID.fromString(group.getString("groupId"));
            return joinGroup(groupId, playerId, playerName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to join group by invite code: " + e.getMessage());
            return false;
        }
    }

    public Document getGroupById(UUID groupId) {
        return getGroup(groupId);
    }

    public boolean canInviteToGroup(UUID groupId, UUID playerId) {
        return isGroupAdmin(groupId, playerId);
    }

    public String generateGroupInviteCode(UUID groupId) {
        try {
            String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$set", new Document("inviteCode", inviteCode))
            ).getModifiedCount();
            return modifiedCount > 0 ? inviteCode : null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate invite code: " + e.getMessage());
            return null;
        }
    }

    public UUID createGroup(UUID ownerId, String ownerName, String groupName, String description, int maxMembers, boolean isPublic) {
        try {
            if (groupExists(groupName)) {
                return null;
            }

            UUID groupId = UUID.randomUUID();
            long timestamp = System.currentTimeMillis();

            Document groupDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("groupName", groupName)
                    .append("description", description)
                    .append("ownerId", ownerId.toString())
                    .append("ownerName", ownerName)
                    .append("isPrivate", !isPublic)
                    .append("createdDate", timestamp)
                    .append("maxMembers", maxMembers)
                    .append("settings", getDefaultSettingsDocument())
                    .append("members", List.of(createOwnerMemberDocument(ownerId, ownerName, timestamp)));

            groupsCollection.insertOne(groupDoc);
            return groupId;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create group: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo createGroup(String groupName, String description, UUID ownerId, String ownerName, int maxMembers) {
        try {
            if (groupExists(groupName)) {
                return null;
            }

            UUID groupId = UUID.randomUUID();
            long timestamp = System.currentTimeMillis();

            Document groupDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("groupName", groupName)
                    .append("description", description)
                    .append("ownerId", ownerId.toString())
                    .append("ownerName", ownerName)
                    .append("isPrivate", false)
                    .append("createdDate", timestamp)
                    .append("maxMembers", maxMembers)
                    .append("settings", getDefaultSettingsDocument())
                    .append("members", List.of(createOwnerMemberDocument(ownerId, ownerName, timestamp)));

            groupsCollection.insertOne(groupDoc);

            // Convert to GroupInfo and return
            return convertDocumentToGroupInfo(groupDoc);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create group: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo getGroupByName(String groupName) {
        try {
            Document doc = groupsCollection.find(new Document("groupName", groupName)).first();
            return convertDocumentToGroupInfo(doc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group by name: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo getGroupByInviteCode(String inviteCode) {
        try {
            Document doc = groupsCollection.find(new Document("inviteCode", inviteCode)).first();
            return convertDocumentToGroupInfo(doc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group by invite code: " + e.getMessage());
            return null;
        }
    }

    public List<GroupInfo> getPlayerGroups(UUID playerId) {
        try {
            List<Document> docs = new ArrayList<>();
            groupsCollection.find(new Document("members.playerId", playerId.toString())).into(docs);
            return docs.stream()
                    .map(this::convertDocumentToGroupInfo)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // uhm api specific method to get player groups as documents
    public List<Document> getPlayerGroupsAsDocuments(UUID playerId) {
        try {
            List<Document> docs = new ArrayList<>();
            groupsCollection.find(new Document("members.playerId", playerId.toString())).into(docs);
            return docs;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player groups as documents: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public GroupMember getGroupMember(UUID groupId, UUID playerId) {
        try {
            Document group = getGroup(groupId);
            if (group == null) return null;

            List<Document> members = group.getList("members", Document.class);
            Document memberDoc = members.stream()
                    .filter(member -> member.getString("playerId").equals(playerId.toString()))
                    .findFirst()
                    .orElse(null);

            return convertDocumentToGroupMember(memberDoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group member: " + e.getMessage());
            return null;
        }
    }

    public boolean isGroupAdmin(UUID groupId, UUID playerId) {
        return isAdminOrOwner(groupId, playerId);
    }

    public boolean isGroupOwner(UUID groupId, UUID playerId) {
        try {
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            return group != null && group.getString("ownerId").equals(playerId.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check group owner: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMemberRole(UUID groupId, UUID playerId, me.harshit.minechat.api.GroupMember.GroupRole newRole) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", playerId.toString()),
                    new Document("$set", new Document("members.$.role", newRole.name()))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update member role: " + e.getMessage());
            return false;
        }
    }

    public boolean banMember(UUID groupId, UUID adminId, UUID targetId, String reason) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            removePlayerFromGroup(groupId, targetId);

            Document banDoc = new Document()
                    .append("playerId", targetId.toString())
                    .append("bannedBy", adminId.toString())
                    .append("bannedAt", System.currentTimeMillis())
                    .append("reason", reason);

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("bannedMembers", banDoc))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban member: " + e.getMessage());
            return false;
        }
    }

    public boolean muteMember(UUID groupId, UUID targetId, UUID adminId, int durationMinutes) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            long muteUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", targetId.toString()),
                    new Document("$set", new Document("members.$.muteUntil", muteUntil))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to mute member: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteGroup(UUID groupId, UUID ownerId) {
        try {
            Document group = getGroup(groupId);
            if (group == null || !group.getString("ownerId").equals(ownerId.toString())) {
                return false;
            }

            groupsCollection.deleteOne(new Document("groupId", groupId.toString()));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete group: " + e.getMessage());
            return false;
        }
    }

    public boolean updateGroupPrivacy(UUID groupId, boolean isPrivate) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$set", new Document("isPrivate", isPrivate))
            ).getModifiedCount();
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group privacy: " + e.getMessage());
            return false;
        }
    }

    public void sendGroupMessage(me.harshit.minechat.api.GroupMessage message) {
        storeGroupMessage(message.getGroupId(), message.getSenderUUID(), message.getSenderName(),
                         message.getContent(), "web");
    }



    private me.harshit.minechat.api.GroupInfo convertDocumentToGroupInfo(Document doc) {
        if (doc == null) return null;

        try {
            UUID groupId = UUID.fromString(doc.getString("groupId"));
            String groupName = doc.getString("groupName");
            String description = doc.getString("description");
            UUID ownerId = UUID.fromString(doc.getString("ownerId"));
            String ownerName = doc.getString("ownerName");
            boolean isPrivate = doc.getBoolean("isPrivate", false);
            int maxMembers = doc.getInteger("maxMembers", 25);
            long createdDate = doc.getLong("createdDate");

            List<Document> memberDocs = doc.getList("members", Document.class);
            List<me.harshit.minechat.api.GroupMember> members = memberDocs.stream()
                .map(this::convertDocumentToGroupMember)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

            Document settingsDoc = doc.get("settings", Document.class);
            me.harshit.minechat.api.GroupSettings settings = convertDocumentToGroupSettings(settingsDoc);

            return new me.harshit.minechat.api.GroupInfo(groupId, groupName, description, ownerId, ownerName,
                               members, java.time.LocalDateTime.ofInstant(
                                   java.time.Instant.ofEpochMilli(createdDate),
                                   java.time.ZoneId.systemDefault()),
                               isPrivate, maxMembers, settings);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupInfo: " + e.getMessage());
            return null;
        }
    }

    // Helper method to convert Document to GroupMember
    private me.harshit.minechat.api.GroupMember convertDocumentToGroupMember(Document doc) {
        if (doc == null) return null;

        try {
            UUID playerId = UUID.fromString(doc.getString("playerId"));
            String playerName = doc.getString("playerName");
            String roleStr = doc.getString("role");
            long joinedDate = doc.getLong("joinedDate");

            me.harshit.minechat.api.GroupMember.GroupRole role = me.harshit.minechat.api.GroupMember.GroupRole.valueOf(roleStr.toUpperCase());
            boolean isOnline = org.bukkit.Bukkit.getPlayer(playerId) != null;

            return new me.harshit.minechat.api.GroupMember(playerId, playerName, role,
                                 java.time.LocalDateTime.ofInstant(
                                     java.time.Instant.ofEpochMilli(joinedDate),
                                     java.time.ZoneId.systemDefault()),
                                 isOnline);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupMember: " + e.getMessage());
            return null;
        }
    }

    private me.harshit.minechat.api.GroupSettings convertDocumentToGroupSettings(Document doc) {
        if (doc == null) return me.harshit.minechat.api.GroupSettings.getDefault();

        try {
            boolean allowInvites = doc.getBoolean("allowInvites", true);
            boolean friendsOnly = doc.getBoolean("friendsOnly", false);
            boolean muteNonMembers = doc.getBoolean("muteNonMembers", false);
            boolean logMessages = doc.getBoolean("logMessages", true);
            boolean webAccessEnabled = doc.getBoolean("webAccessEnabled", true);
            boolean joinRequiresApproval = doc.getBoolean("joinRequiresApproval", false);
            boolean membersCanInvite = doc.getBoolean("membersCanInvite", true);
            boolean onlyAdminsCanMessage = doc.getBoolean("onlyAdminsCanMessage", false);
            boolean enableAnnouncements = doc.getBoolean("enableAnnouncements", true);
            String joinMessage = doc.getString("joinMessage");
            String leaveMessage = doc.getString("leaveMessage");
            String groupMotd = doc.getString("groupMotd");
            String inviteCode = doc.getString("inviteCode");

            return new me.harshit.minechat.api.GroupSettings(allowInvites, friendsOnly, muteNonMembers,
                                   logMessages, webAccessEnabled, joinRequiresApproval,
                                   membersCanInvite, onlyAdminsCanMessage, enableAnnouncements,
                                   joinMessage, leaveMessage, null, null, null, groupMotd, inviteCode);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupSettings: " + e.getMessage());
            return me.harshit.minechat.api.GroupSettings.getDefault();
        }
    }
}
