package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.GroupManager;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SearchGroupsHandler implements HttpHandler {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public SearchGroupsHandler(Minechat plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
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

        if ("GET".equals(exchange.getRequestMethod())) {
            handleSearchGroups(exchange);
        } else {
            sendErrorResponse(exchange, 405, "Method not allowed");
        }
    }

    private void handleSearchGroups(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            sendErrorResponse(exchange, 400, "Missing query parameter");
            return;
        }

        String searchQuery = null;
        int limit = 10;

        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                switch (keyValue[0]) {
                    case "query":
                        searchQuery = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        break;
                    case "limit":
                        try {
                            limit = Integer.parseInt(keyValue[1]);
                            limit = Math.min(limit, 50); // Max 50 results
                        } catch (NumberFormatException e) {
                            limit = 10;
                        }
                        break;
                }
            }
        }

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Search query is required");
            return;
        }

        try {
            List<Document> results = groupManager.searchPublicGroups(searchQuery, limit);

            JsonObject response = new JsonObject();
            JsonObject[] groupsArray = new JsonObject[results.size()];

            for (int i = 0; i < results.size(); i++) {
                Document group = results.get(i);
                JsonObject groupObj = new JsonObject();
                groupObj.addProperty("groupId", group.getString("groupId"));
                groupObj.addProperty("groupName", group.getString("groupName"));
                groupObj.addProperty("description", group.getString("description"));

                List<Document> members = group.getList("members", Document.class);
                groupObj.addProperty("memberCount", members != null ? members.size() : 0);
                groupObj.addProperty("maxMembers", group.getInteger("maxMembers", 25));
                groupObj.addProperty("ownerName", group.getString("ownerName"));
                groupObj.addProperty("isPrivate", group.getBoolean("isPrivate", false));
                groupObj.addProperty("createdAt", group.getLong("createdDate"));

                groupsArray[i] = groupObj;
            }

            response.add("groups", gson.toJsonTree(groupsArray));
            sendSuccessResponse(exchange, response);

        } catch (Exception e) {
            plugin.getLogger().severe("Error searching groups: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to search groups");
        }
    }

    private void sendSuccessResponse(HttpExchange exchange, JsonObject data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String response = gson.toJson(data);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
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
