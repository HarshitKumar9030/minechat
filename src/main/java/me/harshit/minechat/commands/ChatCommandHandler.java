package me.harshit.minechat.commands;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.database.FriendManager;
import me.harshit.minechat.database.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Minechat Command Handler: Handles all commands
public class ChatCommandHandler implements CommandExecutor, TabCompleter {

    private final Minechat plugin;
    private final DatabaseManager databaseManager;
    private final UserDataManager userDataManager;
    private final FriendManager friendManager; // maybe will use it later

    // Map to track last message senders for each player (UUID)
    private final Map<String, String> lastMessageSenders = new ConcurrentHashMap<>();

    public ChatCommandHandler(Minechat plugin, DatabaseManager databaseManager, UserDataManager userDataManager, FriendManager friendManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.userDataManager = userDataManager;
        this.friendManager = friendManager;
    }

    // Command Handlers below
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("minechat")) {
            return handleMineChatCommand(sender, args);
        }

        if (isPrivateMessageCommand(command.getName())) {
            return handlePrivateMessage(sender, command.getName(), args);
        }

        return false;
    }

    private boolean handleMineChatCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("══════════════════").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("        MineChat Help        ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            sender.sendMessage(Component.text("══════════════════").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text(""));

            sender.sendMessage(Component.text("🌐 Web Access Commands:").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            sender.sendMessage(Component.text("  • /minechat setpassword <password>").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Set your web interface password").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            sender.sendMessage(Component.text("  • /minechat removepassword").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Disable web access").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            sender.sendMessage(Component.text("  • /minechat status").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Check your web access status").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            sender.sendMessage(Component.text("  • /minechat weburl").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Get the web interface URL").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            // Friend Commands
            sender.sendMessage(Component.text("👥 Friend System:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
            sender.sendMessage(Component.text("  • /friend - Show friend commands").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Complete friend management system").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            // Group Commands
            sender.sendMessage(Component.text("👪 Group System:").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)); // yeah the emoji means a group 🫡
            sender.sendMessage(Component.text("  • /group - Show group commands").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Complete group chat system").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            // Private Messages
            sender.sendMessage(Component.text("💬 Private Messaging:").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
            sender.sendMessage(Component.text("  • /msg <player> <message>").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Send private message").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            sender.sendMessage(Component.text("  • /reply <message>").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("    Reply to last message received").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text(""));

            // Admin Commands
            if (sender.hasPermission("minechat.admin")) {
                sender.sendMessage(Component.text("⚙ Admin Commands:").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                sender.sendMessage(Component.text("  • /minechat reload").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("    Reload plugin configuration").color(NamedTextColor.GRAY));
                sender.sendMessage(Component.text(""));
            }

            // Footer
            sender.sendMessage(Component.text("═════════════════════════════").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("💡 Tip: Use /minechat status to check web access").color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("🌐 Aliases: /mc, /mchat").color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("═════════════════════════════").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text(""));

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


        if (password.length() < 4) {
            sender.sendMessage(Component.text("Password must be at least 4 characters long!").color(NamedTextColor.RED));
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
    private boolean handlePrivateMessage(CommandSender sender, String commandName, String[] args) {
        if (!plugin.getConfig().getBoolean("private-messages.enable", true)) {
            sender.sendMessage(Component.text("Private messages are disabled!").color(NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can send private messages!").color(NamedTextColor.RED));
            return true;
        }

        Player senderPlayer = (Player) sender;

        // Handle reply command
        if (isReplyCommand(commandName)) {
            return handleReplyMessage(senderPlayer, args);
        }

        // Handle regular message command
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

        // preventing pm to self, using both UUID and name check for robustness
        if (sender.getName().equalsIgnoreCase(target.getName()) ||
            senderPlayer.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot send a private message to yourself!").color(NamedTextColor.RED));
            return true;
        }

        return sendPrivateMessage(senderPlayer, target, message);
    }

    // Handle reply to last message
    private boolean handleReplyMessage(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /reply <message>").color(NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", args);
        String lastSenderName = lastMessageSenders.get(sender.getUniqueId().toString());

        if (lastSenderName == null) {
            sender.sendMessage(Component.text("You have no recent messages to reply to!").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(lastSenderName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player '" + lastSenderName + "' is no longer online!").color(NamedTextColor.RED));
            return true;
        }

        return sendPrivateMessage(sender, target, message);
    }

    // Send private message and update last sender tracking
    private boolean sendPrivateMessage(Player sender, Player target, String message) {
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

        // Update last message sender for the target (so they can reply)
        lastMessageSenders.put(target.getUniqueId().toString(), sender.getName());

        // Store in db if enabled
        if (plugin.getConfig().getBoolean("chat.enable-logging", true)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.storeChatMessage(
                    sender.getName(),
                    sender.getUniqueId(),
                    "[PM to " + target.getName() + "] " + message,
                    plugin.getServer().getName()
                );
            });
        }

        return true;
    }

    // Check if command is a reply command
    private boolean isReplyCommand(String commandName) {
        return commandName.equalsIgnoreCase("reply") || commandName.equalsIgnoreCase("r");
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
                    String url = plugin.getConfig().getString("web.interface-url", "http://localhost:3000");
                    player.sendMessage(Component.text("🌐 Web Interface: " + url).color(NamedTextColor.AQUA));
                } else {
                    player.sendMessage(Component.text("✗ Web access is disabled. Set a password to enable it.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }
}
