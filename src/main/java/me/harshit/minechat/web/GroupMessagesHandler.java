package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.api.GroupMessage;
import me.harshit.minechat.database.GroupManager;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupMessagesHandler implements HttpHandler {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final Gson gson;

    public GroupMessagesHandler(Minechat plugin) {
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

        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetGroupMessages(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in GroupMessagesHandler: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleGetGroupMessages(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            sendErrorResponse(exchange, 400, "Missing parameters");
            return;
        }

        String groupId = null;
        int limit = 50; // Default limit
        Long before = null;

        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                switch (keyValue[0]) {
                    case "groupId":
                        groupId = keyValue[1];
                        break;
                    case "limit":
                        try {
                            limit = Integer.parseInt(keyValue[1]);
                            limit = Math.min(limit, 100); // Max 100 messages
                        } catch (NumberFormatException e) {
                            limit = 50;
                        }
                        break;
                    case "before":
                        try {
                            before = Long.parseLong(keyValue[1]);
                        } catch (NumberFormatException e) {
                            // ignore invalid before parameter
                        }
                        break;
                }
            }
        }

        if (groupId == null || groupId.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing or invalid groupId parameter");
            return;
        }

        try {
            UUID groupUUID = UUID.fromString(groupId);
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

            List<Document> messageDocuments = groupManager.getGroupMessages(groupUUID, limit);
            List<GroupMessage> messages = new ArrayList<>();

            // convert documents to GroupMessage objects
            for (Document doc : messageDocuments) {
                try {
                    GroupMessage message = GroupMessage.fromDocument(doc);
                    if (message != null) {
                        messages.add(message);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse message document: " + e.getMessage());
                }
            }

            JsonObject response = new JsonObject();
            JsonObject[] messagesArray = new JsonObject[messages.size()];

            for (int i = 0; i < messages.size(); i++) {
                GroupMessage message = messages.get(i);
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("messageId", message.getMessageId().toString());
                messageObj.addProperty("groupId", message.getGroupId().toString());
                messageObj.addProperty("senderUUID", message.getSenderUUID() != null ? message.getSenderUUID().toString() : null);
                messageObj.addProperty("senderName", message.getSenderName());
                messageObj.addProperty("content", message.getContent());
                messageObj.addProperty("timestamp", message.getTimestamp().toString());
                messageObj.addProperty("messageType", message.getMessageType().getValue());
                messageObj.addProperty("editedAt", message.getEditedAt() != null ? message.getEditedAt().toString() : null);

                if (message.hasReactions()) {
                    JsonObject reactions = new JsonObject();
                    message.getReactions().forEach((emoji, users) -> {
                        JsonObject emojiData = new JsonObject();
                        emojiData.addProperty("count", users.size());
                        reactions.add(emoji, emojiData);
                    });
                    messageObj.add("reactions", reactions);
                } else {
                    messageObj.add("reactions", new JsonObject());
                }

                messagesArray[i] = messageObj;
            }

            response.add("messages", gson.toJsonTree(messagesArray));

            sendSuccessResponse(exchange, response);

        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Invalid groupId format");
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting group messages: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to get group messages");
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
