package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.harshit.minechat.Minechat;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class MinechatWebSocketHandler extends WebSocketAdapter {

    private final Minechat plugin;
    private final Gson gson;
    private final WebAPIHandler apiHandler;


    private static final Map<Session, String> sessionToId = new ConcurrentHashMap<>();
    private static final Map<String, Session> idToSession = new ConcurrentHashMap<>();

    public MinechatWebSocketHandler(Minechat plugin, WebAPIHandler apiHandler) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.apiHandler = apiHandler;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String sessionId = generateSessionId();
        sessionToId.put(session, sessionId);
        idToSession.put(sessionId, session);

        plugin.getLogger().info("New WebSocket connection established: " + sessionId);

        sendMessage(session, "connection", Map.of(
            "success", true,
            "sessionId", sessionId,
            "message", "Connected to Minechat WebSocket"
        ));
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String sessionId = sessionToId.remove(session);
        if (sessionId != null) {
            idToSession.remove(sessionId);
            apiHandler.removeSession(sessionId);
            plugin.getLogger().info("WebSocket connection closed: " + sessionId + " - " + reason);
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            // json instead of map
            JsonObject messageObj = gson.fromJson(message, JsonObject.class);
            String type = messageObj.has("type") ? messageObj.get("type").getAsString() : null;
            JsonObject data = messageObj.has("data") ? messageObj.getAsJsonObject("data") : new JsonObject();
            String sessionId = sessionToId.get(session);

            if (sessionId == null) {
                sendError(session, "Invalid session");
                return;
            }

            if (type == null) {
                sendError(session, "Message type is required");
                return;
            }

            if ("auth".equals(type)) {
                Map<String, Object> authData = new java.util.HashMap<>();
                if (data.has("username")) {
                    authData.put("username", data.get("username").getAsString());
                }
                if (data.has("password")) {
                    authData.put("password", data.get("password").getAsString());
                }
                handleAuthentication(session, sessionId, authData);
            } else if ("ping".equals(type)) {
                // Handle ping/pong for connection keepalive
                sendMessage(session, "pong", Map.of("timestamp", System.currentTimeMillis()));
            } else {
                apiHandler.handleWebMessage(sessionId, type, data);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Invalid message format");
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        String sessionId = sessionToId.get(session);
        plugin.getLogger().warning("WebSocket error for session " + sessionId + ": " + error.getMessage());
        error.printStackTrace();
    }

    private void handleAuthentication(Session session, String sessionId, Map<String, Object> data) {
        try {
            String username = (String) data.get("username");
            String password = (String) data.get("password");

            if (username == null || password == null) {
                sendMessage(session, "auth_response", Map.of(
                    "success", false,
                    "error", "Username and password required"
                ));
                return;
            }

            boolean authenticated = apiHandler.authenticateSession(sessionId, username, password);

            if (authenticated) {
                sendMessage(session, "auth_response", Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "username", username,
                    "message", "Authentication successful"
                ));
                plugin.getLogger().info("WebSocket authentication successful for: " + username);
            } else {
                sendMessage(session, "auth_response", Map.of(
                    "success", false,
                    "error", "Invalid credentials"
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Authentication error: " + e.getMessage());
            sendError(session, "Authentication failed");
        }
    }

    public static void sendToSession(String sessionId, String type, Object data) {
        Session session = idToSession.get(sessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, type, data);
        }
    }

    public static boolean isSessionConnected(String sessionId) {
        Session session = idToSession.get(sessionId);
        return session != null && session.isOpen();
    }

    public static void broadcastToAll(String type, Object data) {
        idToSession.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, type, data);
            }
        });
    }

    public static java.util.Set<String> getActiveSessionIds() {
        return idToSession.keySet();
    }

    private static void sendMessage(Session session, String type, Object data) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> response = Map.of(
                    "type", type,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
                );

                String json = new Gson().toJson(response);
                session.getRemote().sendString(json);
            } catch (IOException e) {
                System.err.println("Failed to send WebSocket message: " + e.getMessage());
            }
        }
    }

    private void sendError(Session session, String error) {
        sendMessage(session, "error", Map.of("message", error));
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public static void cleanup() {
        sessionToId.clear();
        idToSession.clear();
    }
}
