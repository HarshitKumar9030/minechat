package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.api.GroupMember;
import me.harshit.minechat.database.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bson.Document;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SendGroupMessageHandler implements HttpHandler {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public SendGroupMessageHandler(Minechat plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleSendGroupMessage(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in SendGroupMessageHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleSendGroupMessage(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.get("groupId").getAsString();
            String senderId = json.get("senderId").getAsString();
            String senderName = json.get("senderName").getAsString();
            String message = json.get("message").getAsString();

            if (groupId == null || senderId == null || senderName == null || message == null || message.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "Missing required parameters");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            GroupInfo group = groupManager.getGroupById(groupUUID);

            if (group == null) {
                sendErrorResponse(exchange, 404, "Group not found");
                return;
            }

            UUID senderUUID = UUID.fromString(senderId);

            GroupMember senderMember = groupManager.getGroupMember(groupUUID, senderUUID);
            if (senderMember == null) {
                sendErrorResponse(exchange, 403, "You are not a member of this group");
                return;
            }

            if (group.getSettings().isPlayerMuted(senderUUID)) {
                sendErrorResponse(exchange, 403, "You are muted in this group");
                return;
            }

            groupManager.storeGroupMessage(groupUUID, senderUUID, senderName, message, "web");

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    String format = plugin.getConfig().getString("chat-groups.format",
                        "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
                    String formattedMessage = format
                            .replace("{group}", group.getGroupName())
                            .replace("{player}", senderName + " (Web)")
                            .replace("{message}", message);

                    Component messageComponent = Component.text(formattedMessage.replace("&", "§"));

                    List<GroupMember> members = group.getMembers();
                    for (GroupMember member : members) {
                        Player onlineMember = Bukkit.getPlayer(member.getPlayerId());
                        if (onlineMember != null && onlineMember.isOnline()) {
                            onlineMember.sendMessage(messageComponent);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error sending group message to online players: " + e.getMessage());
                }
            });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent successfully");
            response.put("groupId", groupId);
            response.put("groupName", group.getGroupName());
            response.put("timestamp", System.currentTimeMillis());

            sendJsonResponse(exchange, 200, response);

        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Invalid UUID format");
        } catch (Exception e) {
            plugin.getLogger().severe("Error sending group message: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        String responseString = gson.toJson(data);
        byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("success", false);

        String response = gson.toJson(error);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
