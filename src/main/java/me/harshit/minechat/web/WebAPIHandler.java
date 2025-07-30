package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.database.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final Gson gson;

    // Store active web sessions for real-time updates
    private final Map<String, WebSession> activeSessions = new ConcurrentHashMap<>();

    public WebAPIHandler(Minechat plugin, UserDataManager userDataManager,
                        FriendManager friendManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
        this.friendManager = friendManager;
        this.groupManager = groupManager;
        this.gson = new Gson();
    }

    // handles incoming web msgs and routes them to appropriate handlers
    public void handleWebMessage(String sessionId, String messageType, JsonObject data) {
        WebSession session = activeSessions.get(sessionId);
        if (session == null || !session.isAuthenticated()) {
            plugin.getLogger().warning("Unauthorized web message attempt from session: " + sessionId);
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

            default:
                plugin.getLogger().warning("Unknown web message type: " + messageType);
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
                sendWebResponse(session.getSessionId(), "message_sent", "Message delivered");
            });
        });
    }


    private void handleWebGroupMessage(WebSession session, JsonObject data) {
        String groupName = data.get("group").getAsString();
        String message = data.get("message").getAsString();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find the group
            List<Document> playerGroups = groupManager.getPlayerGroups(session.getPlayerId());
            Document group = playerGroups.stream()
                    .filter(g -> g.getString("groupName").equalsIgnoreCase(groupName))
                    .findFirst()
                    .orElse(null);

            if (group == null) {
                sendWebResponse(session.getSessionId(), "error", "Group not found or you're not a member");
                return;
            }

            UUID groupId = UUID.fromString(group.getString("groupId"));

            // Store message for persistence
            groupManager.storeGroupMessage(groupId, session.getPlayerId(),
                                         session.getPlayerName(), message, "web");

            // Send to all online group members
            Bukkit.getScheduler().runTask(plugin, () -> {
                String format = plugin.getConfig().getString("chat-groups.format",
                    "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
                String formattedMessage = format
                        .replace("{group}", groupName)
                        .replace("{player}", session.getPlayerName() + " (Web)")
                        .replace("{message}", message);

                Component messageComponent = Component.text(formattedMessage.replace("&", "§"));

                List<Document> members = group.getList("members", Document.class);
                for (Document member : members) {
                    String memberName = member.getString("playerName");
                    Player onlineMember = Bukkit.getPlayerExact(memberName);
                    if (onlineMember != null && onlineMember.isOnline()) {
                        onlineMember.sendMessage(messageComponent);
                    }
                }

                // Notify all web sessions in this group
                broadcastToGroupWebSessions(groupId, "group_message", Map.of(
                    "group", groupName,
                    "sender", session.getPlayerName(),
                    "message", message,
                    "timestamp", System.currentTimeMillis(),
                    "source", "web"
                ));

                sendWebResponse(session.getSessionId(), "message_sent", "Message sent to group");
            });
        });
    }

    // sends friends list to the web interfact

    private void handleGetFriends(WebSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> friends = friendManager.getFriendList(session.getPlayerId());

            Map<String, Object> response = new HashMap<>();
            response.put("friends", friends.stream().map(friend -> {
                String friendName = friend.getString("friendName");
                Player onlineFriend = Bukkit.getPlayerExact(friendName);

                Map<String, Object> friendData = new HashMap<>();
                friendData.put("name", friendName);
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
            List<Document> groups = groupManager.getPlayerGroups(session.getPlayerId());

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

            sendWebResponse(session.getSessionId(), "group_messages", response);
        });
    }

    // web session authentication
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

    // remove web session
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    // broadcasts a message from Minecraft to web clients
    public void broadcastMinecraftMessage(UUID senderId, String senderName, String message, String type, Object context) {
        Map<String, Object> data = new HashMap<>();
        data.put("sender", senderName);
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
                broadcastToGroupWebSessions(groupId, "group_message", data);
                break;
        }
    }


    private void sendWebResponse(String sessionId, String type, Object data) {
        // todo: Implement actual web communication using WebSocket or Server-Sent Events
        // For now, we'll use a simple session-based response system

        try {
            WebSession session = activeSessions.get(sessionId);
            if (session != null) {
                // Create response object
                Map<String, Object> response = new HashMap<>();
                response.put("type", type);
                response.put("data", data);
                response.put("timestamp", System.currentTimeMillis());
                response.put("sessionId", sessionId);

                // Store response in session for retrieval
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
        // Find all web sessions that are members of this group
        activeSessions.values().stream()
            .filter(session -> isPlayerInGroup(session.getPlayerId(), groupId))
            .forEach(session -> sendWebResponse(session.getSessionId(), type, data));
    }

    private boolean isPlayerInGroup(UUID playerId, UUID groupId) {
        List<Document> playerGroups = groupManager.getPlayerGroups(playerId);
        return playerGroups.stream()
                .anyMatch(group -> group.getString("groupId").equals(groupId.toString()));
    }

    private UUID getPlayerUUID(String playerName) {
        // You'd implement this to get UUID from database or Bukkit
        Player player = Bukkit.getPlayerExact(playerName);
        return player != null ? player.getUniqueId() : null;
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
                // Keep only last 100 responses to prevent memory leaks
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
