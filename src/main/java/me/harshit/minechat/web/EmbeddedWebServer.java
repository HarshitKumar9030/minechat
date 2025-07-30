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
import java.security.MessageDigest;
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
            // todo: make more friends and groups endpoints

            // nothing more than just a test endpoint
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
            e.printStackTrace();
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

                    // Verify credentials
                    boolean isValid = userDataManager.verifyWebPassword(username, password);

                    Map<String, Object> response = new HashMap<>();
                    if (isValid) {
                        Player player = Bukkit.getPlayerExact(username);
                        UUID playerUUID = player != null ? player.getUniqueId() : getPlayerUUID(username);

                        response.put("success", true);
                        response.put("user", Map.of(
                            "playerUUID", playerUUID.toString(),
                            "playerName", username,
                            "webAccessEnabled", true
                        ));
                        response.put("sessionToken", UUID.randomUUID().toString());
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

    // Helper methods
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
        Player player = Bukkit.getPlayerExact(playerName);
        return player != null ? player.getUniqueId() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
    }
}
