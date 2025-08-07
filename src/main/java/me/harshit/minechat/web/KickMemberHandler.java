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
import java.util.ArrayList;
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
                String path = exchange.getRequestURI().getPath();
                
                switch (path) {
                    case "/api/kick-member":
                        handleKickMember(exchange);
                        break;
                    case "/api/ban-member":
                        handleBanMember(exchange);
                        break;
                    case "/api/mute-member":
                        handleMuteMember(exchange);
                        break;
                    case "/api/unmute-member":
                        handleUnmuteMember(exchange);
                        break;
                    case "/api/promote-member":
                        handlePromoteMember(exchange);
                        break;
                    case "/api/demote-member":
                        handleDemoteMember(exchange);
                        break;
                    case "/api/update-group-motd":
                        handleUpdateMOTD(exchange);
                        break;
                    case "/api/add-announcement":
                        handleAddAnnouncement(exchange);
                        break;
                    case "/api/update-announcement":
                        handleUpdateAnnouncement(exchange);
                        break;
                    case "/api/remove-announcement":
                        handleRemoveAnnouncement(exchange);
                        break;
                    default:
                        sendErrorResponse(exchange, 404, "Endpoint not found");
                        break;
                }
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in ModerationHandler: " + e.getMessage());
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

            Document group = groupManager.getGroup(groupUUID);

            if (group == null) {
                sendErrorResponse(exchange, 404, "Group not found");
                return;
            }

            boolean isAdmin = groupManager.isAdminOrOwner(groupUUID, adminId);
            if (!isAdmin) {
                sendErrorResponse(exchange, 403, "Insufficient permissions to kick members");
                return;
            }

            List<Document> members = group.getList("members", Document.class);
            boolean targetIsMember = members.stream()
                .anyMatch(member -> member.getString("playerId").equals(targetId.toString()));

            if (!targetIsMember) {
                sendErrorResponse(exchange, 404, "Target player is not a member of this group");
                return;
            }

            boolean success = groupManager.kickMember(groupUUID, targetId, adminId, reason);

            if (success) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String adminName = getPlayerNameByUUID(adminId);
                    String targetName = getPlayerNameByUUID(targetId);
                    String groupName = group.getString("groupName");

                    List<Document> updatedMembers = group.getList("members", Document.class);
                    for (Document member : updatedMembers) {
                        String memberIdStr = member.getString("playerId");
                        if (memberIdStr != null) {
                            try {
                                UUID memberId = UUID.fromString(memberIdStr);
                                Player onlineMember = Bukkit.getPlayer(memberId);
                                if (onlineMember != null && onlineMember.isOnline()) {
                                    onlineMember.sendMessage("§e" + targetName + " was kicked from " + groupName + " by " + adminName + " (via web)");
                                }
                            } catch (IllegalArgumentException e) {
                            }
                        }
                    }

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
            plugin.getLogger().severe("Error kicking member: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private String getPlayerNameByUUID(UUID playerUUID) {
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

    private void handleBanMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String targetUUID = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;
            String reason = json.has("reason") && !json.get("reason").isJsonNull() ? json.get("reason").getAsString() : "No reason provided";

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, adminUUID, or targetUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            boolean success = groupManager.banMember(groupUUID, targetId, adminId, reason);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Member banned successfully" : "Failed to ban member");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error banning member: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleMuteMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String targetUUID = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;
            int duration = json.has("duration") && !json.get("duration").isJsonNull() ? json.get("duration").getAsInt() : 60; // default 60 minutes

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, adminUUID, or targetUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            boolean success = groupManager.muteMember(groupUUID, targetId, adminId, duration);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Member muted successfully" : "Failed to mute member");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error muting member: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleUnmuteMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String targetUUID = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, adminUUID, or targetUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            boolean success = groupManager.unmuteMember(groupUUID, targetId, adminId);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Member unmuted successfully" : "Failed to unmute member");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error unmuting member: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handlePromoteMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String targetUUID = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, adminUUID, or targetUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            boolean success = groupManager.promoteGroupMember(groupUUID, targetId);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Member promoted successfully" : "Failed to promote member");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error promoting member: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleDemoteMember(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String targetUUID = json.has("targetUUID") && !json.get("targetUUID").isJsonNull() ? json.get("targetUUID").getAsString() : null;

            if (groupId == null || adminUUID == null || targetUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, adminUUID, or targetUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);
            UUID targetId = UUID.fromString(targetUUID);

            boolean success = groupManager.demoteMember(groupUUID, targetId, adminId);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Member demoted successfully" : "Failed to demote member");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error demoting member: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleUpdateMOTD(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;
            String motd = json.has("motd") && !json.get("motd").isJsonNull() ? json.get("motd").getAsString() : null;

            if (groupId == null || motd == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId or motd");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminId = UUID.fromString(adminUUID);

            boolean success = groupManager.updateGroupMOTD(groupUUID, motd);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "MOTD updated successfully" : "Failed to update MOTD");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating MOTD: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleAddAnnouncement(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String announcement = json.has("announcement") && !json.get("announcement").isJsonNull() ? json.get("announcement").getAsString() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;

            if (groupId == null || announcement == null || adminUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, announcement, or adminUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminUserUUID = UUID.fromString(adminUUID);

            if (!groupManager.isAdminOrOwner(groupUUID, adminUserUUID)) {
                sendErrorResponse(exchange, 403, "Permission denied");
                return;
            }

            Document group = groupManager.getGroup(groupUUID);
            if (group == null) {
                sendErrorResponse(exchange, 404, "Group not found");
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

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Announcement added successfully" : "Failed to add announcement");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding announcement: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleUpdateAnnouncement(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            String announcement = json.has("announcement") && !json.get("announcement").isJsonNull() ? json.get("announcement").getAsString() : null;
            Integer index = json.has("index") && !json.get("index").isJsonNull() ? json.get("index").getAsInt() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;

            if (groupId == null || announcement == null || index == null || adminUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, announcement, index, or adminUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminUserUUID = UUID.fromString(adminUUID);

            if (!groupManager.isAdminOrOwner(groupUUID, adminUserUUID)) {
                sendErrorResponse(exchange, 403, "Permission denied");
                return;
            }

            Document group = groupManager.getGroup(groupUUID);
            if (group == null) {
                sendErrorResponse(exchange, 404, "Group not found");
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

            if (index < 0 || index >= announcements.size()) {
                sendErrorResponse(exchange, 400, "Invalid announcement index");
                return;
            }

            announcements.set(index, announcement.trim());

            boolean success = groupManager.updateGroupAnnouncements(groupUUID, announcements);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Announcement updated successfully" : "Failed to update announcement");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating announcement: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleRemoveAnnouncement(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

            String groupId = json.has("groupId") && !json.get("groupId").isJsonNull() ? json.get("groupId").getAsString() : null;
            Integer index = json.has("index") && !json.get("index").isJsonNull() ? json.get("index").getAsInt() : null;
            String adminUUID = json.has("adminUUID") && !json.get("adminUUID").isJsonNull() ? json.get("adminUUID").getAsString() : null;

            if (groupId == null || index == null || adminUUID == null) {
                sendErrorResponse(exchange, 400, "Missing required parameters: groupId, index, or adminUUID");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            UUID adminUserUUID = UUID.fromString(adminUUID);

            if (!groupManager.isAdminOrOwner(groupUUID, adminUserUUID)) {
                sendErrorResponse(exchange, 403, "Permission denied");
                return;
            }

            Document group = groupManager.getGroup(groupUUID);
            if (group == null) {
                sendErrorResponse(exchange, 404, "Group not found");
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

            if (index < 0 || index >= announcements.size()) {
                sendErrorResponse(exchange, 400, "Invalid announcement index");
                return;
            }

            announcements.remove(index.intValue());

            boolean success = groupManager.updateGroupAnnouncements(groupUUID, announcements);

            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Announcement removed successfully" : "Failed to remove announcement");
            sendJsonResponse(exchange, success ? 200 : 500, response);
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing announcement: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}
