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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.bson.Document;

public class KickMemberHandler implements HttpHandler {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public KickMemberHandler(Minechat plugin) {
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
                handleKickMember(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in KickMemberHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleKickMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.get("groupId").getAsString();
            String adminUUID = json.get("adminUUID").getAsString();
            String targetUUID = json.get("targetUUID").getAsString();
            String reason = json.has("reason") ? json.get("reason").getAsString() : "No reason provided";

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            Document groupDoc = groupManager.getGroupById(groupUUID);
            if (groupDoc == null) {
                sendErrorResponse(exchange, 404, "Group not found");
                return;
            }

            GroupInfo group = GroupInfo.fromDocument(groupDoc);
            if (group == null) {
                sendErrorResponse(exchange, 500, "Failed to parse group data");
                return;
            }

            // verify admin has permission to kick
            GroupMember adminMember = groupManager.getGroupMember(groupUUID, adminId);
            if (adminMember == null || !adminMember.getRole().canModerate()) {
                sendErrorResponse(exchange, 403, "Insufficient permissions to kick members");
                return;
            }

            GroupMember targetMember = groupManager.getGroupMember(groupUUID, targetId);
            if (targetMember == null) {
                sendErrorResponse(exchange, 404, "Target player is not a member of this group");
                return;
            }

            // check if admin can kick this member (can't kick higher or equal rank)
            if (targetMember.getRole().getPriority() >= adminMember.getRole().getPriority()) {
                sendErrorResponse(exchange, 403, "Cannot kick someone with equal or higher permissions");
                return;
            }

            // perform the kick
            boolean success = groupManager.kickMember(groupUUID, targetId, adminId, reason);

            if (success) {
                // Notify online group members
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String adminName = getPlayerNameByUUID(adminId);
                    String targetName = getPlayerNameByUUID(targetId);
                    String groupName = group.getGroupName();

                    List<GroupMember> members = group.getMembers();
                    for (GroupMember member : members) {
                        Player onlineMember = Bukkit.getPlayer(member.getPlayerId());
                        if (onlineMember != null && onlineMember.isOnline()) {
                            onlineMember.sendMessage("§e" + targetName + " was kicked from " + groupName + " by " + adminName + " (via web)");
                        }
                    }

                    // notify kicked player if online
                    Player kickedPlayer = Bukkit.getPlayer(targetId);
                    if (kickedPlayer != null && kickedPlayer.isOnline()) {
                        kickedPlayer.sendMessage("§cYou were kicked from " + groupName + " by " + adminName + ". Reason: " + reason);
                    }
                });

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Member kicked successfully");
                response.addProperty("kickedPlayer", getPlayerNameByUUID(targetId));
                response.addProperty("reason", reason);

                sendJsonResponse(exchange, 200, response);
            } else {
                sendErrorResponse(exchange, 500, "Failed to kick member");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling kick member request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private String getPlayerNameByUUID(UUID playerUUID) {
        // try to get from online players first
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }

        String playerName = plugin.getUserDataManager().getPlayerNameByUUID(playerUUID);
        return playerName != null ? playerName : "Unknown Player";
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        String responseString = gson.toJson(response);
        byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("success", false);

        String response = gson.toJson(error);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
