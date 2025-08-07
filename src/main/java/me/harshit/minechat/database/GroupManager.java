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
      
        
        /*
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
        */
    }


    public List<Document> getGroupMessages(UUID groupId, int limit) {
        // DISABLED: We don't want to store or retrieve group messages
        // Messages are only sent in real-time via WebSocket
        return new ArrayList<>();
        
        /*
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
        */
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
            plugin.getLogger().info("Attempting to join group " + groupId + " with player " + playerName + " (" + playerUUID + ")");
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                plugin.getLogger().warning("Group not found: " + groupId);
                return false; // Group doesn't exist
            }

            // Check if player is already a member
            List<Document> members = group.getList("members", Document.class);
            boolean alreadyMember = members.stream()
                    .anyMatch(member -> member.getString("playerId").equals(playerUUID.toString()));

            if (alreadyMember) {
                plugin.getLogger().warning("Player " + playerName + " is already a member of group " + groupId);
                return false; // Already a member
            }

            int maxMembers = group.getInteger("maxMembers", 25);
            if (members.size() >= maxMembers) {
                plugin.getLogger().warning("Group " + groupId + " is full (" + members.size() + "/" + maxMembers + ")");
                return false; // Group is full
            }

            Document newMember = new Document()
                    .append("playerId", playerUUID.toString())
                    .append("playerName", playerName)
                    .append("role", "MEMBER")
                    .append("joinedDate", System.currentTimeMillis());

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("members", newMember))
            );

            plugin.getLogger().info("Successfully added player " + playerName + " to group " + groupId);
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
                            member.getString("playerId").equals(playerUUID.toString()) &&
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
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayerExact(playerName);
            if (player != null) {
                return player.getUniqueId();
            }

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
                    new Document("$set", new Document("settings.announcement", announcement))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group announcement: " + e.getMessage());
            return false;
        }
    }

    public boolean updateGroupMotd(UUID groupId, String motd) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$set", new Document("settings.groupMotd", motd))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group MOTD: " + e.getMessage());
            return false;
        }
    }

    public boolean muteMember(UUID groupId, UUID targetId, UUID adminId, int durationMinutes) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            Document group = getGroup(groupId);
            if (group == null) {
                return false;
            }

            if (isAdminOrOwner(groupId, targetId)) {
                return false;
            }

            long muteUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000);

            Document muteData = new Document()
                    .append("playerId", targetId.toString())
                    .append("mutedUntil", muteUntil)
                    .append("mutedBy", adminId.toString())
                    .append("mutedAt", System.currentTimeMillis());

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("settings.mutedMembers", muteData))
            );

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to mute member: " + e.getMessage());
            return false;
        }
    }

    public boolean unmuteMember(UUID groupId, UUID targetId, UUID adminId) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("settings.mutedMembers",
                            new Document("playerId", targetId.toString())))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unmute member: " + e.getMessage());
            return false;
        }
    }

    public boolean banMember(UUID groupId, UUID targetId, UUID adminId, String reason) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            Document group = getGroup(groupId);
            if (group == null || group.getString("ownerId").equals(targetId.toString())) {
                return false;
            }

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("members", new Document("playerId", targetId.toString())))
            );

            Document banData = new Document()
                    .append("playerId", targetId.toString())
                    .append("bannedBy", adminId.toString())
                    .append("bannedAt", System.currentTimeMillis())
                    .append("reason", reason);

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("settings.bannedMembers", banData))
            );

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban member: " + e.getMessage());
            return false;
        }
    }

    public boolean unbanMember(UUID groupId, UUID targetId, UUID adminId) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("settings.bannedMembers",
                            new Document("playerId", targetId.toString())))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unban member: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMemberRole(UUID groupId, UUID targetId, GroupMember.GroupRole newRole) {
        try {
            Document group = getGroup(groupId);
            if (group == null) {
                return false;
            }

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", targetId.toString()),
                    new Document("$set", new Document("members.$.role", newRole.name()))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update member role: " + e.getMessage());
            return false;
        }
    }



    public boolean promoteGroupMember(UUID groupId, UUID targetId) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", targetId.toString()),
                    new Document("$set", new Document("members.$.role", "ADMIN"))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to promote member: " + e.getMessage());
            return false;
        }
    }

    public boolean setMemberRole(UUID groupId, UUID targetId, String role) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", targetId.toString()),
                    new Document("$set", new Document("members.$.role", role.toUpperCase()))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set member role: " + e.getMessage());
            return false;
        }
    }

    public boolean clearGroupChat(UUID groupId, UUID adminId) {
        try {
            if (!isAdminOrOwner(groupId, adminId)) {
                return false;
            }

            long deletedCount = groupMessagesCollection.deleteMany(
                    new Document("groupId", groupId.toString())
            ).getDeletedCount();

            plugin.getLogger().info("Cleared " + deletedCount + " messages from group " + groupId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear group chat: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteGroup(UUID groupId, UUID ownerId) {
        try {
            Document group = getGroup(groupId);
            if (group == null || !group.getString("ownerId").equals(ownerId.toString())) {
                return false; 
            }

            groupMessagesCollection.deleteMany(new Document("groupId", groupId.toString()));

            groupInvitesCollection.deleteMany(new Document("groupId", groupId.toString()));

            long deletedCount = groupsCollection.deleteOne(new Document("groupId", groupId.toString())).getDeletedCount();

            return deletedCount > 0;
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

    public boolean isGroupOwner(UUID groupId, UUID playerId) {
        try {
            Document group = getGroup(groupId);
            return group != null && group.getString("ownerId").equals(playerId.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check group ownership: " + e.getMessage());
            return false;
        }
    }


    public GroupInfo createGroup(String groupName, String description, UUID ownerId, String ownerName, int maxMembers) {
        return createGroup(ownerId, ownerName, groupName, description, maxMembers, false);
    }

    public GroupInfo getGroupByName(String groupName) {
        try {
            Document groupDoc = groupsCollection.find(new Document("groupName", groupName)).first();
            return convertDocumentToGroupInfo(groupDoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group by name: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo getGroupByInviteCode(String inviteCode) {
        try {
            Document groupDoc = groupsCollection.find(new Document("settings.inviteCode", inviteCode)).first();
            return convertDocumentToGroupInfo(groupDoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group by invite code: " + e.getMessage());
            return null;
        }
    }

    public Document getGroupDocumentByInviteCode(String inviteCode) {
        try {
            return groupsCollection.find(new Document("settings.inviteCode", inviteCode)).first();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group document by invite code: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo getGroupById(UUID groupId) {
        try {
            Document groupDoc = getGroup(groupId);
            return convertDocumentToGroupInfo(groupDoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group by ID: " + e.getMessage());
            return null;
        }
    }

    public List<GroupInfo> getPlayerGroups(UUID playerId) {
        try {
            List<Document> groupDocs = new ArrayList<>();
            groupsCollection.find(new Document("members.playerId", playerId.toString())).into(groupDocs);

            List<GroupInfo> groups = new ArrayList<>();
            for (Document doc : groupDocs) {
                GroupInfo group = convertDocumentToGroupInfo(doc);
                if (group != null) {
                    groups.add(group);
                }
            }
            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getPlayerGroupsAsDocuments(UUID playerId) {
        try {
            List<Document> groups = new ArrayList<>();
            groupsCollection.find(new Document("members.playerId", playerId.toString())).into(groups);
            return groups;
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

    public List<Document> searchPublicGroups(String query, int limit) {
        try {
            List<Document> groups = new ArrayList<>();
            Document filter = new Document("isPrivate", false);

            if (!query.isEmpty()) {
                filter.append("$or", List.of(
                    new Document("groupName", new Document("$regex", query).append("$options", "i")),
                    new Document("description", new Document("$regex", query).append("$options", "i"))
                ));
            }

            groupsCollection.find(filter).limit(limit).into(groups);
            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to search public groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String generateInviteCode() {
        return "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private GroupInfo convertDocumentToGroupInfo(Document doc) {
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
            List<GroupMember> members = new ArrayList<>();
            for (Document memberDoc : memberDocs) {
                GroupMember member = convertDocumentToGroupMember(memberDoc);
                if (member != null) {
                    members.add(member);
                }
            }

            Document settingsDoc = doc.get("settings", Document.class);
            GroupSettings settings = convertDocumentToGroupSettings(settingsDoc);

            return new GroupInfo(groupId, groupName, description, ownerId, ownerName,
                    members, java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(createdDate),
                    java.time.ZoneId.systemDefault()),
                    isPrivate, maxMembers, settings);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupInfo: " + e.getMessage());
            return null;
        }
    }

    private GroupMember convertDocumentToGroupMember(Document doc) {
        if (doc == null) return null;

        try {
            UUID playerId = UUID.fromString(doc.getString("playerId"));
            String playerName = doc.getString("playerName");
            String roleStr = doc.getString("role");
            long joinedDate = doc.getLong("joinedDate");

            GroupMember.GroupRole role = GroupMember.GroupRole.valueOf(roleStr.toUpperCase());
            boolean isOnline = org.bukkit.Bukkit.getPlayer(playerId) != null;

            return new GroupMember(playerId, playerName, role,
                    java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(joinedDate),
                            java.time.ZoneId.systemDefault()),
                    isOnline);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupMember: " + e.getMessage());
            return null;
        }
    }

    private GroupSettings convertDocumentToGroupSettings(Document doc) {
        if (doc == null) {
            return GroupSettings.getDefault();
        }

        try {
            String inviteCode = doc.getString("inviteCode");
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

            if (joinMessage == null) joinMessage = "Welcome to the group!";
            if (leaveMessage == null) leaveMessage = "Thanks for being part of the group!";
            if (groupMotd == null) groupMotd = "";
            if (inviteCode == null) inviteCode = generateInviteCode();

            List<String> allowedRanks = doc.getList("allowedRanks", String.class);
            if (allowedRanks == null) allowedRanks = new ArrayList<>();

            List<String> mutedMemberStrings = doc.getList("mutedMembers", String.class);
            List<UUID> mutedMembers = new ArrayList<>();
            if (mutedMemberStrings != null) {
                for (String uuidStr : mutedMemberStrings) {
                    try {
                        mutedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in mutedMembers: " + uuidStr);
                    }
                }
            }

            List<String> bannedMemberStrings = doc.getList("bannedMembers", String.class);
            List<UUID> bannedMembers = new ArrayList<>();
            if (bannedMemberStrings != null) {
                for (String uuidStr : bannedMemberStrings) {
                    try {
                        bannedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in bannedMembers: " + uuidStr);
                    }
                }
            }

            return new GroupSettings(
                allowInvites,
                friendsOnly,
                muteNonMembers,
                logMessages,
                webAccessEnabled,
                joinRequiresApproval,
                membersCanInvite,
                onlyAdminsCanMessage,
                enableAnnouncements,
                joinMessage,
                leaveMessage,
                allowedRanks,
                mutedMembers,
                bannedMembers,
                groupMotd,
                inviteCode
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupSettings: " + e.getMessage());
            return GroupSettings.getDefault();
        }
    }


    public boolean isGroupAdmin(UUID groupId, UUID playerId) {
        return isAdminOrOwner(groupId, playerId);
    }

    public boolean removePlayerFromGroup(UUID groupId, UUID playerId) {
        return leaveGroup(playerId, groupId);
    }

    public boolean banPlayerFromGroup(UUID groupId, UUID playerId) {
        try {
            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$pull", new Document("members", new Document("playerId", playerId.toString())))
            );

            Document banData = new Document()
                    .append("playerId", playerId.toString())
                    .append("bannedAt", System.currentTimeMillis());

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("settings.bannedMembers", banData))
            );

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban player from group: " + e.getMessage());
            return false;
        }
    }

    public boolean muteGroupMember(UUID groupId, UUID playerId, long durationMs) {
        try {
            long muteUntil = System.currentTimeMillis() + durationMs;

            Document muteData = new Document()
                    .append("playerId", playerId.toString())
                    .append("mutedUntil", muteUntil)
                    .append("mutedAt", System.currentTimeMillis());

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$push", new Document("settings.mutedMembers", muteData))
            );

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to mute group member: " + e.getMessage());
            return false;
        }
    }

    public boolean joinGroupByInviteCode(UUID playerId, String playerName, String inviteCode) {
        try {
            plugin.getLogger().info("Attempting to join group with invite code: " + inviteCode);
            Document group = groupsCollection.find(new Document("settings.inviteCode", inviteCode)).first();
            if (group == null) {
                plugin.getLogger().warning("No group found with invite code: " + inviteCode);
                return false;
            }

            plugin.getLogger().info("Found group: " + group.getString("groupName") + " for invite code: " + inviteCode);
            UUID groupId = UUID.fromString(group.getString("groupId"));
            boolean result = joinGroup(groupId, playerId, playerName);
            plugin.getLogger().info("Join group result: " + result + " for player: " + playerName);
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to join group by invite code: " + e.getMessage());
            return false;
        }
    }

    public boolean canInviteToGroup(UUID groupId, UUID playerId) {
        try {
            GroupMember member = getGroupMember(groupId, playerId);
            if (member == null) return false;

            GroupInfo group = getGroupById(groupId);
            if (group == null) return false;

            return group.getSettings().isAllowInvites() &&
                   (group.getSettings().isMembersCanInvite() || member.getRole().canManageGroup());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check invite permission: " + e.getMessage());
            return false;
        }
    }

    public String generateGroupInviteCode(UUID groupId) {
        try {
            String newCode = generateInviteCode();

            groupsCollection.updateOne(
                    new Document("groupId", groupId.toString()),
                    new Document("$set", new Document("settings.inviteCode", newCode))
            );

            return newCode;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate group invite code: " + e.getMessage());
            return null;
        }
    }

    public GroupInfo createGroup(UUID ownerId, String ownerName, String groupName, String description, int maxMembers, boolean isPrivate) {
        try {
            if (groupExists(groupName)) {
                return null;
            }

            UUID groupId = UUID.randomUUID();
            long timestamp = System.currentTimeMillis();
            String inviteCode = generateInviteCode();

            Document settingsDoc = getDefaultSettingsDocument()
                    .append("inviteCode", inviteCode);

            Document groupDoc = new Document()
                    .append("groupId", groupId.toString())
                    .append("groupName", groupName)
                    .append("description", description)
                    .append("ownerId", ownerId.toString())
                    .append("ownerName", ownerName)
                    .append("isPrivate", isPrivate)
                    .append("createdDate", timestamp)
                    .append("maxMembers", maxMembers)
                    .append("settings", settingsDoc)
                    .append("members", List.of(createOwnerMemberDocument(ownerId, ownerName, timestamp)));

            groupsCollection.insertOne(groupDoc);

            return convertDocumentToGroupInfo(groupDoc);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create group: " + e.getMessage());
            return null;
        }
    }


    public List<Document> getAllPublicGroups() {
        try {
            List<Document> groups = new ArrayList<>();
            groupsCollection.find(new Document("isPrivate", false)).into(groups);

            for (Document group : groups) {
                List<Document> members = group.getList("members", Document.class);
                group.append("memberCount", members != null ? members.size() : 0);
            }

            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get all public groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getTrendingGroups() {
        try {
            List<Document> groups = new ArrayList<>();

            long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

            // For now, return most active public groups based on member count
            // TODO: Implement proper trending algorithm based on message activity
            groupsCollection.find(new Document("isPrivate", false))
                .sort(new Document("createdDate", -1))
                .limit(10)
                .into(groups);

            for (Document group : groups) {
                List<Document> members = group.getList("members", Document.class);
                group.append("memberCount", members != null ? members.size() : 0);
            }

            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get trending groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Document> getRecommendedGroups(UUID playerId) {
        try {
            List<Document> groups = new ArrayList<>();

            List<Document> playerGroups = getPlayerGroupsAsDocuments(playerId);
            List<String> playerGroupIds = playerGroups.stream()
                .map(g -> g.getString("groupId"))
                .toList();

            Document filter = new Document("isPrivate", false);
            if (!playerGroupIds.isEmpty()) {
                filter.append("groupId", new Document("$nin", playerGroupIds));
            }

            groupsCollection.find(filter)
                .sort(new Document("createdDate", -1))
                .limit(5)
                .into(groups);

            for (Document group : groups) {
                List<Document> members = group.getList("members", Document.class);
                group.append("memberCount", members != null ? members.size() : 0);
            }

            return groups;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get recommended groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean updateGroupInfo(UUID groupId, String groupName, String description, Integer maxMembers, Boolean isPrivate) {
        try {
            Document updateDoc = new Document();

            if (groupName != null && !groupName.trim().isEmpty()) {
                updateDoc.append("groupName", groupName.trim());
            }

            if (description != null) {
                updateDoc.append("description", description);
            }

            if (maxMembers != null && maxMembers > 0) {
                updateDoc.append("maxMembers", maxMembers);
            }

            if (isPrivate != null) {
                updateDoc.append("isPrivate", isPrivate);
            }

            if (updateDoc.isEmpty()) {
                return false; 
            }

            updateDoc.append("lastUpdated", System.currentTimeMillis());

            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$set", updateDoc)
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group info: " + e.getMessage());
            return false;
        }
    }

    public boolean updateGroupAnnouncements(UUID groupId, List<String> announcements) {
        try {
            if (announcements == null) {
                announcements = new ArrayList<>();
            }

            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$set", new Document("settings.announcements", announcements))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group announcements: " + e.getMessage());
            return false;
        }
    }

    public boolean banMember(UUID groupId, UUID memberId, UUID adminId) {
        try {
            boolean removed = removeMember(groupId, memberId);
            if (!removed) {
                return false;
            }

            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$addToSet", new Document("bannedMembers", memberId.toString()))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ban member: " + e.getMessage());
            return false;
        }
    }

    public boolean muteMember(UUID groupId, UUID memberId, UUID adminId, long durationMinutes) {
        try {
            long muteUntil = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
            
            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString())
                    .append("members.playerId", memberId.toString()),
                new Document("$set", new Document("members.$.muteUntil", muteUntil))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to mute member: " + e.getMessage());
            return false;
        }
    }

    public boolean promoteMember(UUID groupId, UUID memberId, UUID adminId) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString())
                    .append("members.playerId", memberId.toString()),
                new Document("$set", new Document("members.$.role", "admin"))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to promote member: " + e.getMessage());
            return false;
        }
    }

    public boolean demoteMember(UUID groupId, UUID targetId, UUID adminId) {
        try {
            if (!isGroupOwner(groupId, adminId)) {
                return false;
            }

            Document group = getGroup(groupId);
            if (group == null || group.getString("ownerId").equals(targetId.toString())) {
                return false;
            }

            long modifiedCount = groupsCollection.updateOne(
                    new Document("groupId", groupId.toString())
                            .append("members.playerId", targetId.toString()),
                    new Document("$set", new Document("members.$.role", "MEMBER"))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to demote member: " + e.getMessage());
            return false;
        }
    }

    public boolean updateGroupMOTD(UUID groupId, String motd) {
        try {
            if (motd == null) {
                motd = "";
            }

            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$set", new Document("settings.motd", motd))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update group MOTD: " + e.getMessage());
            return false;
        }
    }

    public boolean isPlayerBanned(UUID groupId, UUID playerId) {
        try {
            Document group = groupsCollection.find(new Document("groupId", groupId.toString())).first();
            if (group == null) {
                return false;
            }

            List<String> bannedMembers = group.getList("bannedMembers", String.class);
            return bannedMembers != null && bannedMembers.contains(playerId.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check if player is banned: " + e.getMessage());
            return false;
        }
    }

    public boolean removeMember(UUID groupId, UUID memberId) {
        try {
            long modifiedCount = groupsCollection.updateOne(
                new Document("groupId", groupId.toString()),
                new Document("$pull", new Document("members", new Document("playerId", memberId.toString())))
            ).getModifiedCount();

            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove member: " + e.getMessage());
            return false;
        }
    }

    public List<Document> getGroupInvites(UUID playerUUID) {
        try {
            List<Document> invites = new ArrayList<>();
            groupInvitesCollection.find(
                new Document("targetId", playerUUID.toString())
                    .append("status", "pending")
            ).into(invites);
            return invites;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get group invites: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean acceptGroupInviteById(String inviteId, UUID playerUUID) {
        try {
            Document invite = groupInvitesCollection.find(
                new Document("inviteId", inviteId)
                    .append("targetId", playerUUID.toString())
                    .append("status", "pending")
            ).first();
            
            if (invite == null) {
                return false;
            }
            
            String groupIdStr = invite.getString("groupId");
            UUID groupId = UUID.fromString(groupIdStr);
            
            boolean joinSuccess = joinGroup(groupId, playerUUID, invite.getString("targetName"));
            
            if (joinSuccess) {
                groupInvitesCollection.updateOne(
                    new Document("inviteId", inviteId),
                    new Document("$set", new Document("status", "accepted")
                        .append("processedAt", System.currentTimeMillis()))
                );
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to accept group invite: " + e.getMessage());
            return false;
        }
    }

    public boolean rejectGroupInviteById(String inviteId, UUID playerUUID) {
        try {
            long modifiedCount = groupInvitesCollection.updateOne(
                new Document("inviteId", inviteId)
                    .append("targetId", playerUUID.toString())
                    .append("status", "pending"),
                new Document("$set", new Document("status", "rejected")
                    .append("processedAt", System.currentTimeMillis()))
            ).getModifiedCount();
            
            return modifiedCount > 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reject group invite: " + e.getMessage());
            return false;
        }
    }
}
