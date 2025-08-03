package me.harshit.minechat.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.harshit.minechat.Minechat;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class MinechatWebSocketHandler extends WebSocketAdapter {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<Session, String> sessionIds = new ConcurrentHashMap<>();

    private final Minechat plugin;
    private final WebAPIHandler apiHandler;
    private final Gson gson;
    private String sessionId;

    public MinechatWebSocketHandler(Minechat plugin, WebAPIHandler apiHandler) {
        this.plugin = plugin;
        this.apiHandler = apiHandler;
        this.gson = new Gson();
    }

    @Override
    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        this.sessionId = generateSessionId();
        sessions.put(sessionId, session);
        sessionIds.put(session, sessionId);

        plugin.getLogger().fine("WebSocket connection established: " + sessionId);

        // send connection confirmation
        JsonObject response = new JsonObject();
        response.addProperty("type", "connection");
        response.addProperty("sessionId", sessionId);
        response.addProperty("status", "connected");

        sendMessage(session, response);
    }

    @Override
    @OnWebSocketMessage
    public void onWebSocketText(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            String type = json.get("type").getAsString();
            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();

            // only log non-routine messages to reduce spam
            if (!"ping".equals(type) && !"group_message".equals(type)) {
                plugin.getLogger().fine("WebSocket message received: " + type + " from session: " + sessionId);
            }

            // Handle authentication
            if ("auth".equals(type)) {
                handleAuthentication(data);
                return;
            }

            // Handle ping messages silently
            if ("ping".equals(type)) {
                handlePing();
                return;
            }

            // route to API handler for other message types
            if (apiHandler != null) {
                apiHandler.handleWebMessage(sessionId, type, data);
            } else {
                sendError("API handler not available");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error processing WebSocket message: " + e.getMessage());
            sendError("Invalid message format");
        }
    }

    @Override
    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);

        if (sessionId != null) {
            sessions.remove(sessionId);
            sessionIds.remove(getSession());

            if (apiHandler != null) {
                apiHandler.removeSession(sessionId);
            }

            // Only log unexpected disconnections, not normal client disconnects or timeouts
            if (statusCode != 1000 && statusCode != 1001) {
                plugin.getLogger().fine("WebSocket connection closed: " + sessionId + " (Code: " + statusCode + ", Reason: " + reason + ")");
            }
        }
    }

    @Override
    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);

        // Handle timeout errors silently as they are normal
        if (cause.getMessage() != null && cause.getMessage().contains("Idle Timeout")) {
            plugin.getLogger().fine("WebSocket idle timeout for session " + sessionId);
            return;
        }

        plugin.getLogger().warning("WebSocket error for session " + sessionId + ": " + cause.getMessage());
    }

    private void handleAuthentication(JsonObject data) {
        try {
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            boolean authenticated = apiHandler.authenticateSession(sessionId, username, password);

            JsonObject response = new JsonObject();
            response.addProperty("type", "auth_response");
            response.addProperty("success", authenticated);

            if (authenticated) {
                response.addProperty("message", "Authentication successful");
                response.addProperty("username", username);
                plugin.getLogger().info("WebSocket authentication successful for: " + username);
            } else {
                response.addProperty("message", "Authentication failed");
                plugin.getLogger().warning("WebSocket authentication failed for: " + username);
            }

            sendMessage(getSession(), response);

        } catch (Exception e) {
            plugin.getLogger().warning("Error during WebSocket authentication: " + e.getMessage());
            sendError("Authentication error");
        }
    }

    private void handlePing() {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("type", "pong");
            sendMessage(getSession(), response);
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling ping: " + e.getMessage());
        }
    }

    private void sendMessage(Session session, JsonObject message) {
        try {
            if (session != null && session.isOpen()) {
                String jsonString = gson.toJson(message);
                session.getRemote().sendString(jsonString);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending WebSocket message: " + e.getMessage());
        }
    }

    private void sendError(String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("message", errorMessage);

        sendMessage(getSession(), error);
    }

    private String generateSessionId() {
        return "ws-" + System.currentTimeMillis() + "-" + Math.random();
    }

    // static methods for external access
    public static boolean isSessionConnected(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null && session.isOpen();
    }

    public static void sendToSession(String sessionId, String type, Object data) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                JsonObject message = new JsonObject();
                message.addProperty("type", type);
                message.add("data", new Gson().toJsonTree(data));
                message.addProperty("timestamp", System.currentTimeMillis());

                String jsonString = new Gson().toJson(message);
                session.getRemote().sendString(jsonString);
            } catch (Exception e) {
                System.err.println("Error sending message to WebSocket session " + sessionId + ": " + e.getMessage());
            }
        }
    }

    public static Set<String> getActiveSessionIds() {
        return sessions.keySet();
    }

    public static int getActiveSessionCount() {
        return sessions.size();
    }

    public static void broadcastToAll(String type, Object data) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("data", new Gson().toJsonTree(data));
        message.addProperty("timestamp", System.currentTimeMillis());

        String jsonString = new Gson().toJson(message);

        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(jsonString);
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting WebSocket message: " + e.getMessage());
            }
        });
    }

    public static void cleanup() {
        try {
            // close all open sessions
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) {
                    System.err.println("Error closing WebSocket session: " + e.getMessage());
                }
            });

            // Clear all session collections
            sessions.clear();
            sessionIds.clear();
        } catch (Exception e) {
            System.err.println("Error during WebSocket cleanup: " + e.getMessage());
        }
    }
}
