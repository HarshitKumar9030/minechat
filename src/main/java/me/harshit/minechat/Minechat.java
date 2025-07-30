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
    private FriendAPI friendAPI;
    private EmbeddedWebServer webServer;
    private ChatListener chatListener;
    private ChatCommandHandler commandHandler;
    private FriendCommandHandler friendCommandHandler;
    private GroupCommandHandler groupCommandHandler;

    @Override
    public void onEnable() {
        getLogger().info("Starting MineChat...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize db manager
        databaseManager = new DatabaseManager(this);

        if (databaseManager.connect()) {
            getLogger().info("✓ Database connection successful!");

            userDataManager = new UserDataManager(databaseManager.getDatabase(), this);

            friendManager = new FriendManager(databaseManager.getDatabase(), this);

            groupManager = new GroupManager(databaseManager.getDatabase(), this);

            friendAPI = new FriendAPIImpl(friendManager, this);

            // Initialize embedded web server
            if (getConfig().getBoolean("web.enable-api", true)) {
                webServer = new EmbeddedWebServer(this, userDataManager, friendManager, groupManager);
                webServer.start();
            }
        } else {
            getLogger().severe("✗ Failed to connect to database! Check your config.yml");
            getLogger().severe("Plugin will still work but messages won't be saved to database.");
        }

        // Initialize chat listener
        chatListener = new ChatListener(this, databaseManager);

        // Initialize command handlers
        commandHandler = new ChatCommandHandler(this, databaseManager, userDataManager, friendManager);
        friendCommandHandler = new FriendCommandHandler(this, friendManager, userDataManager);
        groupCommandHandler = new GroupCommandHandler(this, groupManager);

        getServer().getPluginManager().registerEvents(chatListener, this);

        registerCommands();

        registerAPIService();

        // Success message
        getLogger().info("✓ MineChat enabled successfully!");

        // Notify online players that the plugin is active
        getServer().broadcast(Component.text("MineChat is now active!")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down MineChat...");

        if (webServer != null) {
            webServer.stop();
        }

        // Disconnect from database
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("✓ MineChat disabled successfully!");
    }

    // register all the plugin commands
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

    // @return DatabaseManager instance
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // gets then returns @UserDataManager instance
    // this is used to manage user data like web access, etc.
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }
}
