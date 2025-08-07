package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.GroupManager;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddAnnouncementHandler implements HttpHandler {
    
    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;
    
    public AddAnnouncementHandler(Minechat plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.gson = new Gson();
    }
    
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

        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                String requestBody = readRequestBody(exchange);
                JsonObject json = gson.fromJson(requestBody, JsonObject.class);

                String groupId = json.get("groupId").getAsString();
                String announcement = json.get("announcement").getAsString();
                String adminUUID = json.get("adminUUID").getAsString();

                if (groupId == null || announcement == null || adminUUID == null) {
                    sendErrorResponse(exchange, "Missing required fields", 400);
                    return;
                }

                if (announcement.trim().isEmpty()) {
                    sendErrorResponse(exchange, "Announcement cannot be empty", 400);
                    return;
                }

                UUID groupUUID = UUID.fromString(groupId);
                UUID adminUserUUID = UUID.fromString(adminUUID);

                if (!groupManager.isAdminOrOwner(groupUUID, adminUserUUID)) {
                    sendErrorResponse(exchange, "Permission denied", 403);
                    return;
                }

                Document group = groupManager.getGroup(groupUUID);
                if (group == null) {
                    sendErrorResponse(exchange, "Group not found", 404);
                    return;
                }

                Document settings = group.get("settings", Document.class);
                List<String> announcements = new ArrayList<>();
                
                if (settings != null) {
                    List<String> existing = settings.getList("announcements", String.class);
                    if (existing != null) {
                        announcements.addAll(existing);
                    }
                }

                announcements.add(announcement.trim());

                boolean success = groupManager.updateGroupAnnouncements(groupUUID, announcements);

                if (success) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Announcement added successfully");
                    response.put("announcements", announcements);
                    sendJsonResponse(exchange, response, 200);
                } else {
                    sendErrorResponse(exchange, "Failed to add announcement", 500);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error adding announcement: " + e.getMessage());
                sendErrorResponse(exchange, "Internal server error", 500);
            }
        } else {
            sendErrorResponse(exchange, "Method not allowed", 405);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        String jsonResponse = gson.toJson(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        sendJsonResponse(exchange, errorResponse, statusCode);
    }
}
