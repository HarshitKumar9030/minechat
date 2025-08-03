package me.harshit.minechat.web;

import me.harshit.minechat.Minechat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

public class MinechatWebSocketServer {

    private final Minechat plugin;
    private final WebAPIHandler apiHandler;
    private final int port;
    private Server server;

    public MinechatWebSocketServer(Minechat plugin, WebAPIHandler apiHandler, int port) {
        this.plugin = plugin;
        this.apiHandler = apiHandler;
        this.port = port;
    }

    public void start() {
        // Stop any existing server first
        stop();

        try {
            server = new Server();

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            connector.setHost("localhost"); // Bind to localhost only for security
            connector.setReuseAddress(true); // Allow port reuse
            server.addConnector(connector);

            // servlet context here
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                wsContainer.setMaxTextMessageSize(65536);
                wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));

                wsContainer.addMapping("/ws", (upgradeRequest, upgradeResponse) ->
                    new MinechatWebSocketHandler(plugin, apiHandler));
            });

            server.start();
            plugin.getLogger().info("WebSocket server started on port " + port);
            plugin.getLogger().info("WebSocket endpoint: ws://localhost:" + port + "/ws");

        } catch (Exception e) {
            boolean isPortConflict = isPortBindingException(e);

            if (isPortConflict) {
                plugin.getLogger().severe("Port " + port + " is already in use!");
                plugin.getLogger().severe("Either another application is using this port, or the previous server wasn't properly shut down.");
                plugin.getLogger().severe("Try changing the websocket-port in config.yml or restart the server.");

                tryAlternativePorts();
            } else {
                plugin.getLogger().severe("Failed to start WebSocket server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isPortBindingException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                message = message.toLowerCase();
                if (message.contains("bind") ||
                    message.contains("address already in use") ||
                    message.contains("port") && message.contains("use")) {
                    return true;
                }
            }

            if (current instanceof java.net.BindException ||
                current instanceof java.net.SocketException) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private void tryAlternativePorts() {
        plugin.getLogger().info("Attempting to find an available port...");

        int[] alternativePorts = {8082, 8083, 8084, 8085, 9081, 9082, 9083};

        for (int altPort : alternativePorts) {
            try {
                plugin.getLogger().info("Trying port " + altPort + "...");

                server = new Server();
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(altPort);
                connector.setHost("localhost");
                connector.setReuseAddress(true);
                server.addConnector(connector);

                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                context.setContextPath("/");
                server.setHandler(context);

                JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
                    wsContainer.setMaxTextMessageSize(65536);
                    wsContainer.setIdleTimeout(java.time.Duration.ofMinutes(10));

                    wsContainer.addMapping("/ws", (upgradeRequest, upgradeResponse) ->
                        new MinechatWebSocketHandler(plugin, apiHandler));
                });

                server.start();
                plugin.getLogger().info("✓ WebSocket server started successfully on alternative port " + altPort);
                plugin.getLogger().info("WebSocket endpoint: ws://localhost:" + altPort + "/ws");
                plugin.getLogger().warning("Consider updating your config.yml websocket-port to " + altPort + " to avoid this issue.");
                return;

            } catch (Exception e) {
                plugin.getLogger().info("Port " + altPort + " is also in use, trying next...");
                stop();
            }
        }

        plugin.getLogger().severe("Could not find any available ports for WebSocket server!");
        plugin.getLogger().severe("Please check what processes are using ports 8081-8085 and 9081-9083, or manually set a different port in config.yml");
    }

    public void stop() {
        if (server != null) {
            try {
                MinechatWebSocketHandler.cleanup();

                if (server.isRunning()) {
                    server.stop();
                }

                // Force cleanup
                if (server.getConnectors() != null) {
                    for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
                        try {
                            connector.stop();
                        } catch (Exception e) {
                        }
                    }
                }

                server = null;
                plugin.getLogger().info("WebSocket server stopped");

            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping WebSocket server: " + e.getMessage());
                server = null;
            }
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    public int getPort() {
        if (server != null && server.getConnectors().length > 0) {
            ServerConnector connector = (ServerConnector) server.getConnectors()[0];
            return connector.getLocalPort();
        }
        return port;
    }
}
