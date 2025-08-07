package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.GroupManager;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupInvitesHandler implements HttpHandler {
    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GroupInvitesHandler(Minechat plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetGroupInvites(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in GroupInvitesHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleGetGroupInvites(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("playerUUID=")) {
                sendErrorResponse(exchange, 400, "Missing playerUUID parameter");
                return;
            }

            String playerUUIDStr = extractPlayerUUID(query);
            if (playerUUIDStr == null) {
                sendErrorResponse(exchange, 400, "Invalid playerUUID parameter");
                return;
            }

            UUID playerUUID = UUID.fromString(playerUUIDStr);
            List<Document> invites = groupManager.getGroupInvites(playerUUID);

            JsonObject response = new JsonObject();
            JsonArray invitesArray = new JsonArray();

            for (Document invite : invites) {
                JsonObject inviteObj = new JsonObject();
                inviteObj.addProperty("inviteId", invite.getString("inviteId"));
                inviteObj.addProperty("groupId", invite.getString("groupId"));
                inviteObj.addProperty("groupName", invite.getString("groupName"));
                inviteObj.addProperty("inviterUUID", invite.getString("inviterUUID"));
                inviteObj.addProperty("inviterName", invite.getString("inviterName"));
                inviteObj.addProperty("inviteeUUID", invite.getString("inviteeUUID"));
                inviteObj.addProperty("inviteeName", invite.getString("inviteeName"));
                inviteObj.addProperty("timestamp", invite.getLong("timestamp"));
                inviteObj.addProperty("status", invite.getString("status"));
                
                if (invite.containsKey("message")) {
                    inviteObj.addProperty("message", invite.getString("message"));
                }

                invitesArray.add(inviteObj);
            }

            response.add("invites", invitesArray);
            sendSuccessResponse(exchange, response);

        } catch (Exception e) {
            plugin.getLogger().severe("Error getting group invites: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get group invites");
        }
    }

    private String extractPlayerUUID(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("playerUUID=")) {
                return param.substring("playerUUID=".length());
            }
        }
        return null;
    }

    private void sendSuccessResponse(HttpExchange exchange, JsonObject data) throws IOException {
        String response = gson.toJson(data);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("success", false);
        
        String response = gson.toJson(error);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
