package me.harshit.minechat;

import me.harshit.minechat.api.FriendAPI;
import me.harshit.minechat.api.FriendAPIImpl;
import me.harshit.minechat.api.GroupAPI;
import me.harshit.minechat.api.GroupAPIImpl;
import me.harshit.minechat.commands.ChatCommandHandler;
import me.harshit.minechat.commands.FriendCommandHandler;
import me.harshit.minechat.commands.GroupCommandHandler;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.database.UserDataManager;
import me.harshit.minechat.listeners.ChatListener;
import me.harshit.minechat.listeners.PlayerDataListener;
import me.harshit.minechat.ranks.RankManager;
import me.harshit.minechat.web.EmbeddedWebServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minechat extends JavaPlugin {

    private DatabaseManager databaseManager;
    private UserDataManager userDataManager;
    private FriendManager friendManager;
    private GroupManager groupManager;
    private RankManager rankManager;
    private FriendAPI friendAPI;
    private GroupAPI groupAPI;
    private EmbeddedWebServer webServer;
    private ChatListener chatListener;
    private PlayerDataListener playerDataListener;
    private ChatCommandHandler commandHandler;
    private FriendCommandHandler friendCommandHandler;
    private GroupCommandHandler groupCommandHandler;

    private me.harshit.minechat.web.WebAPIHandler webAPIHandler;
    private me.harshit.minechat.web.MinechatWebSocketServer webSocketServer;

    public static boolean QUIET_WS_LOGS = true;

    @Override
    public void onEnable() {
        getLogger().info("Starting MineChat...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

    rankManager = new RankManager(this);

    setupQuietLoggers();

        QUIET_WS_LOGS = getConfig().getBoolean("web.quiet-websocket-logs", true);
        boolean showBanner = getConfig().getBoolean("logging.banner", true);
        boolean debug = getConfig().getBoolean("logging.debug", false);

        if (showBanner) {
            printBanner(debug);
        }

        databaseManager = new DatabaseManager(this);

        if (databaseManager.connect()) {
            getLogger().info("✓ Database connection successful!");

            userDataManager = new UserDataManager(databaseManager.getDatabase(), this);

            friendManager = new FriendManager(databaseManager.getDatabase(), this);

            groupManager = new GroupManager(databaseManager.getDatabase(), this);

            friendAPI = new FriendAPIImpl(friendManager, this);
            groupAPI = new GroupAPIImpl(groupManager, this);

            webAPIHandler = new me.harshit.minechat.web.WebAPIHandler(this, userDataManager, friendManager, groupManager);

            if (getConfig().getBoolean("web.enable-api", true)) {
                webServer = new EmbeddedWebServer(this, databaseManager, userDataManager, friendManager, groupManager);
                webServer.start();
            }

            if (getConfig().getBoolean("web.enable-websocket", true)) {
                int wsPort = getConfig().getInt("web.websocket-port", 8081);
                webSocketServer = new me.harshit.minechat.web.MinechatWebSocketServer(this, webAPIHandler, wsPort);
                webAPIHandler.setWebSocketServer(webSocketServer);
                webSocketServer.start();
            }
        } else {
            getLogger().severe("✗ Failed to connect to database! Check your config.yml");
            getLogger().severe("Plugin will still work but messages won't be saved to database.");
        }

        chatListener = new ChatListener(this, databaseManager, rankManager);

        playerDataListener = new PlayerDataListener(this, userDataManager);

        commandHandler = new ChatCommandHandler(this, databaseManager, userDataManager, friendManager);
        friendCommandHandler = new FriendCommandHandler(this, friendManager, userDataManager);
        groupCommandHandler = new GroupCommandHandler(this, groupManager);

        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(playerDataListener, this);

        registerCommands();

    registerAPIService();

    getLogger().info("✓ MineChat enabled successfully!");

    // if not available log else dont spam the console
        if (!rankManager.isRankSystemAvailable()) {
            getLogger().warning("! No rank plugin detected - using default chat format");

        }

        getServer().broadcast(Component.text("MineChat is now active!")
                .color(NamedTextColor.GREEN));
    }

    private void setupQuietLoggers() {
        boolean debug = getConfig().getBoolean("logging.debug", false);
        if (debug) return; 

        Logger mongo = Logger.getLogger("org.mongodb");
        mongo.setLevel(Level.WARNING);
        Logger mongoDriver = Logger.getLogger("org.mongodb.driver");
        mongoDriver.setLevel(Level.WARNING);

        Logger jetty = Logger.getLogger("org.eclipse.jetty");
        jetty.setLevel(Level.WARNING);

        Logger spark = Logger.getLogger("spark");
        spark.setLevel(Level.SEVERE);
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down MineChat...");

        if (webServer != null) {
            webServer.stop();
        }

        if (webSocketServer != null) {
            webSocketServer.stop();
        }

        if (webAPIHandler != null) {
            webAPIHandler.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("✓ MineChat disabled successfully!");
    }

    private void registerCommands() {
        // reg main command
        getCommand("minechat").setExecutor(commandHandler);
        getCommand("minechat").setTabCompleter(commandHandler);

        // Register friend command
        getCommand("friend").setExecutor(friendCommandHandler);
        getCommand("friend").setTabCompleter(friendCommandHandler);

        // Register group command
        getCommand("group").setExecutor(groupCommandHandler);
        getCommand("group").setTabCompleter(groupCommandHandler);

        // reg private message commands
        for (String alias : getConfig().getStringList("private-messages.aliases")) {
            if (getCommand(alias) != null) {
                getCommand(alias).setExecutor(commandHandler);
                getCommand(alias).setTabCompleter(commandHandler);
            }
        }

    }
    
    private void printBanner(boolean debug) {
        String[] lines = new String[] {
                   "     __  ________   ________________  _____  ______",
                   "    /  |/  /  _/ | / / ____/ ____/ / / /   |/_  __/",
                   "   / /|_/ // //  |/ / __/ / /   / /_/ / /| | / /   ",
                   "  / /  / // // /|  / /___/ /___/ __  / ___ |/ /    ",
                   " /_/  /_/___/_/ |_/_____/____/_/  /_/_/   _/_/     ",
                   "                                                  ",
        };
        for (int i = 0; i < lines.length; i++) {
            getServer().getConsoleSender().sendMessage(Component.text(lines[i]).color(NamedTextColor.GOLD));
        }
        String subtitle = "              Minechat loaded" + (debug ? " [debug]" : "");
        getServer().getConsoleSender().sendMessage(Component.text(subtitle).color(NamedTextColor.DARK_GRAY));
    }


    private void registerAPIService() {
        if (friendAPI != null) {
            getServer().getServicesManager().register(FriendAPI.class, friendAPI, this, ServicePriority.Normal);
        }
        if (groupAPI != null) {
            getServer().getServicesManager().register(GroupAPI.class, groupAPI, this, ServicePriority.Normal);
        }
    }


    public FriendAPI getFriendAPI() {
        return friendAPI;
    }

    public GroupAPI getGroupAPI() {
        return groupAPI;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // gets then returns @UserDataManager instance
    // this is used to manage user data like web access, etc.
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public me.harshit.minechat.web.WebAPIHandler getWebAPIHandler() {
        return webAPIHandler;
    }

    public me.harshit.minechat.web.MinechatWebSocketServer getWebSocketServer() {
        return webSocketServer;
    }
}
