package me.harshit.minechat.commands;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.database.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Minechat Command Handler: Handles all commands
public class ChatCommandHandler implements CommandExecutor, TabCompleter {

    private final Minechat plugin;
    private final DatabaseManager databaseManager;
    private final UserDataManager userDataManager;

    public ChatCommandHandler(Minechat plugin, DatabaseManager databaseManager, UserDataManager userDataManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.userDataManager = userDataManager;
    }

    // Command Handlers below
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("minechat")) {
            return handleMineChatCommand(sender, args);
        }

        if (isPrivateMessageCommand(command.getName())) {
            return handlePrivateMessage(sender, args);
        }

        return false;
    }

    private boolean handleMineChatCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Show help
            sender.sendMessage(Component.text("=== MineChat Commands ===").color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/minechat setpassword <password> - Set web access password").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("/minechat removepassword - Remove web access").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("/minechat status - Check your web access status").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("/minechat weburl - Get the web interface URL").color(NamedTextColor.GREEN));
            if (sender.hasPermission("minechat.admin")) {
                sender.sendMessage(Component.text("/minechat reload - Reload plugin configuration").color(NamedTextColor.YELLOW));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setpassword":
                return handleSetPassword(sender, args);

            case "removepassword":
                return handleRemovePassword(sender);

            case "status":
                return handleStatus(sender);

            case "weburl":
            case "url":
            case "web":
                return handleWebUrl(sender);

            case "reload":
                return handleReload(sender);

            default:
                sender.sendMessage(Component.text("Unknown command. Use /minechat for help.").color(NamedTextColor.RED));
                return true;
        }
    }

    // web password handler here
    private boolean handleSetPassword(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can set web passwords!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /minechat setpassword <password>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /minechat setpassword mySecretPassword123").color(NamedTextColor.GRAY));
            return true;
        }

        Player player = (Player) sender;
        String password = String.join(" ", Arrays.copyOfRange(args, 1, args.length));


        if (password.length() < 8) {
            sender.sendMessage(Component.text("Password must be at least 8 characters long!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = userDataManager.setWebPassword(player.getUniqueId(), player.getName(), password);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Web password set successfully!").color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("You can now log into the web interface at " +
                        plugin.getConfig().getString("web.interface-url", "http://localhost:3000"))
                        .color(NamedTextColor.AQUA));
                    player.sendMessage(Component.text("Use your username and the password you set.").color(NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("✗ Failed to set web password. Check console for errors.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleRemovePassword(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can remove web passwords!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = userDataManager.disableWebAccess(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Web access disabled successfully!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("✗ Failed to disable web access.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }


    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can check web status!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasAccess = userDataManager.hasWebAccess(player.getName());

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(Component.text("=== Web Access Status ===").color(NamedTextColor.AQUA));
                if (hasAccess) {
                    sender.sendMessage(Component.text("✓ Web access: ENABLED").color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("You can log into the web interface with your username and password.").color(NamedTextColor.GRAY));

                    // Show the web interface URL
                    String webUrl = plugin.getConfig().getString("web.interface-url", "http://localhost:3000");
                    sender.sendMessage(Component.text("Web Interface: " + webUrl).color(NamedTextColor.AQUA));
                } else {
                    sender.sendMessage(Component.text("✗ Web access: DISABLED").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Use /minechat setpassword <password> to enable web access.").color(NamedTextColor.GRAY));
                }
            });
        });

        return true;
    }


    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("minechat.admin")) {
            sender.sendMessage(Component.text("You don't have permission to reload the plugin!").color(NamedTextColor.RED));
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(Component.text("✓ MineChat configuration reloaded!").color(NamedTextColor.GREEN));
        return true;
    }

   // pm handler
    private boolean handlePrivateMessage(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("private-messages.enable", true)) {
            sender.sendMessage(Component.text("Private messages are disabled!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /msg <player> <message>").color(NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player '" + targetName + "' is not online!").color(NamedTextColor.RED));
            return true;
        }

        // msg format from the config
        String format = plugin.getConfig().getString("private-messages.format",
            "&7[&dPM&7] &e{sender} &7→ &e{receiver}&7: &f{message}");

        String formattedMessage = format
            .replace("{sender}", sender.getName())
            .replace("{receiver}", target.getName())
            .replace("{message}", message);

        Component messageComponent = Component.text(formattedMessage.replace("&", "§"));

        // Send to both players
        sender.sendMessage(messageComponent);
        target.sendMessage(messageComponent);


        // Store in db if true
        if (plugin.getConfig().getBoolean("chat.enable-logging", true)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (sender instanceof Player) {
                    databaseManager.storeChatMessage(
                        sender.getName(),
                        ((Player) sender).getUniqueId(),
                        "[PM to " + target.getName() + "] " + message,
                        plugin.getServer().getName()
                    );
                }
            });
        }

        return true;
    }


    private boolean isPrivateMessageCommand(String commandName) {
        List<String> aliases = plugin.getConfig().getStringList("private-messages.aliases");
        return aliases.contains(commandName.toLowerCase());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("minechat")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("setpassword", "removepassword", "status", "weburl"));
                if (sender.hasPermission("minechat.admin")) {
                    completions.add("reload");
                }
            }
        } else if (isPrivateMessageCommand(command.getName())) {
            if (args.length == 1) {
                // Tab complete online player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.equals(sender)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }


    private boolean handleWebUrl(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can access the web URL!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasAccess = userDataManager.hasWebAccess(player.getName());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasAccess) {
                    String url = plugin.getConfig().getString("web.url", "http://localhost:8080");
                    player.sendMessage(Component.text("Web interface URL: " + url).color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("✗ Web access is disabled. Set a password to enable it.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }
}
