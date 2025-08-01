package me.harshit.minechat.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.harshit.minechat.Minechat;
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

            // all api endpoints
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

            // nothing just a test endpoint
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
                    String query = exchange.getRequestURI().getQuery();
                    String playerUUID = getQueryParam(query, "playerUUID");

                    if (playerUUID == null) {
                        sendErrorResponse(exchange, "Player UUID required", 400);
                        return;
                    }

                    List<Document> groups = groupManager.getPlayerGroups(UUID.fromString(playerUUID));
                    Map<String, Object> response = Map.of("groups", groups);
                    sendJsonResponse(exchange, response, 200);

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
                    boolean isPrivate = json.has("isPrivate") ? json.get("isPrivate").getAsBoolean() : false;

                    boolean success = groupManager.createGroup(
                        UUID.fromString(creatorUUID), creatorName, groupName, description, isPrivate
                    );

                    if (success) {
                        Map<String, Object> response = Map.of(
                            "success", true,
                            "message", "Group created successfully"
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
}
