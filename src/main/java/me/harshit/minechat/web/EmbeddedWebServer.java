package me.harshit.minechat.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.database.UserDataManager;
import me.harshit.minechat.web.KickMemberHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.google.gson.JsonElement;
import java.util.UUID;
import java.util.concurrent.Executors;

public class EmbeddedWebServer {

    private final Minechat plugin;
    private final DatabaseManager databaseManager;
    private final UserDataManager userDataManager;
    private final FriendManager friendManager;
    private final GroupManager groupManager;
    private final Gson gson;
    private HttpServer server;
    private final int port;

    public EmbeddedWebServer(Minechat plugin, DatabaseManager databaseManager, UserDataManager userDataManager,
                           FriendManager friendManager, GroupManager groupManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.userDataManager = userDataManager;
        this.friendManager = friendManager;
        this.groupManager = groupManager;
        this.gson = new Gson();
        this.port = plugin.getConfig().getInt("web.port", 8080);
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            server.createContext("/", new CORSHandler());

            server.createContext("/api/auth", new AuthHandler());

            server.createContext("/api/friends", new FriendsHandler());
            server.createContext("/api/send-friend-request", new SendFriendRequestHandler());
            server.createContext("/api/accept-friend-request", new AcceptFriendRequestHandler());
            server.createContext("/api/reject-friend-request", new RejectFriendRequestHandler());
            server.createContext("/api/remove-friend", new RemoveFriendHandler());
            server.createContext("/api/cancel-friend-request", new CancelFriendRequestHandler());
            server.createContext("/api/friend-requests", new FriendRequestsHandler());
            server.createContext("/api/friend-requests/incoming", new IncomingFriendRequestsHandler());
            server.createContext("/api/friend-requests/outgoing", new OutgoingFriendRequestsHandler());
            server.createContext("/api/friend-stats", new FriendStatsHandler());
      
            server.createContext("/api/groups", new GroupsHandler());
            server.createContext("/api/create-group", new CreateGroupHandler());
            server.createContext("/api/delete-group", new DeleteGroupHandler());
            server.createContext("/api/join-group", new JoinGroupHandler());
            server.createContext("/api/join-group-by-code", new JoinGroupByCodeHandler(plugin));
            server.createContext("/api/leave-group", new LeaveGroupHandler());
            server.createContext("/api/update-group", new UpdateGroupHandler());
            server.createContext("/api/group-stats", new GroupStatsHandler());
            server.createContext("/api/group-members", new GroupMembersHandler());
            server.createContext("/api/group-invites", new GroupInvitesHandler(plugin, groupManager));
            server.createContext("/api/accept-group-invite", new AcceptGroupInviteHandler(plugin));
            server.createContext("/api/reject-group-invite", new RejectGroupInviteHandler(plugin));
            server.createContext("/api/add-announcement", new AddAnnouncementHandler(plugin));
            server.createContext("/api/group-details", new GroupDetailsHandler());
            
            KickMemberHandler moderationHandler = new KickMemberHandler(plugin);
            server.createContext("/api/kick-member", moderationHandler);
            server.createContext("/api/ban-member", moderationHandler);
            server.createContext("/api/mute-member", moderationHandler);
            server.createContext("/api/unmute-member", moderationHandler);
            server.createContext("/api/promote-member", moderationHandler);
            server.createContext("/api/demote-member", moderationHandler);
            server.createContext("/api/update-group-motd", moderationHandler);
            server.createContext("/api/update-announcement", moderationHandler);
            server.createContext("/api/remove-announcement", moderationHandler);

            server.createContext("/api/public-groups", new GroupsHandler());
            server.createContext("/api/trending-groups", new GroupsHandler());
            server.createContext("/api/recommended-groups", new GroupsHandler());

            server.createContext("/api/messages", new MessagesHandler());
            server.createContext("/api/private-messages", new PrivateMessagesHandler());
            server.createContext("/api/send-message", new SendMessageHandler());
            server.createContext("/api/group-messages", new GroupMessagesHandler());
            server.createContext("/api/send-group-message", new SendGroupMessageHandler());

            server.createContext("/api/users", new UsersHandler());
            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/search-players", new SearchPlayersHandler());
            server.createContext("/api/ranks", new RanksHandler());

            server.createContext("/api/user-settings", new UserSettingsHandler());
            server.createContext("/api/enable-web-access", new EnableWebAccessHandler());
            server.createContext("/api/disable-web-access", new DisableWebAccessHandler());
            server.createContext("/api/update-web-password", new UpdateWebPasswordHandler());

            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/health/detailed", new HealthHandler());
            server.createContext("/api/test", new TestHandler());

            server.start();
            plugin.getLogger().info("Web server started successfully on port " + port);
            plugin.getLogger().info("Access web API at: http://localhost:" + port);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start web server on port " + port + ": " + e.getMessage());
            plugin.getLogger().severe("Make sure port " + port + " is not in use by another application");
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error starting web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web server stopped");
        }
    }

    private class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
        }
    }

    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String username = json.get("username").getAsString();
                    String password = json.get("password").getAsString();

                    // verify creds
                    boolean isValid = userDataManager.verifyWebPassword(username, password);

                    Map<String, Object> response = new HashMap<>();
                    if (isValid) {
                        Player player = Bukkit.getPlayerExact(username);
                        UUID playerUUID = player != null ? player.getUniqueId() : getPlayerUUID(username);

                        // get rank info
                        String rank = "";
                        String formattedRank = "";
                        boolean isOnline = false;

                        if (player != null && player.isOnline()) {
                            rank = plugin.getRankManager().getPlayerRank(player);
                            formattedRank = plugin.getRankManager().getFormattedRank(player);
                            isOnline = true;
                        }

                        response.put("success", true);
                        response.put("user", Map.of(
                            "playerUUID", playerUUID.toString(),
                            "playerName", username,
                            "webAccessEnabled", true,
                            "rank", rank,
                            "formattedRank", formattedRank,
                            "online", isOnline,
                            "loginTime", System.currentTimeMillis()
                        ));
                        response.put("sessionToken", UUID.randomUUID().toString());

                        // Log successful authentication with rank info
                        plugin.getLogger().info("Web authentication successful for " + username +
                            (isOnline ? " (Online, Rank: " + rank + ")" : " (Offline)"));
                    } else {
                        response.put("success", false);
                        response.put("error", "Invalid credentials");
                    }

                    sendJsonResponse(exchange, response, isValid ? 200 : 401);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class FriendsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    List<Document> friends = friendManager.getFriendList(UUID.fromString(playerUUID));

                    friends.forEach(friend -> {
                        String friendName = friend.getString("friendName");
                        Player onlineFriend = Bukkit.getPlayerExact(friendName);
                        friend.append("online", onlineFriend != null && onlineFriend.isOnline());
                    });

                    Map<String, Object> response = Map.of("friends", friends);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class UserSettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");
                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    org.bson.Document user = userDataManager.getUserData(UUID.fromString(playerUUID));
                    if (user == null) {
                        sendErrorResponse(exchange, "User not found", 404);
                        return;
                    }

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("playerUUID", user.getString("playerUUID"));
                    userMap.put("playerName", user.getString("playerName"));
                    userMap.put("webAccessEnabled", user.getBoolean("webAccessEnabled", false));
                    userMap.put("rank", user.getString("rank"));
                    userMap.put("formattedRank", user.getString("formattedRank"));
                    userMap.put("online", user.getBoolean("online", false));
                    if (user.containsKey("lastSeen")) userMap.put("lastSeen", user.getLong("lastSeen"));
                    if (user.containsKey("firstJoin")) userMap.put("firstJoin", user.getLong("firstJoin"));

                    sendJsonResponse(exchange, Map.of("user", userMap), 200);
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class EnableWebAccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String playerUUID = json.has("playerUUID") ? json.get("playerUUID").getAsString() : null;
                    String playerName = json.has("playerName") ? json.get("playerName").getAsString() : null;
                    String password = json.has("password") ? json.get("password").getAsString() : null;

                    if (playerUUID == null || playerName == null || password == null || password.isEmpty()) {
                        sendErrorResponse(exchange, "playerUUID, playerName and password are required", 400);
                        return;
                    }

                    boolean success = userDataManager.setWebPassword(UUID.fromString(playerUUID), playerName, password);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", success);
                    res.put("message", success ? "Web access enabled" : "Failed to enable web access");
                    sendJsonResponse(exchange, res, success ? 200 : 500);
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class DisableWebAccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String playerUUID = json.has("playerUUID") ? json.get("playerUUID").getAsString() : null;
                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "playerUUID is required", 400);
                        return;
                    }
                    boolean success = userDataManager.disableWebAccess(UUID.fromString(playerUUID));
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", success);
                    res.put("message", success ? "Web access disabled" : "Failed to disable web access");
                    sendJsonResponse(exchange, res, success ? 200 : 500);
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class UpdateWebPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String playerUUID = json.has("playerUUID") ? json.get("playerUUID").getAsString() : null;
                    String playerName = json.has("playerName") ? json.get("playerName").getAsString() : null;
                    String currentPassword = json.has("currentPassword") ? json.get("currentPassword").getAsString() : null;
                    String newPassword = json.has("newPassword") ? json.get("newPassword").getAsString() : null;

                    if (playerUUID == null || playerName == null || newPassword == null || newPassword.isEmpty()) {
                        sendErrorResponse(exchange, "playerUUID, playerName and newPassword are required", 400);
                        return;
                    }

                    if (currentPassword != null && !currentPassword.isEmpty()) {
                        boolean ok = userDataManager.verifyWebPassword(playerName, currentPassword);
                        if (!ok) {
                            sendErrorResponse(exchange, "Current password incorrect", 401);
                            return;
                        }
                    }

                    boolean success = userDataManager.setWebPassword(UUID.fromString(playerUUID), playerName, newPassword);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", success);
                    res.put("message", success ? "Password updated" : "Failed to update password");
                    sendJsonResponse(exchange, res, success ? 200 : 500);
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class GroupsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String path = exchange.getRequestURI().getPath();
                    String query = exchange.getRequestURI().getQuery();

                    if (path.endsWith("/public-groups")) {
                        handlePublicGroups(exchange);
                    } else if (path.endsWith("/trending-groups")) {
                        handleTrendingGroups(exchange);
                    } else if (path.endsWith("/recommended-groups")) {
                        handleRecommendedGroups(exchange, query);
                    } else {
                        String playerUUID = getQueryParam(query, "playerUUID");
                        if (playerUUID == null) {
                            sendErrorResponse(exchange, "Player UUID required", 400);
                            return;
                        }
                        UUID playerId = UUID.fromString(playerUUID);
                        List<Document> groups = groupManager.getPlayerGroupsAsDocuments(playerId);
                        
                        for (Document group : groups) {
                            List<Document> members = group.getList("members", Document.class);
                            group.append("memberCount", members != null ? members.size() : 0);
                            
                            String userRole = members.stream()
                                .filter(member -> member.getString("playerId").equals(playerId.toString()))
                                .map(member -> member.getString("role"))
                                .findFirst()
                                .orElse("MEMBER");
                            group.append("role", userRole);
                            
                            Document settings = group.get("settings", Document.class);
                            if (settings != null) {
                                String motd = settings.getString("groupMotd");
                                if (motd != null && !motd.isEmpty()) {
                                    group.append("motd", motd);
                                }
                                
                                List<String> announcements = settings.getList("announcements", String.class);
                                if (announcements != null && !announcements.isEmpty()) {
                                    group.append("announcements", announcements);
                                }
                            }
                            
                            if (!group.containsKey("motd") && group.getInteger("memberCount", 0) == 0) {
                                group.append("motd", "Welcome to " + group.getString("groupName") + "! Start chatting to get the conversation going.");
                            }
                            if (!group.containsKey("announcements") && group.getInteger("memberCount", 0) == 0) {
                                group.append("announcements", List.of(
                                    "📋 Group rules: Be respectful and have fun!",
                                    "🎉 New group created - say hello to everyone!"
                                ));
                            }
                        }

                        Map<String, Object> response = Map.of("groups", groups);
                        sendJsonResponse(exchange, response, 200);
                    }
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }

        private void handlePublicGroups(HttpExchange exchange) throws IOException {
            try {
                List<Document> publicGroups = groupManager.getAllPublicGroups();
                for (Document group : publicGroups) {
                    Document settings = group.get("settings", Document.class);
                    if (settings != null) {
                        String motd = settings.getString("groupMotd");
                        if (motd != null && !motd.isEmpty()) {
                            group.append("motd", motd);
                        }
                        
                        List<String> announcements = settings.getList("announcements", String.class);
                        if (announcements != null && !announcements.isEmpty()) {
                            group.append("announcements", announcements);
                        }
                    }
                    
                    if (!group.containsKey("motd") && group.getInteger("memberCount", 0) <= 1) {
                        group.append("motd", "Join " + group.getString("groupName") + " and be part of our growing community!");
                    }
                    if (!group.containsKey("announcements") && group.getInteger("memberCount", 0) <= 1) {
                        group.append("announcements", List.of(
                            "🌟 New members always welcome!",
                            "💬 Active community - join the conversation!"
                        ));
                    }
                }
                Map<String, Object> response = Map.of("groups", publicGroups);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to load public groups", 500);
            }
        }

        private void handleTrendingGroups(HttpExchange exchange) throws IOException {
            try {
                List<Document> trendingGroups = groupManager.getTrendingGroups();
                Map<String, Object> response = Map.of("groups", trendingGroups);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to load trending groups", 500);
            }
        }

        private void handleRecommendedGroups(HttpExchange exchange, String query) throws IOException {
            try {
                String playerUUID = getQueryParam(query, "playerUUID");
                if (playerUUID == null) {
                    sendErrorResponse(exchange, "Player UUID required", 400);
                    return;
                }
                List<Document> recommendedGroups = groupManager.getRecommendedGroups(UUID.fromString(playerUUID));
                Map<String, Object> response = Map.of("groups", recommendedGroups);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to load recommended groups", 500);
            }
        }
    }

    private class GroupDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String groupId = getQueryParam(query, "groupId");
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID required", 400);
                        return;
                    }

                    UUID groupUUID = UUID.fromString(groupId);
                    Document groupDoc = groupManager.getGroup(groupUUID);
                    if (groupDoc == null) {
                        sendErrorResponse(exchange, "Group not found", 404);
                        return;
                    }

                    Map<String, Object> group = new HashMap<>();
                    group.put("groupId", groupId);
                    group.put("groupName", groupDoc.getString("groupName"));
                    group.put("description", groupDoc.getString("description"));
                    group.put("maxMembers", groupDoc.getInteger("maxMembers", 20));
                    group.put("isPrivate", groupDoc.getBoolean("isPrivate", false));

                    if (groupDoc.containsKey("createdAt")) {
                        try {
                            Object createdAt = groupDoc.get("createdAt");
                            if (createdAt instanceof Number) {
                                group.put("createdAt", ((Number) createdAt).longValue());
                            }
                        } catch (Exception ignored) {}
                    }

                    List<Document> members = groupDoc.getList("members", Document.class);
                    int memberCount = members != null ? members.size() : 0;
                    group.put("memberCount", memberCount);

                    if (playerUUID != null && members != null) {
                        String role = members.stream()
                                .filter(m -> playerUUID.equals(m.getString("playerId")))
                                .map(m -> m.getString("role"))
                                .findFirst()
                                .orElse(null);
                        if (role != null) {
                            group.put("role", role);
                        }
                    }

                    if (groupDoc.containsKey("ownerId")) {
                        group.put("ownerId", groupDoc.getString("ownerId"));
                    }
                    if (groupDoc.containsKey("ownerName")) {
                        group.put("ownerName", groupDoc.getString("ownerName"));
                    }

                    Document settings = groupDoc.get("settings", Document.class);
                    if (settings != null) {
                        String motd = settings.getString("groupMotd");
                        if (motd != null && !motd.isEmpty()) {
                            group.put("motd", motd);
                        }

                        List<String> announcements = settings.getList("announcements", String.class);
                        if (announcements != null && !announcements.isEmpty()) {
                            group.put("announcements", announcements);
                        }
                    }

                    Map<String, Object> response = Map.of("group", group);
                    sendJsonResponse(exchange, response, 200);
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(exchange, "Invalid UUID format", 400);
                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String groupId = getQueryParam(query, "groupId");
                    String limit = getQueryParam(query, "limit");

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID required", 400);
                        return;
                    }

                    int messageLimit = limit != null ? Integer.parseInt(limit) : 50;
                    List<Document> messages = groupManager.getGroupMessages(UUID.fromString(groupId), messageLimit);

                    Map<String, Object> response = Map.of("messages", messages);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class PrivateMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String player1 = getQueryParam(query, "player1");
                    String player2 = getQueryParam(query, "player2");
                    String limitParam = getQueryParam(query, "limit");

                    if (player1 == null || player2 == null) {
                        sendErrorResponse(exchange, "player1 and player2 are required", 400);
                        return;
                    }

                    int limit = 100;
                    if (limitParam != null) {
                        try { limit = Integer.parseInt(limitParam); } catch (NumberFormatException ignored) {}
                    }

                    List<Document> messages = databaseManager.getPrivateMessages(player1, player2, limit);

                    Map<String, Object> response = Map.of("messages", messages);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class SendMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderId = json.get("senderId").getAsString();
                    String senderName = json.get("senderName").getAsString();
                    String message = json.get("message").getAsString();
                    String groupId = json.get("groupId").getAsString();

   

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        if (group != null) {
                            String groupName = group.getString("groupName");
                            String format = plugin.getConfig().getString("chat-groups.format",
                                "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
                            String formattedMessage = format
                                    .replace("{group}", groupName)
                                    .replace("{player}", senderName + " (Web)")
                                    .replace("{message}", message);

                            List<Document> members = group.getList("members", Document.class);
                            for (Document member : members) {
                                String memberName = member.getString("playerName");
                                Player onlineMember = Bukkit.getPlayerExact(memberName);
                                if (onlineMember != null && onlineMember.isOnline()) {
                                    onlineMember.sendMessage(formattedMessage.replace("&", "§"));
                                }
                            }
                        }
                    });

                    Map<String, Object> response = Map.of("success", true);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class SendFriendMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderUUIDStr = json.has("senderUUID") && !json.get("senderUUID").isJsonNull() ? json.get("senderUUID").getAsString() : null;
                    String senderName = json.has("senderName") && !json.get("senderName").isJsonNull() ? json.get("senderName").getAsString() : null;
                    String targetUUIDStr = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;
                    String targetName = json.has("targetName") && !json.get("targetName").isJsonNull() ? json.get("targetName").getAsString() : null;
                    String message = json.has("message") && !json.get("message").isJsonNull() ? json.get("message").getAsString() : null;

                    if (message == null || message.trim().isEmpty()) {
                        sendErrorResponse(exchange, "Message content required", 400);
                        return;
                    }

                    UUID senderUUID = null;
                    if (senderUUIDStr != null) {
                        try { senderUUID = UUID.fromString(senderUUIDStr); } catch (Exception ignored) {}
                    }
                    if (senderUUID == null && senderName != null) {
                        senderUUID = getPlayerUUID(senderName);
                    }
                    if (senderName == null && senderUUID != null) {
                        senderName = getPlayerNameByUUID(senderUUID);
                    }

                    UUID targetUUID = null;
                    if (targetUUIDStr != null) {
                        try { targetUUID = UUID.fromString(targetUUIDStr); } catch (Exception ignored) {}
                    }
                    if (targetUUID == null && targetName != null) {
                        targetUUID = getPlayerUUID(targetName);
                    }
                    if (targetName == null && targetUUID != null) {
                        targetName = getPlayerNameByUUID(targetUUID);
                    }

                    if (senderUUID == null || targetUUID == null || senderName == null || targetName == null) {
                        sendErrorResponse(exchange, "Invalid sender/target identifiers", 400);
                        return;
                    }

                    if (senderUUID.equals(targetUUID)) {
                        sendErrorResponse(exchange, "Cannot message yourself", 400);
                        return;
                    }

                    boolean areFriends = friendManager.areFriends(senderUUID, targetUUID);
                    if (!areFriends) {
                        sendErrorResponse(exchange, "You're not friends with this player", 403);
                        return;
                    }

                    Player target = Bukkit.getPlayerExact(targetName);
                    // Allow messaging to offline players, just store in database and notify web sessions

                    final String finalSenderName = senderName;
                    final String finalMessage = message;
                    final UUID finalSenderUUID = senderUUID;
                    final String finalTargetName = targetName;
                    final UUID finalTargetUUID = targetUUID;
                    final Player finalTarget = target;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Send to Minecraft if player is online
                        if (finalTarget != null && finalTarget.isOnline()) {
                            String format = plugin.getConfig().getString("private-messages.format",
                                "&7[&dPM&7] &e{sender} &7→ &e{receiver}&7: &f{message}");

                            String formattedMessage = format
                                .replace("{sender}", finalSenderName + " (Web)")
                                .replace("{receiver}", finalTarget.getName())
                                .replace("{message}", finalMessage);

                            finalTarget.sendMessage(formattedMessage.replace("&", "§"));
                        }

                        if (plugin.getDatabaseManager() != null) {
                            plugin.getDatabaseManager().storePrivateMessage(
                                finalSenderName, finalSenderUUID, 
                                finalTargetName, finalTargetUUID, 
                                finalMessage, "web"
                            );
                        }

                        if (plugin.getWebAPIHandler() != null) {
                            plugin.getWebAPIHandler().broadcastMinecraftMessage(finalSenderUUID, finalSenderName, finalMessage,
                                "friend_message", finalTargetUUID);
                        }
                    });

                    Map<String, Object> response = Map.of(
                        "success", true,
                        "targetName", targetName,
                        "timestamp", System.currentTimeMillis(),
                        "delivered", target != null && target.isOnline()
                    );
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class SendDirectMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderUUIDStr = json.has("senderUUID") && !json.get("senderUUID").isJsonNull() ? json.get("senderUUID").getAsString() : null;
                    String senderName = json.has("senderName") && !json.get("senderName").isJsonNull() ? json.get("senderName").getAsString() : null;
                    String targetUUIDStr = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;
                    String targetName = json.has("targetName") && !json.get("targetName").isJsonNull() ? json.get("targetName").getAsString() : null;
                    String message = json.has("message") && !json.get("message").isJsonNull() ? json.get("message").getAsString() : null;

                    if (message == null || message.trim().isEmpty()) {
                        sendErrorResponse(exchange, "Message content required", 400);
                        return;
                    }

                    UUID senderUUID = null;
                    if (senderUUIDStr != null) {
                        try { senderUUID = UUID.fromString(senderUUIDStr); } catch (Exception ignored) {}
                    }
                    if (senderUUID == null && senderName != null) {
                        senderUUID = getPlayerUUID(senderName);
                    }
                    if (senderName == null && senderUUID != null) {
                        senderName = getPlayerNameByUUID(senderUUID);
                    }

                    UUID targetUUID = null;
                    if (targetUUIDStr != null) {
                        try { targetUUID = UUID.fromString(targetUUIDStr); } catch (Exception ignored) {}
                    }
                    if (targetUUID == null && targetName != null) {
                        targetUUID = getPlayerUUID(targetName);
                    }
                    if (targetName == null && targetUUID != null) {
                        targetName = getPlayerNameByUUID(targetUUID);
                    }

                    if (senderUUID == null || targetUUID == null || senderName == null || targetName == null) {
                        sendErrorResponse(exchange, "Invalid sender/target identifiers", 400);
                        return;
                    }

                    if (senderUUID.equals(targetUUID)) {
                        sendErrorResponse(exchange, "Cannot message yourself", 400);
                        return;
                    }

                    Player target = Bukkit.getPlayerExact(targetName);
                    // Allow messaging to offline players, just store in database and notify web sessions

                    final String finalSenderName = senderName;
                    final String finalMessage = message;
                    final UUID finalSenderUUID = senderUUID;
                    final String finalTargetName = targetName;
                    final UUID finalTargetUUID = targetUUID;
                    final Player finalTarget = target;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Send to Minecraft if player is online
                        if (finalTarget != null && finalTarget.isOnline()) {
                            String format = plugin.getConfig().getString("private-messages.format",
                                "&7[&dPM&7] &e{sender} &7→ &e{receiver}&7: &f{message}");

                            String formattedMessage = format
                                .replace("{sender}", finalSenderName + " (Web)")
                                .replace("{receiver}", finalTarget.getName())
                                .replace("{message}", finalMessage);

                            finalTarget.sendMessage(formattedMessage.replace("&", "§"));
                        }

                        if (plugin.getDatabaseManager() != null) {
                            plugin.getDatabaseManager().storePrivateMessage(
                                finalSenderName, finalSenderUUID, 
                                finalTargetName, finalTargetUUID, 
                                finalMessage, "web"
                            );
                        }

                        if (plugin.getWebAPIHandler() != null) {
                            plugin.getWebAPIHandler().broadcastMinecraftMessage(finalSenderUUID, finalSenderName, finalMessage,
                                "friend_message", finalTargetUUID);
                        }
                    });

                    Map<String, Object> response = Map.of(
                        "success", true,
                        "targetName", targetName,
                        "timestamp", System.currentTimeMillis(),
                        "delivered", target != null && target.isOnline()
                    );
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, Object> response = Map.of("status", "Server is running");
                sendJsonResponse(exchange, response, 200);
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String path = exchange.getRequestURI().getPath();

                    if (path.endsWith("/detailed")) {
                        Map<String, Object> health = new HashMap<>();
                        health.put("status", "UP");
                        health.put("timestamp", System.currentTimeMillis());
                        health.put("server", "Minechat Web API");
                        health.put("version", "1.0.0");

                        Map<String, Object> components = new HashMap<>();

                        try {
                            if (databaseManager != null && databaseManager.isConnected()) {
                                components.put("database", Map.of(
                                    "status", "UP",
                                    "type", "MongoDB"
                                ));
                            } else {
                                components.put("database", Map.of(
                                    "status", "DOWN",
                                    "type", "MongoDB"
                                ));
                            }
                        } catch (Exception e) {
                            components.put("database", Map.of(
                                "status", "DOWN",
                                "error", e.getMessage()
                            ));
                        }

                        try {
                            components.put("bukkit", Map.of(
                                "status", "UP",
                                "onlinePlayers", Bukkit.getOnlinePlayers().size(),
                                "maxPlayers", Bukkit.getMaxPlayers()
                            ));
                        } catch (Exception e) {
                            components.put("bukkit", Map.of(
                                "status", "DOWN",
                                "error", e.getMessage()
                            ));
                        }

                        health.put("components", components);
                        sendJsonResponse(exchange, health, 200);
                    } else {
                        Map<String, Object> health = Map.of(
                            "status", "UP",
                            "timestamp", System.currentTimeMillis()
                        );
                        sendJsonResponse(exchange, health, 200);
                    }
                } catch (Exception e) {
                    Map<String, Object> health = Map.of(
                        "status", "DOWN",
                        "timestamp", System.currentTimeMillis(),
                        "error", e.getMessage()
                    );
                    sendJsonResponse(exchange, health, 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class RanksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    List<Document> ranks = userDataManager.getAllRanks();
                    Map<String, Object> response = Map.of("ranks", ranks);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    Document user = userDataManager.getUserData(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("user", user);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    // more api handlers to handle offline friend requests, group management, etc.
    private class SendFriendRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderUUID = json.get("senderUUID").getAsString();
                    String senderName = json.get("senderName").getAsString();
                    String targetName = json.get("targetName").getAsString();

                    if (!userDataManager.playerExists(targetName)) {
                        sendErrorResponse(exchange, "Player not found", 404);
                        return;
                    }

                    UUID targetUUID = userDataManager.getPlayerUUIDByName(targetName);
                    boolean success = friendManager.sendFriendRequest(
                        UUID.fromString(senderUUID), senderName, targetUUID, targetName
                    );

                    if (success) {
                        Player targetPlayer = Bukkit.getPlayerExact(targetName);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.sendMessage("§aYou received a friend request from " + senderName + " (via web)");
                        }

                        Map<String, Object> response = Map.of("success", true, "message", "Friend request sent");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to send friend request", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class AcceptFriendRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String playerUUID = json.get("playerUUID").getAsString();
                    String requesterUUID = json.get("requesterUUID").getAsString();

                    boolean success = friendManager.acceptFriendRequest(
                        UUID.fromString(playerUUID), UUID.fromString(requesterUUID)
                    );

                    if (success) {
                        String requesterName = getPlayerNameByUUID(UUID.fromString(requesterUUID));
                        Player requesterPlayer = Bukkit.getPlayerExact(requesterName);
                        if (requesterPlayer != null && requesterPlayer.isOnline()) {
                            String playerName = getPlayerNameByUUID(UUID.fromString(playerUUID));
                            requesterPlayer.sendMessage("§a" + playerName + " accepted your friend request!");
                        }

                        Map<String, Object> response = Map.of("success", true, "message", "Friend request accepted");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to accept friend request", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class RejectFriendRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String playerUUID = json.get("playerUUID").getAsString();
                    String requesterUUID = json.get("requesterUUID").getAsString();

                    boolean success = friendManager.rejectFriendRequest(
                        UUID.fromString(playerUUID), UUID.fromString(requesterUUID)
                    );

                    Map<String, Object> response = Map.of("success", success);
                    sendJsonResponse(exchange, response, success ? 200 : 400);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class RemoveFriendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String playerUUID = json.get("playerUUID").getAsString();
                    String friendUUID = json.get("friendUUID").getAsString();

                    boolean success = friendManager.removeFriend(
                        UUID.fromString(playerUUID), UUID.fromString(friendUUID)
                    );

                    Map<String, Object> response = Map.of("success", success);
                    sendJsonResponse(exchange, response, success ? 200 : 400);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class CreateGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String creatorUUID = json.get("creatorUUID").getAsString();
                    String creatorName = json.get("creatorName").getAsString();
                    String groupName = json.get("groupName").getAsString();
                    String description = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : "";
                    int maxMembers = json.has("maxMembers") && !json.get("maxMembers").isJsonNull() ? json.get("maxMembers").getAsInt() : 20;
                    boolean isPrivate = false;
                    if (json.has("isPrivate") && !json.get("isPrivate").isJsonNull()) {
                        try {
                            isPrivate = json.get("isPrivate").getAsBoolean();
                        } catch (Exception e) {
                            isPrivate = false;
                        }
                    }

                    GroupInfo createdGroup = groupManager.createGroup(
                        UUID.fromString(creatorUUID), creatorName, groupName, description, maxMembers, isPrivate
                    );

                    if (createdGroup != null) {
                        Map<String, Object> response = Map.of(
                            "success", true,
                            "message", "Group created successfully",
                            "groupId", createdGroup.getGroupId().toString()
                        );
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to create group", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class JoinGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String playerUUID = json.get("playerUUID").getAsString();
                    String playerName = json.get("playerName").getAsString();
                    String groupId = json.get("groupId").getAsString();

                    boolean success = groupManager.joinGroup(
                        UUID.fromString(groupId), UUID.fromString(playerUUID), playerName
                    );

                    if (success) {
                        // Notify online group members
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        if (group != null) {
                            String groupName = group.getString("groupName");
                            List<Document> members = group.getList("members", Document.class);
                            for (Document member : members) {
                                String memberName = member.getString("playerName");
                                Player onlineMember = Bukkit.getPlayerExact(memberName);
                                if (onlineMember != null && onlineMember.isOnline() && !memberName.equals(playerName)) {
                                    onlineMember.sendMessage("§a" + playerName + " joined the group " + groupName + " (via web)");
                                }
                            }
                        }

                        Map<String, Object> response = Map.of("success", true, "message", "Joined group successfully");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        if (group != null) {
                            List<Document> members = group.getList("members", Document.class);
                            boolean alreadyMember = members.stream()
                                    .anyMatch(member -> member.getString("playerId").equals(playerUUID));
                            
                            if (alreadyMember) {
                                Map<String, Object> response = Map.of(
                                    "success", false, 
                                    "error", "ALREADY_MEMBER",
                                    "message", "You are already a member of this group"
                                );
                                sendJsonResponse(exchange, response, 400);
                                return;
                            }
                            
                            int maxMembers = group.getInteger("maxMembers", 25);
                            if (members.size() >= maxMembers) {
                                Map<String, Object> response = Map.of(
                                    "success", false, 
                                    "error", "GROUP_FULL",
                                    "message", "This group is full"
                                );
                                sendJsonResponse(exchange, response, 400);
                                return;
                            }
                        }
                        
                        sendErrorResponse(exchange, "Failed to join group", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class LeaveGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String playerUUID = json.get("playerUUID").getAsString();
                    String groupId = json.get("groupId").getAsString();

                    boolean success = groupManager.leaveGroup(
                        UUID.fromString(groupId), UUID.fromString(playerUUID)
                    );

                    Map<String, Object> response = Map.of("success", success);
                    sendJsonResponse(exchange, response, success ? 200 : 400);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class DeleteGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String groupId = json.get("groupId").getAsString();
                    String ownerUUID = json.get("ownerUUID").getAsString();

                    Document group = groupManager.getGroup(UUID.fromString(groupId));
                    if (group == null) {
                        sendErrorResponse(exchange, "Group not found", 404);
                        return;
                    }

                    String ownerId = group.getString("ownerId");
                    if (!ownerUUID.equals(ownerId)) {
                        sendErrorResponse(exchange, "Only the group owner can delete the group", 403);
                        return;
                    }

                    boolean success = groupManager.deleteGroup(UUID.fromString(groupId), UUID.fromString(ownerUUID));

                    if (success) {
                        List<Document> members = group.getList("members", Document.class);
                        String groupName = group.getString("groupName");
                        String ownerName = getPlayerNameByUUID(UUID.fromString(ownerUUID));
                        
                        for (Document member : members) {
                            String memberName = member.getString("playerName");
                            Player onlineMember = Bukkit.getPlayerExact(memberName);
                            if (onlineMember != null && onlineMember.isOnline() && !memberName.equals(ownerName)) {
                                onlineMember.sendMessage("§cThe group '" + groupName + "' has been deleted by " + ownerName);
                            }
                        }

                        Map<String, Object> response = Map.of("success", true, "message", "Group deleted successfully");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to delete group", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class FriendRequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    List<Document> pendingRequests = friendManager.getPendingRequests(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("requests", pendingRequests);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    List<Document> allPlayers = userDataManager.getAllPlayers();
                    
                    Map<String, Object> response = Map.of(
                        "players", allPlayers,
                        "totalPlayers", allPlayers.size(),
                        "onlinePlayers", (int) allPlayers.stream().filter(p -> 
                            p.getBoolean("online", false)).count()
                    );
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class SearchPlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String searchQuery = getQueryParam(query, "query");
                    String limitParam = getQueryParam(query, "limit");

                    if (searchQuery == null) {
                        sendErrorResponse(exchange, "Query parameter is required", 400);
                        return;
                    }

                    List<Document> players = userDataManager.searchPlayersByName(searchQuery);
                    
                    if (limitParam != null) {
                        try {
                            int limit = Integer.parseInt(limitParam);
                            if (players.size() > limit) {
                                players = players.subList(0, limit);
                            }
                        } catch (NumberFormatException e) {
                        }
                    }

                    Map<String, Object> response = Map.of("players", players);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class IncomingFriendRequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    List<Document> requests = friendManager.getIncomingFriendRequests(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("requests", requests);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class OutgoingFriendRequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    List<Document> requests = friendManager.getOutgoingFriendRequests(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("requests", requests);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class FriendStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    Document stats = friendManager.getFriendStats(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("stats", stats);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class CancelFriendRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderUUID = json.get("senderUUID").getAsString();
                    String targetUUID = json.get("targetUUID").getAsString();

                    boolean success = friendManager.cancelFriendRequest(UUID.fromString(senderUUID), UUID.fromString(targetUUID));

                    Map<String, Object> response = Map.of("success", success);
                    sendJsonResponse(exchange, response, success ? 200 : 400);

                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to cancel friend request: " + e.getMessage());
                    e.printStackTrace();
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class GroupMembersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String groupId = getQueryParam(query, "groupId");

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID required", 400);
                        return;
                    }

                    Document group = groupManager.getGroup(UUID.fromString(groupId));
                    if (group == null) {
                        sendErrorResponse(exchange, "Group not found", 404);
                        return;
                    }

                    List<Document> members = group.getList("members", Document.class);

                    members.forEach(member -> {
                        String memberName = member.getString("playerName");
                        Player onlineMember = Bukkit.getPlayerExact(memberName);
                        member.append("online", onlineMember != null && onlineMember.isOnline());

                        if (onlineMember != null && onlineMember.isOnline()) {
                            String cleanRank = plugin.getRankManager().getCleanRank(onlineMember);
                            String formattedRank = plugin.getRankManager().getFormattedRank(onlineMember);
                            member.append("rank", cleanRank);
                            member.append("formattedRank", formattedRank);
                        }
                    });

                    Map<String, Object> response = Map.of("members", members);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class LegacyKickMemberHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String groupId = json.get("groupId").getAsString();
                    String adminUUID = json.get("adminUUID").getAsString();
                    String targetUUID = json.get("targetUUID").getAsString();
                    String reason = json.has("reason") ? json.get("reason").getAsString() : "No reason provided";

                    Document group = groupManager.getGroup(UUID.fromString(groupId));
                    if (group == null) {
                        sendErrorResponse(exchange, "Group not found", 404);
                        return;
                    }

                    boolean isAdmin = groupManager.isAdminOrOwner(UUID.fromString(groupId), UUID.fromString(adminUUID));
                    if (!isAdmin) {
                        sendErrorResponse(exchange, "Insufficient permissions", 403);
                        return;
                    }

                    boolean success = groupManager.kickMember(
                        UUID.fromString(groupId),
                        UUID.fromString(adminUUID),
                        UUID.fromString(targetUUID),
                        reason
                    );

                    if (success) {
                        String adminName = getPlayerNameByUUID(UUID.fromString(adminUUID));
                        String targetName = getPlayerNameByUUID(UUID.fromString(targetUUID));
                        String groupName = group.getString("groupName");

                        List<Document> members = group.getList("members", Document.class);
                        for (Document member : members) {
                            String memberName = member.getString("playerName");
                            Player onlineMember = Bukkit.getPlayerExact(memberName);
                            if (onlineMember != null && onlineMember.isOnline()) {
                                onlineMember.sendMessage("§e" + targetName + " was kicked from " + groupName + " by " + adminName + " (via web)");
                            }
                        }

                        Player kickedPlayer = Bukkit.getPlayerExact(targetName);
                        if (kickedPlayer != null && kickedPlayer.isOnline()) {
                            kickedPlayer.sendMessage("§cYou were kicked from " + groupName + " by " + adminName + ". Reason: " + reason);
                        }

                        Map<String, Object> response = Map.of("success", true, "message", "Member kicked successfully");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to kick member", 400);
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class GroupMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String groupId = getQueryParam(query, "groupId");
                    String limit = getQueryParam(query, "limit");

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID required", 400);
                        return;
                    }

                    int messageLimit = limit != null ? Integer.parseInt(limit) : 50;
                    List<Document> messages = groupManager.getGroupMessages(UUID.fromString(groupId), messageLimit);

                    Map<String, Object> response = Map.of("messages", messages);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class SendGroupMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String senderId = json.get("senderId").getAsString();
                    String senderName = json.get("senderName").getAsString();
                    String message = json.get("message").getAsString();
                    String groupId = json.get("groupId").getAsString();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        if (group != null) {
                            String groupName = group.getString("groupName");
                            String format = plugin.getConfig().getString("chat-groups.format",
                                "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
                            String formattedMessage = format
                                    .replace("{group}", groupName)
                                    .replace("{player}", senderName + " (Web)")
                                    .replace("{message}", message);

                            List<Document> members = group.getList("members", Document.class);
                            for (Document member : members) {
                                String memberName = member.getString("playerName");
                                Player onlineMember = Bukkit.getPlayerExact(memberName);
                                if (onlineMember != null && onlineMember.isOnline()) {
                                    onlineMember.sendMessage(formattedMessage.replace("&", "§"));
                                }
                            }
                        }
                    });

                    Map<String, Object> response = Map.of("success", true);
                    sendJsonResponse(exchange, response, 200);

                } catch (Exception e) {
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private class GroupStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String groupId = getQueryParam(query, "groupId");

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID required", 400);
                        return;
                    }

                    UUID groupUUID = UUID.fromString(groupId);
                    Document group = groupManager.getGroup(groupUUID);

                    if (group == null) {
                        sendErrorResponse(exchange, "Group not found", 404);
                        return;
                    }

                    List<Document> members = group.getList("members", Document.class);
                    List<Document> messages = groupManager.getGroupMessages(groupUUID, 1000); // Get last 1000 messages for stats

                    int totalMembers = members.size();
                    int onlineMembers = 0;
                    int admins = 0;
                    int moderators = 0;

                    for (Document member : members) {
                        String memberName = member.getString("playerName");
                        String role = member.getString("role");

                        Player onlinePlayer = Bukkit.getPlayerExact(memberName);
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            onlineMembers++;
                        }

                        if ("ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role)) {
                            admins++;
                        } else if ("MODERATOR".equalsIgnoreCase(role)) {
                            moderators++;
                        }
                    }

                    int totalMessages = messages.size();
                    long todayMessages = messages.stream()
                        .mapToLong(msg -> {
                            try {
                                Object timestamp = msg.get("timestamp");
                                if (timestamp instanceof String) {
                                    return Long.parseLong((String) timestamp);
                                } else if (timestamp instanceof Long) {
                                    return (Long) timestamp;
                                } else if (timestamp instanceof Number) {
                                    return ((Number) timestamp).longValue();
                                }
                                return 0L; 
                            } catch (Exception e) {
                                return 0L; 
                            }
                        })
                        .filter(timestamp -> {
                            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                            return timestamp > oneDayAgo;
                        })
                        .count();

                    long weekMessages = messages.stream()
                        .mapToLong(msg -> {
                            try {
                                Object timestamp = msg.get("timestamp");
                                if (timestamp instanceof String) {
                                    return Long.parseLong((String) timestamp);
                                } else if (timestamp instanceof Long) {
                                    return (Long) timestamp;
                                } else if (timestamp instanceof Number) {
                                    return ((Number) timestamp).longValue();
                                }
                                return 0L; 
                            } catch (Exception e) {
                                return 0L;
                            }
                        })
                        .filter(timestamp -> {
                            long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                            return timestamp > oneWeekAgo;
                        })
                        .count();

                    Map<String, Long> memberMessageCounts = new HashMap<>();
                    for (Document message : messages) {
                        String sender = message.getString("senderName");
                        memberMessageCounts.put(sender, memberMessageCounts.getOrDefault(sender, 0L) + 1);
                    }

                    String mostActiveMember = memberMessageCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("N/A");

                    long mostActiveCount = memberMessageCounts.getOrDefault(mostActiveMember, 0L);

                    long groupAge = System.currentTimeMillis() - group.getLong("createdDate");
                    double daysSinceCreation = Math.max(1, groupAge / (24.0 * 60 * 60 * 1000));
                    double activityScore = totalMessages / daysSinceCreation;

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("groupId", groupId);
                    stats.put("groupName", group.getString("groupName"));
                    stats.put("description", group.getString("description"));
                    stats.put("createdDate", group.getLong("createdDate"));
                    stats.put("isPrivate", group.getBoolean("isPrivate", false));
                    stats.put("maxMembers", group.getInteger("maxMembers", 25));

                    Map<String, Object> memberStats = new HashMap<>();
                    memberStats.put("total", totalMembers);
                    memberStats.put("online", onlineMembers);
                    memberStats.put("offline", totalMembers - onlineMembers);
                    memberStats.put("admins", admins);
                    memberStats.put("moderators", moderators);
                    memberStats.put("members", totalMembers - admins - moderators);
                    stats.put("memberStats", memberStats);

                    Map<String, Object> messageStats = new HashMap<>();
                    messageStats.put("total", totalMessages);
                    messageStats.put("today", todayMessages);
                    messageStats.put("thisWeek", weekMessages);
                    messageStats.put("averagePerDay", Math.round(activityScore * 100.0) / 100.0);
                    stats.put("messageStats", messageStats);

                    Map<String, Object> activityStats = new HashMap<>();
                    activityStats.put("mostActiveMember", mostActiveMember);
                    activityStats.put("mostActiveMessageCount", mostActiveCount);
                    activityStats.put("activityScore", Math.round(activityScore * 100.0) / 100.0);
                    activityStats.put("daysSinceCreation", Math.round(daysSinceCreation * 100.0) / 100.0);
                    stats.put("activityStats", activityStats);

                    Map<String, Object> ownerInfo = new HashMap<>();
                    ownerInfo.put("name", group.getString("ownerName"));
                    ownerInfo.put("uuid", group.getString("ownerId"));

                    Player ownerPlayer = Bukkit.getPlayerExact(group.getString("ownerName"));
                    ownerInfo.put("online", ownerPlayer != null && ownerPlayer.isOnline());

                    if (ownerPlayer != null && ownerPlayer.isOnline()) {
                        ownerInfo.put("rank", plugin.getRankManager().getCleanRank(ownerPlayer));
                        ownerInfo.put("formattedRank", plugin.getRankManager().getFormattedRank(ownerPlayer));
                    }
                    stats.put("owner", ownerInfo);

                    Map<String, Object> response = Map.of("stats", stats);
                    sendJsonResponse(exchange, response, 200);

                } catch (IllegalArgumentException e) {
                    sendErrorResponse(exchange, "Invalid group ID format", 400);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error getting group stats: " + e.getMessage());
                    e.printStackTrace();
                    sendErrorResponse(exchange, "Internal server error", 500);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendJsonResponse(HttpExchange exchange, Object data, int statusCode) throws IOException {
        String jsonResponse = gson.toJson(data);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.close();
    }

    private void sendErrorResponse(HttpExchange exchange, String error, int statusCode) throws IOException {
        Map<String, String> errorResponse = Map.of("error", error);
        sendJsonResponse(exchange, errorResponse, statusCode);
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    private UUID getPlayerUUID(String playerName) {
        UUID uuid = userDataManager.getPlayerUUIDByName(playerName);
        return uuid != null ? uuid : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
    }

    private String getPlayerNameByUUID(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        String playerName = userDataManager.getPlayerNameByUUID(playerUUID);
        return playerName != null ? playerName : "Unknown Player";
    }

    private class UpdateGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                    String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
                    String groupName = json.has("groupName") && !json.get("groupName").isJsonNull() ? json.get("groupName").getAsString() : null;
                    String description = json.has("description") && !json.get("description").isJsonNull() ? json.get("description").getAsString() : null;
                    Integer maxMembers = null;
                    if (json.has("maxMembers") && !json.get("maxMembers").isJsonNull()) {
                        try {
                            maxMembers = json.get("maxMembers").getAsInt();
                        } catch (Exception e) {
                            try { maxMembers = (int) json.get("maxMembers").getAsDouble(); } catch (Exception ignored) {}
                        }
                    }
                    Boolean isPrivate = json.has("isPrivate") && !json.get("isPrivate").isJsonNull() ? json.get("isPrivate").getAsBoolean() : null;
                    String motd = json.has("motd") && !json.get("motd").isJsonNull() ? json.get("motd").getAsString() : null;

                    List<String> announcements = null;
                    if (json.has("announcements") && !json.get("announcements").isJsonNull()) {
                        announcements = new ArrayList<>();
                        JsonElement annElem = json.get("announcements");
                        if (annElem.isJsonArray()) {
                            for (JsonElement elem : annElem.getAsJsonArray()) {
                                if (!elem.isJsonNull()) announcements.add(elem.getAsString());
                            }
                        }
                    }

                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID is required", 400);
                        return;
                    }

                    UUID groupUUID = UUID.fromString(groupId);

                    boolean success = groupManager.updateGroupInfo(
                        groupUUID,
                        groupName,
                        description,
                        maxMembers,
                        isPrivate
                    );

                    if (motd != null && success) {
                        success = groupManager.updateGroupMotd(groupUUID, motd);
                    }

                    if (announcements != null && success) {
                        success = groupManager.updateGroupAnnouncements(groupUUID, announcements);
                    }

                    if (success) {
                        Map<String, Object> response = Map.of("success", true, "message", "Group updated successfully");
                        sendJsonResponse(exchange, response, 200);
                    } else {
                        sendErrorResponse(exchange, "Failed to update group", 500);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    sendErrorResponse(exchange, "Invalid request format", 400);
                }
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        }
    }
}
