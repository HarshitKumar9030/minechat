package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.api.GroupMember;
import me.harshit.minechat.database.GroupManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupMembersHandler implements HttpHandler {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public GroupMembersHandler(Minechat plugin) {
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
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetGroupMembers(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in GroupMembersHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleGetGroupMembers(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            String groupId = getQueryParam(query, "groupId");
            String requestingPlayerUUID = getQueryParam(query, "playerUUID");

            if (groupId == null) {
                sendErrorResponse(exchange, 400, "Group ID required");
                return;
            }

            UUID groupUUID = UUID.fromString(groupId);
            Document groupDoc = groupManager.getGroupById(groupUUID);

            if (groupDoc == null) {
                sendErrorResponse(exchange, 404, "Group not found");
                return;
            }

            // convert document to GroupInfo
            GroupInfo group = GroupInfo.fromDocument(groupDoc);
            if (group == null) {
                sendErrorResponse(exchange, 500, "Failed to parse group data");
                return;
            }

            // if requesting player UUID is provided, verify they a member
            if (requestingPlayerUUID != null) {
                UUID playerUUID = UUID.fromString(requestingPlayerUUID);
                GroupMember requestingMember = groupManager.getGroupMember(groupUUID, playerUUID);
                if (requestingMember == null) {
                    sendErrorResponse(exchange, 403, "You are not a member of this group");
                    return;
                }
            }

            List<GroupMember> members = group.getMembers();
            List<Map<String, Object>> memberList = new ArrayList<>();

            for (GroupMember member : members) {
                Map<String, Object> memberData = new HashMap<>();
                memberData.put("playerId", member.getPlayerId().toString());
                memberData.put("playerName", member.getPlayerName());
                memberData.put("role", member.getRole().name());
                memberData.put("roleDisplayName", member.getRole().getDisplayName());
                memberData.put("joinedDate", member.getJoinedDate().toString());

                Player onlinePlayer = Bukkit.getPlayer(member.getPlayerId());
                memberData.put("online", onlinePlayer != null && onlinePlayer.isOnline());

                // add rank information if player is online
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    String rank = plugin.getRankManager().getPlayerRank(onlinePlayer);
                    String formattedRank = plugin.getRankManager().getFormattedRank(onlinePlayer);
                    memberData.put("rank", rank);
                    memberData.put("formattedRank", formattedRank);
                } else {
                    memberData.put("rank", "");
                    memberData.put("formattedRank", "");
                }

                memberList.add(memberData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("groupId", groupId);
            response.put("groupName", group.getGroupName());
            response.put("members", memberList);
            response.put("memberCount", members.size());
            response.put("maxMembers", group.getMaxMembers());

            sendJsonResponse(exchange, 200, response);

        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Invalid UUID format");
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting group members: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
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
