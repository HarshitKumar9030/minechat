package me.harshit.minechat.web;

import com.google.gson.JsonObject;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.database.UserDataManager;
import net.kyori.adventure.text.Component;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebAPIHandler {

    private final Minechat plugin;
    private final UserDataManager userDataManager;
    private final FriendManager friendManager;
    private final GroupManager groupManager;
    private final MinechatWebSocketServer webSocketServer;

    // Store active web sessions for real-time updates
    private final Map<String, WebSession> activeSessions = new ConcurrentHashMap<>();

    public WebAPIHandler(Minechat plugin, UserDataManager userDataManager,
                        FriendManager friendManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
        this.friendManager = friendManager;
        this.groupManager = groupManager;

        int wsPort = plugin.getConfig().getInt("web.websocket-port", 8081);
        this.webSocketServer = new MinechatWebSocketServer(plugin, this, wsPort);
        this.webSocketServer.start();

    }

    // handles incoming web msgs and routes them to appropriate handlers
    public void handleWebMessage(String sessionId, String messageType, JsonObject data) {
        WebSession session = activeSessions.get(sessionId);
        if (session == null || !session.isAuthenticated()) {
            plugin.getLogger().warning("Unauthorized web message attempt from session: " + sessionId);
            sendWebResponse(sessionId, "error", "Unauthorized access");
            return;
        }


        switch (messageType.toLowerCase()) {
            case "friend_message":
                handleWebFriendMessage(session, data);
                break;

            case "group_message":
                handleWebGroupMessage(session, data);
                break;

            case "get_friends":
                handleGetFriends(session);
                break;

            case "get_groups":
                handleGetGroups(session);
                break;

            case "get_group_messages":
                handleGetGroupMessages(session, data);
                break;

            case "get_friend_requests":
                handleGetFriendRequests(session);
                break;

            case "send_friend_request":
                handleSendFriendRequest(session, data);
                break;

            case "accept_friend_request":
                handleAcceptFriendRequest(session, data);
                break;

            case "reject_friend_request":
                handleRejectFriendRequest(session, data);
                break;

            case "get_online_players":
                handleGetOnlinePlayers(session);
                break;

            case "group_announcement":
                handleWebGroupAnnouncement(session, data);
                break;

            case "kick_group_member":
                handleKickGroupMember(session, data);
                break;

            case "promote_group_member":
                handlePromoteGroupMember(session, data);
                break;

            case "ban_group_member":
                handleBanGroupMember(session, data);
                break;

            case "mute_group_member":
                handleMuteGroupMember(session, data);
                break;

            case "search_groups":
                handleSearchGroups(session, data);
                break;

            case "join_group_by_code":
                handleJoinGroupByCode(session, data);
                break;

            case "get_group_members":
                handleGetGroupMembers(session, data);
                break;

            case "send_group_invite":
                handleSendGroupInvite(session, data);
                break;

            case "create_group":
                handleCreateGroup(session, data);
                break;

            case "leave_group":
                handleLeaveGroup(session, data);
                break;

            case "get_group_invite_code":
                handleGetGroupInviteCode(session, data);
                break;

            default:
                plugin.getLogger().warning("Unknown web message type: " + messageType);
                sendWebResponse(sessionId, "error", "Unknown message type: " + messageType);
        }
    }

    // Web friend msg handler
    private void handleWebFriendMessage(WebSession session, JsonObject data) {
        String targetName = data.get("target").getAsString();
        String message = data.get("message").getAsString();

        // Prevent self-messaging
        if (session.getPlayerName().equalsIgnoreCase(targetName)) {
            sendWebResponse(session.getSessionId(), "error", "You cannot send a message to yourself");
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            sendWebResponse(session.getSessionId(), "error", "Player not online");
            return;
        }

        // Verify they're friends
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean areFriends = friendManager.areFriends(session.getPlayerId(), target.getUniqueId());

            if (!areFriends) {
                sendWebResponse(session.getSessionId(), "error", "You're not friends with this player");
                return;
            }

            // Send the message to Minecraft
            Bukkit.getScheduler().runTask(plugin, () -> {
                String format = plugin.getConfig().getString("private-messages.format",
                    "&7[&dPM&7] &e{sender} &7→ &e{receiver}&7: &f{message}");

                String formattedMessage = format
                    .replace("{sender}", session.getPlayerName() + " (Web)")
                    .replace("{receiver}", target.getName())
                    .replace("{message}", message);

                Component messageComponent = Component.text(formattedMessage.replace("&", "§"));
                target.sendMessage(messageComponent);

                // Send confirmation back to web
                sendWebResponse(session.getSessionId(), "message_sent", Map.of(
                    "type", "friend_message",
                    "target", targetName,
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                ));

                // Notify target's web sessions if they're online via web
                broadcastMinecraftMessage(session.getPlayerId(), session.getPlayerName(),
                    message, "friend_message", target.getUniqueId());
            });
        });
    }

    private void handleWebGroupMessage(WebSession session, JsonObject data) {
        // Handle both groupId and group name formats for backwards compatibility
        String groupIdStr = data.has("groupId") ? data.get("groupId").getAsString() : null;
        String groupNameParam = data.has("group") ? data.get("group").getAsString() : null;
        String message = data.has("content") ? data.get("content").getAsString() :
                        data.has("message") ? data.get("message").getAsString() : null;

        if (message == null) {
            sendWebResponse(session.getSessionId(), "error", "Missing message content");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document foundGroup = null;
            UUID foundGroupId = null;
            String finalGroupName = null;

            if (groupIdStr != null) {
                // Use groupId to find group directly
                try {
                    foundGroupId = UUID.fromString(groupIdStr);
                    foundGroup = groupManager.getGroup(foundGroupId);
                    if (foundGroup != null) {
                        // Verify user is member by checking if they're in the group's member list
                        List<Document> members = foundGroup.getList("members", Document.class);
                        boolean isMember = members.stream()
                            .anyMatch(member -> member.getString("playerId").equals(session.getPlayerId().toString()));
                        
                        if (isMember) {
                            finalGroupName = foundGroup.getString("groupName");
                        } else {
                            foundGroup = null; // User is not a member
                        }
                    }
                } catch (IllegalArgumentException e) {
                    sendWebResponse(session.getSessionId(), "error", "Invalid group ID format");
                    return;
                }
            } else if (groupNameParam != null) {
                // Find group by name (legacy method)
                List<Document> playerGroups = groupManager.getPlayerGroupsAsDocuments(session.getPlayerId());
                foundGroup = playerGroups.stream()
                        .filter(g -> g.getString("groupName").equalsIgnoreCase(groupNameParam))
                        .findFirst()
                        .orElse(null);
                
                if (foundGroup != null) {
                    foundGroupId = UUID.fromString(foundGroup.getString("groupId"));
                    finalGroupName = foundGroup.getString("groupName");
                }
            }

            if (foundGroup == null || foundGroupId == null) {
                sendWebResponse(session.getSessionId(), "error", "Group not found or you're not a member");
                return;
            }

            // Create final variables for use in lambda
            final Document finalGroup = foundGroup;
            final UUID finalGroupId = foundGroupId;
            final String groupNameForLambda = finalGroupName;

            // Store message for persistence - DISABLED: We don't want to store group messages
            // groupManager.storeGroupMessage(finalGroupId, session.getPlayerId(),
            //                              session.getPlayerName(), message, "web");

            // Send to all online group members
            Bukkit.getScheduler().runTask(plugin, () -> {
                String format = plugin.getConfig().getString("chat-groups.format",
                    "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
                String formattedMessage = format
                        .replace("{group}", groupNameForLambda)
                        .replace("{player}", session.getPlayerName() + " (Web)")
                        .replace("{message}", message);

                Component messageComponent = Component.text(formattedMessage.replace("&", "§"));

                // Get group members and send to online players
                List<Document> members = finalGroup.getList("members", Document.class);
                for (Document member : members) {
                    String memberName = member.getString("playerName");
                    Player onlineMember = Bukkit.getPlayerExact(memberName);
                    if (onlineMember != null && onlineMember.isOnline()) {
                        onlineMember.sendMessage(messageComponent);
                    }
                }

                // Notify all web sessions in this group
                broadcastToGroupWebSessions(finalGroupId, "group_message", Map.of(
                    "group", groupNameForLambda,
                    "groupId", finalGroupId.toString(),
                    "messageId", UUID.randomUUID().toString(),
                    "senderUUID", session.getPlayerId().toString(),
                    "senderName", session.getPlayerName(),
                    "content", message,
                    "timestamp", System.currentTimeMillis(),
                    "messageType", "TEXT",
                    "source", "web"
                ));

                sendWebResponse(session.getSessionId(), "message_sent", Map.of(
                    "type", "group_message",
                    "group", groupNameForLambda,
                    "groupId", finalGroupId.toString(),
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                ));
            });
        });
    }

    // sends friends list to the web interface
    private void handleGetFriends(WebSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> friends = friendManager.getFriendList(session.getPlayerId());

            Map<String, Object> response = new HashMap<>();
            response.put("friends", friends.stream().map(friend -> {
                String friendName = friend.getString("friendName");
                Player onlineFriend = Bukkit.getPlayerExact(friendName);

                Map<String, Object> friendData = new HashMap<>();
                friendData.put("name", friendName);
                friendData.put("uuid", friend.getString("friendId"));
                friendData.put("online", onlineFriend != null && onlineFriend.isOnline());
                friendData.put("since", friend.getLong("timestamp"));
                return friendData;
            }).toList());

            sendWebResponse(session.getSessionId(), "friends_list", response);
        });
    }

    // send group list to web
    private void handleGetGroups(WebSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> groups = groupManager.getPlayerGroupsAsDocuments(session.getPlayerId());

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups.stream().map(group -> {
                Map<String, Object> groupData = new HashMap<>();
                groupData.put("id", group.getString("groupId"));
                groupData.put("name", group.getString("groupName"));
                groupData.put("description", group.getString("description"));
                groupData.put("memberCount", group.getList("members", Document.class).size());
                groupData.put("maxMembers", group.getInteger("maxMembers"));
                return groupData;
            }).toList());

            sendWebResponse(session.getSessionId(), "groups_list", response);
        });
    }

    // recent group msgs
    private void handleGetGroupMessages(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        int limit = data.has("limit") ? data.get("limit").getAsInt() : 50;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> messages = groupManager.getGroupMessages(UUID.fromString(groupId), limit);

            Map<String, Object> response = new HashMap<>();
            response.put("messages", messages);
            response.put("groupId", groupId);

            sendWebResponse(session.getSessionId(), "group_messages", response);
        });
    }

    private void handleGetFriendRequests(WebSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> incomingRequests = friendManager.getIncomingFriendRequests(session.getPlayerId());
            List<Document> outgoingRequests = friendManager.getOutgoingFriendRequests(session.getPlayerId());

            Map<String, Object> response = new HashMap<>();
            response.put("incoming", incomingRequests);
            response.put("outgoing", outgoingRequests);

            sendWebResponse(session.getSessionId(), "friend_requests", response);
        });
    }

    private void handleSendFriendRequest(WebSession session, JsonObject data) {
        String targetName = data.get("targetName").getAsString();

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendWebResponse(session.getSessionId(), "error", "Player not found");
            return;
        }

        if (target.getUniqueId().equals(session.getPlayerId())) {
            sendWebResponse(session.getSessionId(), "error", "You cannot send a friend request to yourself");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = friendManager.sendFriendRequest(session.getPlayerId(), session.getPlayerName(),
                target.getUniqueId(), target.getName());

            if (success) {
                sendWebResponse(session.getSessionId(), "friend_request_sent", Map.of(
                    "targetName", targetName,
                    "message", "Friend request sent successfully"
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to send friend request");
            }
        });
    }

    private void handleAcceptFriendRequest(WebSession session, JsonObject data) {
        String requesterName = data.get("requesterName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player requester = Bukkit.getPlayerExact(requesterName);
            if (requester == null) {
                sendWebResponse(session.getSessionId(), "error", "Requester not found");
                return;
            }

            boolean success = friendManager.acceptFriendRequest(requester.getUniqueId(), session.getPlayerId());

            if (success) {
                sendWebResponse(session.getSessionId(), "friend_request_accepted", Map.of(
                    "requesterName", requesterName,
                    "message", "Friend request accepted"
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to accept friend request");
            }
        });
    }

    private void handleRejectFriendRequest(WebSession session, JsonObject data) {
        String requesterName = data.get("requesterName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player requester = Bukkit.getPlayerExact(requesterName);
            if (requester == null) {
                sendWebResponse(session.getSessionId(), "error", "Requester not found");
                return;
            }

            boolean success = friendManager.rejectFriendRequest(requester.getUniqueId(), session.getPlayerId());

            if (success) {
                sendWebResponse(session.getSessionId(), "friend_request_rejected", Map.of(
                    "requesterName", requesterName,
                    "message", "Friend request rejected"
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to reject friend request");
            }
        });
    }

    private void handleGetOnlinePlayers(WebSession session) {
        List<Map<String, Object>> onlinePlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("name", player.getName());
            playerData.put("uuid", player.getUniqueId().toString());
            playerData.put("displayName", player.displayName().toString());
            onlinePlayers.add(playerData);
        }

        sendWebResponse(session.getSessionId(), "online_players", Map.of(
            "players", onlinePlayers,
            "count", onlinePlayers.size()
        ));
    }

    private void handleWebGroupAnnouncement(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String announcement = data.get("announcement").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.isGroupAdmin(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to make announcements");
                return;
            }

            boolean success = groupManager.updateGroupAnnouncement(UUID.fromString(groupId), announcement);

            if (success) {
                sendWebResponse(session.getSessionId(), "announcement_updated", Map.of(
                    "groupId", groupId,
                    "announcement", announcement
                ));

                broadcastToGroupWebSessions(UUID.fromString(groupId), "group_announcement", Map.of(
                    "groupId", groupId,
                    "announcement", announcement,
                    "updatedBy", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to update announcement");
            }
        });
    }

    private void handleKickGroupMember(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String targetName = data.get("targetName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.isGroupAdmin(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to kick members");
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendWebResponse(session.getSessionId(), "error", "Player not found");
                return;
            }

            boolean success = groupManager.removePlayerFromGroup(UUID.fromString(groupId), target.getUniqueId());

            if (success) {
                sendWebResponse(session.getSessionId(), "member_kicked", Map.of(
                    "groupId", groupId,
                    "targetName", targetName
                ));

                broadcastToGroupWebSessions(UUID.fromString(groupId), "member_kicked", Map.of(
                    "groupId", groupId,
                    "targetName", targetName,
                    "kickedBy", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to kick member");
            }
        });
    }

    private void handlePromoteGroupMember(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String targetName = data.get("targetName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.isGroupOwner(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "Only group owners can promote members");
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendWebResponse(session.getSessionId(), "error", "Player not found");
                return;
            }

            boolean success = groupManager.promoteGroupMember(UUID.fromString(groupId), target.getUniqueId());

            if (success) {
                sendWebResponse(session.getSessionId(), "member_promoted", Map.of(
                    "groupId", groupId,
                    "targetName", targetName
                ));

                // Notify group members
                broadcastToGroupWebSessions(UUID.fromString(groupId), "member_promoted", Map.of(
                    "groupId", groupId,
                    "targetName", targetName,
                    "promotedBy", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to promote member");
            }
        });
    }

    private void handleBanGroupMember(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String targetName = data.get("targetName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Verify user is group admin/owner
            if (!groupManager.isGroupAdmin(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to ban members");
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendWebResponse(session.getSessionId(), "error", "Player not found");
                return;
            }

            boolean success = groupManager.banPlayerFromGroup(UUID.fromString(groupId), target.getUniqueId());

            if (success) {
                sendWebResponse(session.getSessionId(), "member_banned", Map.of(
                    "groupId", groupId,
                    "targetName", targetName
                ));

                // Notify group members
                broadcastToGroupWebSessions(UUID.fromString(groupId), "member_banned", Map.of(
                    "groupId", groupId,
                    "targetName", targetName,
                    "bannedBy", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to ban member");
            }
        });
    }

    private void handleMuteGroupMember(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String targetName = data.get("targetName").getAsString();
        long duration = data.has("duration") ? data.get("duration").getAsLong() : 3600000; // 1 hour default

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.isGroupAdmin(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to mute members");
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendWebResponse(session.getSessionId(), "error", "Player not found");
                return;
            }

            boolean success = groupManager.muteGroupMember(UUID.fromString(groupId), target.getUniqueId(), duration);

            if (success) {
                sendWebResponse(session.getSessionId(), "member_muted", Map.of(
                    "groupId", groupId,
                    "targetName", targetName,
                    "duration", duration
                ));

                broadcastToGroupWebSessions(UUID.fromString(groupId), "member_muted", Map.of(
                    "groupId", groupId,
                    "targetName", targetName,
                    "duration", duration,
                    "mutedBy", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to mute member");
            }
        });
    }

    private void handleSearchGroups(WebSession session, JsonObject data) {
        String query = data.get("query").getAsString();
        int limit = data.has("limit") ? data.get("limit").getAsInt() : 20;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> groups = groupManager.searchPublicGroups(query, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups.stream().map(group -> {
                Map<String, Object> groupData = new HashMap<>();
                groupData.put("id", group.getString("groupId"));
                groupData.put("name", group.getString("groupName"));
                groupData.put("description", group.getString("description"));
                groupData.put("memberCount", group.getList("members", Document.class).size());
                groupData.put("maxMembers", group.getInteger("maxMembers"));
                return groupData;
            }).toList());

            sendWebResponse(session.getSessionId(), "groups_search_results", response);
        });
    }

    private void handleJoinGroupByCode(WebSession session, JsonObject data) {
        String inviteCode = data.get("inviteCode").getAsString();
        plugin.getLogger().info("WebSocket: Player " + session.getPlayerName() + " attempting to join group with code: " + inviteCode);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = groupManager.joinGroupByInviteCode(session.getPlayerId(),
                session.getPlayerName(), inviteCode);

            plugin.getLogger().info("WebSocket: Join group by code result: " + success + " for player: " + session.getPlayerName());

            if (success) {
                sendWebResponse(session.getSessionId(), "group_joined", Map.of(
                    "inviteCode", inviteCode,
                    "message", "Successfully joined group"
                ));

                try {
                    Document group = groupManager.getGroupDocumentByInviteCode(inviteCode);
                    if (group != null) {
                        UUID groupId = UUID.fromString(group.getString("groupId"));
                        plugin.getLogger().info("WebSocket: Broadcasting member joined for group: " + group.getString("groupName"));
                        broadcastToGroupMembers(groupId, "member_joined", Map.of(
                            "playerName", session.getPlayerName(),
                            "playerId", session.getPlayerId().toString()
                        ));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to broadcast member joined: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("WebSocket: Failed to join group with code: " + inviteCode + " for player: " + session.getPlayerName());
                sendWebResponse(session.getSessionId(), "error", "Invalid invite code or failed to join group");
            }
        });
    }

    private void broadcastToGroupMembers(UUID groupId, String messageType, Object data) {
        try {
            Document group = groupManager.getGroup(groupId);
            if (group == null) return;

            List<Document> members = group.getList("members", Document.class);
            for (Document member : members) {
                String memberIdStr = member.getString("playerId");
                if (memberIdStr != null) {
                    UUID memberId = UUID.fromString(memberIdStr);
                    WebSession memberSession = activeSessions.get(memberId);
                    if (memberSession != null) {
                        sendWebResponse(memberSession.getSessionId(), messageType, data);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to broadcast to group members: " + e.getMessage());
        }
    }

    private void handleGetGroupMembers(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!isPlayerInGroup(session.getPlayerId(), UUID.fromString(groupId))) {
                sendWebResponse(session.getSessionId(), "error", "You are not a member of this group");
                return;
            }

            GroupInfo group = groupManager.getGroupById(UUID.fromString(groupId));
            if (group == null) {
                sendWebResponse(session.getSessionId(), "error", "Group not found");
                return;
            }

            List<Document> members = groupManager.getPlayerGroupsAsDocuments(session.getPlayerId());

            Map<String, Object> response = new HashMap<>();
            response.put("groupId", groupId);
            response.put("members", members.stream().map(member -> {
                String memberName = member.getString("playerName");
                Player onlineMember = Bukkit.getPlayerExact(memberName);

                Map<String, Object> memberData = new HashMap<>();
                memberData.put("name", memberName);
                memberData.put("uuid", member.getString("playerId"));
                memberData.put("role", member.getString("role"));
                memberData.put("online", onlineMember != null && onlineMember.isOnline());
                memberData.put("joinedAt", member.getLong("joinedAt"));
                return memberData;
            }).toList());

            sendWebResponse(session.getSessionId(), "group_members", response);
        });
    }

    private void handleSendGroupInvite(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();
        String targetName = data.get("targetName").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.canInviteToGroup(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to invite to this group");
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sendWebResponse(session.getSessionId(), "error", "Player not found");
                return;
            }

            boolean success = groupManager.sendGroupInvite(UUID.fromString(groupId),
                session.getPlayerId(), session.getPlayerName(), target.getUniqueId(), target.getName());

            if (success) {
                sendWebResponse(session.getSessionId(), "group_invite_sent", Map.of(
                    "groupId", groupId,
                    "targetName", targetName
                ));

                // notify target player if they have an active web session
                WebSession targetSession = activeSessions.get(target.getUniqueId());
                if (targetSession != null) {
                    sendWebResponse(targetSession.getSessionId(), "group_invite_received", Map.of(
                        "groupId", groupId,
                        "inviterName", session.getPlayerName(),
                        "groupName", getGroupName(groupId) // Helper method to get group name
                    ));
                }
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to send group invite");
            }
        });
    }

    private String getGroupName(String groupId) {
        try {
            Document group = groupManager.getGroup(UUID.fromString(groupId));
            return group != null ? group.getString("groupName") : "Unknown Group";
        } catch (Exception e) {
            return "Unknown Group";
        }
    }

    private void handleCreateGroup(WebSession session, JsonObject data) {
        String groupName = data.get("groupName").getAsString();
        String description = data.has("description") ? data.get("description").getAsString() : "";
        int maxMembers = data.has("maxMembers") ? data.get("maxMembers").getAsInt() : 20;
        boolean isPublic = data.has("isPublic") && data.get("isPublic").getAsBoolean();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            GroupInfo groupInfo = groupManager.createGroup(session.getPlayerId(), session.getPlayerName(),
                groupName, description, maxMembers, isPublic);

            if (groupInfo != null) {
                sendWebResponse(session.getSessionId(), "group_created", Map.of(
                    "groupId", groupInfo.getGroupId().toString(),
                    "groupName", groupName,
                    "description", description,
                    "maxMembers", maxMembers,
                    "isPublic", isPublic
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to create group");
            }
        });
    }

    private void handleLeaveGroup(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = groupManager.removePlayerFromGroup(UUID.fromString(groupId), session.getPlayerId());

            if (success) {
                sendWebResponse(session.getSessionId(), "group_left", Map.of(
                    "groupId", groupId
                ));

                // Notify remaining group members
                broadcastToGroupWebSessions(UUID.fromString(groupId), "member_left", Map.of(
                    "groupId", groupId,
                    "playerName", session.getPlayerName()
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to leave group");
            }
        });
    }

    private void handleGetGroupInviteCode(WebSession session, JsonObject data) {
        String groupId = data.get("groupId").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!groupManager.isGroupAdmin(UUID.fromString(groupId), session.getPlayerId())) {
                sendWebResponse(session.getSessionId(), "error", "You don't have permission to get invite codes");
                return;
            }

            String inviteCode = groupManager.generateGroupInviteCode(UUID.fromString(groupId));

            if (inviteCode != null) {
                sendWebResponse(session.getSessionId(), "group_invite_code", Map.of(
                    "groupId", groupId,
                    "inviteCode", inviteCode
                ));
            } else {
                sendWebResponse(session.getSessionId(), "error", "Failed to generate invite code");
            }
        });
    }

    public boolean authenticateSession(String sessionId, String username, String password) {
        if (!userDataManager.verifyWebPassword(username, password)) {
            return false;
        }

        Player player = Bukkit.getPlayerExact(username);
        UUID playerId = player != null ? player.getUniqueId() : getPlayerUUID(username);

        if (playerId == null) {
            return false;
        }

        WebSession session = new WebSession(sessionId, playerId, username, true);
        activeSessions.put(sessionId, session);

        plugin.getLogger().info("Web session authenticated for player: " + username);
        return true;
    }

    public void removeSession(String sessionId) {
        WebSession session = activeSessions.remove(sessionId);
        if (session != null) {
            plugin.getLogger().info("Removed web session: " + sessionId + " for player: " + session.getPlayerName());
        }
    }

    // broadcasts a message from Minecraft to web clients
    public void broadcastMinecraftMessage(UUID senderId, String senderName, String message, String type, Object context) {
        Map<String, Object> data = new HashMap<>();
        data.put("sender", senderName);
        data.put("senderId", senderId.toString());
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("source", "minecraft");

        switch (type) {
            case "friend_message":
                UUID targetId = (UUID) context;
                activeSessions.values().stream()
                    .filter(session -> session.getPlayerId().equals(targetId))
                    .forEach(session -> sendWebResponse(session.getSessionId(), "friend_message", data));
                break;

            case "group_message":
                UUID groupId = (UUID) context;
                data.put("groupId", groupId.toString());
                broadcastToGroupWebSessions(groupId, "group_message", data);
                break;
        }
    }

    private void sendWebResponse(String sessionId, String type, Object data) {
        if (MinechatWebSocketHandler.isSessionConnected(sessionId)) {
            MinechatWebSocketHandler.sendToSession(sessionId, type, data);
            plugin.getLogger().info("WebSocket response sent for session " + sessionId + ": " + type);
            return;
        }

        try {
            WebSession session = activeSessions.get(sessionId);
            if (session != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", type);
                response.put("data", data);
                response.put("timestamp", System.currentTimeMillis());
                response.put("sessionId", sessionId);

                session.addResponse(response);

                plugin.getLogger().info("Web response queued for session " + sessionId + ": " + type);
            } else {
                plugin.getLogger().warning("Attempted to send response to inactive session: " + sessionId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send web response: " + e.getMessage());
        }
    }

    private void broadcastToGroupWebSessions(UUID groupId, String type, Object data) {
        activeSessions.values().stream()
            .filter(session -> isPlayerInGroup(session.getPlayerId(), groupId))
            .forEach(session -> sendWebResponse(session.getSessionId(), type, data));
    }

    private boolean isPlayerInGroup(UUID playerId, UUID groupId) {
        List<Document> playerGroups = groupManager.getPlayerGroupsAsDocuments(playerId);
        return playerGroups.stream()
                .anyMatch(group -> group.getString("groupId").equals(groupId.toString()));
    }

    private UUID getPlayerUUID(String playerName) {
        // First try to get from online players
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return player.getUniqueId();
        }

        try {
            UUID playerUUID = userDataManager.getPlayerUUIDByName(playerName);
            if (playerUUID != null) {
                return playerUUID;
            }

            // if not found(which is rare), try to get from offline players using bukkit api
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer.getUniqueId();
            }

            plugin.getLogger().warning("Could not find UUID for player: " + playerName);
            return null;

        } catch (Exception e) {
            plugin.getLogger().warning("Error getting UUID for player " + playerName + ": " + e.getMessage());
            return null;
        }
    }

    public void shutdown() {
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping WebSocket server: " + e.getMessage());
            }
        }
    }

    public int getActiveSessionsCount() {
        return activeSessions.size();
    }

    public int getWebSocketConnectionsCount() {
        return MinechatWebSocketHandler.getActiveSessionIds().size();
    }

    // Represents a web session for a player
    private static class WebSession {
        private final String sessionId;
        private final UUID playerId;
        private final String playerName;
        private final boolean authenticated;
        private final List<Map<String, Object>> responseQueue;

        public WebSession(String sessionId, UUID playerId, String playerName, boolean authenticated) {
            this.sessionId = sessionId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.authenticated = authenticated;
            this.responseQueue = new ArrayList<>();
        }

        public String getSessionId() { return sessionId; }
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public boolean isAuthenticated() { return authenticated; }

        public void addResponse(Map<String, Object> response) {
            synchronized (responseQueue) {
                responseQueue.add(response);
                // keep only last 100 responses to prevent memory leaks
                if (responseQueue.size() > 100) {
                    responseQueue.remove(0);
                }
            }
        }

        public List<Map<String, Object>> getAndClearResponses() {
            synchronized (responseQueue) {
                List<Map<String, Object>> responses = new ArrayList<>(responseQueue);
                responseQueue.clear();
                return responses;
            }
        }
    }
}
