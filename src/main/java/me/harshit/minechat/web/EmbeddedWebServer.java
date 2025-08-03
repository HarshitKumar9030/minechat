package me.harshit.minechat.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.database.UserDataManager;
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
import java.util.UUID;
import java.util.concurrent.Executors;

// todo: add routes to manage friends and groups from the web app
public class EmbeddedWebServer {

    private final Minechat plugin;
    private final UserDataManager userDataManager;
    private final FriendManager friendManager;
    private final GroupManager groupManager;
    private final Gson gson;
    private HttpServer server;
    private final int port;

    public EmbeddedWebServer(Minechat plugin, UserDataManager userDataManager,
                           FriendManager friendManager, GroupManager groupManager) {
        this.plugin = plugin;
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

            // cors is imp
            server.createContext("/", new CORSHandler());

            // Basic API endpoints
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/friends", new FriendsHandler());
            server.createContext("/api/groups", new GroupsHandler());
            server.createContext("/api/messages", new MessagesHandler());
            server.createContext("/api/send-message", new SendMessageHandler());
            server.createContext("/api/send-friend-request", new SendFriendRequestHandler());
            server.createContext("/api/accept-friend-request", new AcceptFriendRequestHandler());
            server.createContext("/api/reject-friend-request", new RejectFriendRequestHandler());
            server.createContext("/api/remove-friend", new RemoveFriendHandler());
            server.createContext("/api/create-group", new CreateGroupHandler());
            server.createContext("/api/delete-group", new DeleteGroupHandler());
            server.createContext("/api/join-group", new JoinGroupHandler());
            server.createContext("/api/leave-group", new LeaveGroupHandler());
            server.createContext("/api/ranks", new RanksHandler());
            server.createContext("/api/users", new UsersHandler());
            server.createContext("/api/friend-requests", new FriendRequestsHandler());

            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/search-players", new SearchPlayersHandler());
            server.createContext("/api/friend-requests/incoming", new IncomingFriendRequestsHandler());
            server.createContext("/api/friend-requests/outgoing", new OutgoingFriendRequestsHandler());
            server.createContext("/api/friend-stats", new FriendStatsHandler());
            server.createContext("/api/cancel-friend-request", new CancelFriendRequestHandler());

            // core group management endpoints
            server.createContext("/api/group-members", new me.harshit.minechat.web.GroupMembersHandler(plugin));
            server.createContext("/api/kick-member", new me.harshit.minechat.web.KickMemberHandler(plugin));
            server.createContext("/api/group-messages", new me.harshit.minechat.web.GroupMessagesHandler(plugin));
            server.createContext("/api/send-group-message", new me.harshit.minechat.web.SendGroupMessageHandler(plugin));
            server.createContext("/api/search-groups", new me.harshit.minechat.web.SearchGroupsHandler(plugin));

            // essential group operations (using existing handlers with proper logic)
            server.createContext("/api/public-groups", new GroupsHandler());
            server.createContext("/api/trending-groups", new GroupsHandler());
            server.createContext("/api/recommended-groups", new GroupsHandler());
            server.createContext("/api/group-details", new GroupsHandler());
            server.createContext("/api/join-group-by-code", new JoinGroupHandler());

            // member management (using KickMemberHandler as base for similar operations)
            server.createContext("/api/ban-member", new me.harshit.minechat.web.KickMemberHandler(plugin));
            server.createContext("/api/promote-member", new me.harshit.minechat.web.KickMemberHandler(plugin));
            server.createContext("/api/mute-member", new me.harshit.minechat.web.KickMemberHandler(plugin));

            // group invitations (reusing friend request logic where applicable)
            server.createContext("/api/group-invites", new FriendRequestsHandler());
            server.createContext("/api/send-group-invite", new SendFriendRequestHandler());
            server.createContext("/api/accept-group-invite", new AcceptFriendRequestHandler());

            server.createContext("/api/group-settings", new GroupsHandler());
            server.createContext("/api/update-group", new UpdateGroupHandler());
            server.createContext("/api/update-group-settings", new CreateGroupHandler());
            server.createContext("/api/group-invite-code", new GroupsHandler());
            server.createContext("/api/group-stats", new GroupStatsHandler());

            // Test endpoint
            server.createContext("/api/test", new TestHandler());

            server.start();
            plugin.getLogger().info("Web server started successfully on port " + port);
            plugin.getLogger().info("Access web API at: http://localhost:" + port);

            // This is for someone who is running this plugin on their server
            // Either you can use a reverse proxy like nginx to forward this port to a domain then connect it with the web interface
            // or you can run the web interface in the same machine and just connect to "localhost:8080"
            // I'd say go with the reverse proxy if ur running a big server, else avoid hassle and just use localhost

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start web server on port " + port + ": " + e.getMessage());
            plugin.getLogger().severe("Make sure port " + port + " is not in use by another application");
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error starting web server: " + e.getMessage());
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

                    // Add online status
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
                // For now, return the most active public groups,  todo: implement trending logic
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

                    // Store message in database
                    groupManager.storeGroupMessage(UUID.fromString(groupId), UUID.fromString(senderId),
                                                 senderName, message, "web");

                    // Send to online players in the group
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

                    // Check if target player exists in database
                    if (!userDataManager.playerExists(targetName)) {
                        sendErrorResponse(exchange, "Player not found", 404);
                        return;
                    }

                    UUID targetUUID = userDataManager.getPlayerUUIDByName(targetName);
                    boolean success = friendManager.sendFriendRequest(
                        UUID.fromString(senderUUID), senderName, targetUUID, targetName
                    );

                    if (success) {
                        // Notify online target player if they're online
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
                        // Get requester name and notify if online
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
                    String description = json.has("description") ? json.get("description").getAsString() : "";
                    int maxMembers = json.has("maxMembers") ? json.get("maxMembers").getAsInt() : 20;
                    boolean isPrivate = json.has("isPrivate") ? json.get("isPrivate").getAsBoolean() : false;

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

                    // verify that the user is the owner of the group
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
                        // notify
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
                    
                    // apply limit if specified
                    if (limitParam != null) {
                        try {
                            int limit = Integer.parseInt(limitParam);
                            if (players.size() > limit) {
                                players = players.subList(0, limit);
                            }
                        } catch (NumberFormatException e) {
                            // ignore invalid limit, use all results
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

                    // Add online status and rank info for each member
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

    private class KickMemberHandler implements HttpHandler {
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

                    // Verify admin has permission to kick
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
                        // Notify online group members
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

                        // Notify kicked player if online
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

                    // Store message in database
                    groupManager.storeGroupMessage(UUID.fromString(groupId), UUID.fromString(senderId),
                                                 senderName, message, "web");

                    // Send to online players in the group
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
                                return 0L; // default fallback
                            } catch (Exception e) {
                                return 0L; // return 0 for invalid timestamps
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
                                // handle both string and long timestamp formats
                                Object timestamp = msg.get("timestamp");
                                if (timestamp instanceof String) {
                                    return Long.parseLong((String) timestamp);
                                } else if (timestamp instanceof Long) {
                                    return (Long) timestamp;
                                } else if (timestamp instanceof Number) {
                                    return ((Number) timestamp).longValue();
                                }
                                return 0L; // default fallback
                            } catch (Exception e) {
                                return 0L; // return 0 for invalid timestamps
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
        // try to get from online players first
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        // search in db
        String playerName = userDataManager.getPlayerNameByUUID(playerUUID);
        return playerName != null ? playerName : "Unknown Player";
    }

    private class UpdateGroupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            new CORSHandler().handle(exchange);

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream inputStream = exchange.getRequestBody();
                    String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    
                    Map<String, Object> requestData = gson.fromJson(requestBody, Map.class);
                    
                    String groupId = (String) requestData.get("groupId");
                    String groupName = (String) requestData.get("groupName");
                    String description = (String) requestData.get("description");
                    Double maxMembersDouble = (Double) requestData.get("maxMembers");
                    Boolean isPrivate = (Boolean) requestData.get("isPrivate");
                    String motd = (String) requestData.get("motd");
                    List<String> announcements = (List<String>) requestData.get("announcements");
                    
                    if (groupId == null) {
                        sendErrorResponse(exchange, "Group ID is required", 400);
                        return;
                    }
                    
                    UUID groupUUID = UUID.fromString(groupId);
                    
                    boolean success = groupManager.updateGroupInfo(
                        groupUUID, 
                        groupName, 
                        description, 
                        maxMembersDouble != null ? maxMembersDouble.intValue() : null,
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
