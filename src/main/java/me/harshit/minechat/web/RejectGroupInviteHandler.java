package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.GroupManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RejectGroupInviteHandler implements HttpHandler {
    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public RejectGroupInviteHandler(Minechat plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleRejectGroupInvite(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in RejectGroupInviteHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleRejectGroupInvite(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JsonObject json = gson.fromJson(requestBody, JsonObject.class);

            if (!json.has("inviteId") || !json.has("playerUUID")) {
                sendErrorResponse(exchange, 400, "Missing inviteId or playerUUID");
                return;
            }

            String inviteId = json.get("inviteId").getAsString();
            String playerUUIDStr = json.get("playerUUID").getAsString();
            UUID playerUUID = UUID.fromString(playerUUIDStr);

            boolean success = groupManager.rejectGroupInviteById(inviteId, playerUUID);

            JsonObject response = new JsonObject();
            if (success) {
                response.addProperty("success", true);
                response.addProperty("message", "Group invite rejected successfully");
                sendSuccessResponse(exchange, response);
            } else {
                sendErrorResponse(exchange, 400, "Failed to reject group invite. Invite may be invalid or already processed.");
            }

        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Invalid UUID format");
        } catch (Exception e) {
            plugin.getLogger().severe("Error rejecting group invite: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to reject group invite");
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
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
