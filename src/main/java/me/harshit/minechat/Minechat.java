package me.harshit.minechat;

import me.harshit.minechat.api.FriendAPI;
import me.harshit.minechat.api.FriendAPIImpl;
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
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minechat extends JavaPlugin {

    private DatabaseManager databaseManager;
    private UserDataManager userDataManager;
    private FriendManager friendManager;
    private GroupManager groupManager;
    private RankManager rankManager;
    private FriendAPI friendAPI;
    private EmbeddedWebServer webServer;
    private ChatListener chatListener;
    private PlayerDataListener playerDataListener;
    private ChatCommandHandler commandHandler;
    private FriendCommandHandler friendCommandHandler;
    private GroupCommandHandler groupCommandHandler;

    private me.harshit.minechat.web.WebAPIHandler webAPIHandler;
    private me.harshit.minechat.web.MinechatWebSocketServer webSocketServer;

    @Override
    public void onEnable() {
        getLogger().info("Starting MineChat...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        rankManager = new RankManager(this);

        databaseManager = new DatabaseManager(this);

        if (databaseManager.connect()) {
            getLogger().info("✓ Database connection successful!");

            userDataManager = new UserDataManager(databaseManager.getDatabase(), this);

            friendManager = new FriendManager(databaseManager.getDatabase(), this);

            groupManager = new GroupManager(databaseManager.getDatabase(), this);

            friendAPI = new FriendAPIImpl(friendManager, this);

            webAPIHandler = new me.harshit.minechat.web.WebAPIHandler(this, userDataManager, friendManager, groupManager);

            if (getConfig().getBoolean("web.enable-api", true)) {
                webServer = new EmbeddedWebServer(this, databaseManager, userDataManager, friendManager, groupManager);
                webServer.start();
            }

            if (getConfig().getBoolean("web.enable-websocket", true)) {
                int wsPort = getConfig().getInt("web.websocket-port", 8081);
                webSocketServer = new me.harshit.minechat.web.MinechatWebSocketServer(this, webAPIHandler, wsPort);
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


    private void registerAPIService() {
        if (friendAPI != null) {
            getServer().getServicesManager().register(FriendAPI.class, friendAPI, this, ServicePriority.Normal);
        }
    }


    public FriendAPI getFriendAPI() {
        return friendAPI;
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
