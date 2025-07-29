package me.harshit.minechat;

import me.harshit.minechat.commands.ChatCommandHandler;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.database.UserDataManager;
import me.harshit.minechat.listeners.ChatListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Minechat extends JavaPlugin {

    private DatabaseManager databaseManager;
    private UserDataManager userDataManager;
    private ChatListener chatListener;
    private ChatCommandHandler commandHandler;

    @Override
    public void onEnable() {
        getLogger().info("Starting MineChat...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize db manager
        databaseManager = new DatabaseManager(this);

        if (databaseManager.connect()) {
            getLogger().info("✓ Database connection successful!");

            // Initialize user data manager
            userDataManager = new UserDataManager(databaseManager.getDatabase(), this);
            getLogger().info("✓ Userdata manager initialized!");
        } else {
            getLogger().severe("✗ Failed to connect to database! Check your config.yml");
            getLogger().severe("Plugin will still work but messages won't be saved to database.");
        }

        // Initialize chat listener
        chatListener = new ChatListener(this, databaseManager);

        // Initialize command handler
        commandHandler = new ChatCommandHandler(this, databaseManager, userDataManager);

        // Register the chat listener
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Register commands
        registerCommands();

        // Success message
        getLogger().info("✓ MineChat enabled successfully!");

        // Notify online players that the plugin is active
        // I mean why not
        getServer().broadcast(Component.text("MineChat is now active!")
                .color(NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down MineChat...");

        // Disconnect from database
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("✓ MineChat plugin disabled successfully!");
    }

    // register all the plugin commands
    private void registerCommands() {
        // reg main command
        getCommand("minechat").setExecutor(commandHandler);
        getCommand("minechat").setTabCompleter(commandHandler);

        // reg private message commands
        for (String alias : getConfig().getStringList("private-messages.aliases")) {
            if (getCommand(alias) != null) {
                getCommand(alias).setExecutor(commandHandler);
                getCommand(alias).setTabCompleter(commandHandler);
            }
        }

        getLogger().info("✓ Commands registered successfully!");
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
