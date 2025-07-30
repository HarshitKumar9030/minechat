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
        try {
            server = new Server();


            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
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
            plugin.getLogger().severe("Failed to start WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            try {
                MinechatWebSocketHandler.cleanup();
                server.stop();
                plugin.getLogger().info("WebSocket server stopped");
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping WebSocket server: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    public int getPort() {
        return port;
    }
}
