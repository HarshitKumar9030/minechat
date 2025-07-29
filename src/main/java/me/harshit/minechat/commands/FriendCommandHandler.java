package me.harshit.minechat.commands;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.FriendManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// friends command handler
public class FriendCommandHandler implements CommandExecutor, TabCompleter {

    private final Minechat plugin;
    private final FriendManager friendManager;

    public FriendCommandHandler(Minechat plugin, FriendManager friendManager) {
        this.plugin = plugin;
        this.friendManager = friendManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use friend commands!").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getConfig().getBoolean("friends.enable", true)) {
            sender.sendMessage(Component.text("Friend system is disabled!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showFriendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
            case "invite":
                return handleAddFriend(player, args);

            case "accept":
                return handleAcceptFriend(player, args);

            case "deny":
            case "decline":
                return handleDenyFriend(player, args);

            case "remove":
            case "delete":
                return handleRemoveFriend(player, args);

            case "list":
                return handleFriendList(player);

            case "requests":
            case "pending":
                return handlePendingRequests(player);

            case "help":
                showFriendHelp(player);
                return true;

            default:
                player.sendMessage(Component.text("Unknown friend command. Use /friend help for help.").color(NamedTextColor.RED));
                return true;
        }
    }


    private void showFriendHelp(Player player) {
        // Show comprehensive friend help with better formatting
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("════════════════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("               Friend System               ").color(NamedTextColor.GOLD).bold());
        player.sendMessage(Component.text("════════════════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("👥 Friend Management:").color(NamedTextColor.YELLOW).bold());
        player.sendMessage(Component.text("  • /friend add <player>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Send a friend request to a player").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /friend accept <player>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Accept a friend request").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /friend deny <player>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Deny a friend request").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /friend remove <player>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Remove a friend from your list").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("📋 Information Commands:").color(NamedTextColor.BLUE).bold());
        player.sendMessage(Component.text("  • /friend list").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    View your current friends list").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /friend requests").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    View pending friend requests").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        // Footer
        player.sendMessage(Component.text("════════════════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("💡 Tip: Click [Accept] or [Deny] buttons in requests").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("🎯 Aliases: /friends, /f").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("════════════════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
    }


    private boolean handleAddFriend(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /friend add <player>").color(NamedTextColor.RED));
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player '" + targetName + "' is not online!").color(NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't add yourself as a friend!").color(NamedTextColor.RED));
            return true;
        }

        // Check friend limit (unless player has unlimited permission)
        if (!player.hasPermission("minechat.friends.unlimited")) {
            int maxFriends = plugin.getConfig().getInt("friends.max-friends", 50);
            if (friendManager.getFriendCount(player.getUniqueId()) >= maxFriends) {
                player.sendMessage(Component.text("You've reached the maximum number of friends (" + maxFriends + ")!").color(NamedTextColor.RED));
                return true;
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = friendManager.sendFriendRequest(
                player.getUniqueId(), player.getName(),
                target.getUniqueId(), target.getName()
            );

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Friend request sent to " + target.getName() + "!").color(NamedTextColor.GREEN));

                    // Notify the target player
                    Component requestMessage = Component.text("🤝 " + player.getName() + " sent you a friend request! ")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("[Accept]")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/friend accept " + player.getName())))
                            .append(Component.text(" "))
                            .append(Component.text("[Deny]")
                                    .color(NamedTextColor.RED)
                                    .clickEvent(ClickEvent.runCommand("/friend deny " + player.getName())));

                    target.sendMessage(requestMessage);
                } else {
                    if (friendManager.areFriends(player.getUniqueId(), target.getUniqueId())) {
                        player.sendMessage(Component.text("You're already friends with " + target.getName() + "!").color(NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(Component.text("Friend request already pending or failed to send.").color(NamedTextColor.RED));
                    }
                }
            });
        });

        return true;
    }


    private boolean handleAcceptFriend(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /friend accept <player>").color(NamedTextColor.RED));
            return true;
        }

        String senderName = args[1];
        Player sender = Bukkit.getPlayerExact(senderName);

        if (sender == null) {
            player.sendMessage(Component.text("Player '" + senderName + "' not found!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = friendManager.acceptFriendRequest(player.getUniqueId(), sender.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ You're now friends with " + sender.getName() + "!").color(NamedTextColor.GREEN));

                    if (sender.isOnline()) {
                        sender.sendMessage(Component.text("✓ " + player.getName() + " accepted your friend request!").color(NamedTextColor.GREEN));
                    }
                } else {
                    player.sendMessage(Component.text("No pending friend request from " + senderName + ".").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }


    private boolean handleDenyFriend(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /friend deny <player>").color(NamedTextColor.RED));
            return true;
        }

        String senderName = args[1];
        Player sender = Bukkit.getPlayerExact(senderName);

        if (sender == null) {
            player.sendMessage(Component.text("Player '" + senderName + "' not found!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = friendManager.denyFriendRequest(player.getUniqueId(), sender.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Friend request from " + senderName + " denied.").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("No pending friend request from " + senderName + ".").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }


    private boolean handleRemoveFriend(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /friend remove <player>").color(NamedTextColor.RED));
            return true;
        }

        String friendName = args[1];
        Player friend = Bukkit.getPlayerExact(friendName);

        if (friend == null) {
            player.sendMessage(Component.text("Player '" + friendName + "' not found!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = friendManager.removeFriend(player.getUniqueId(), friend.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Removed " + friendName + " from your friend list.").color(NamedTextColor.YELLOW));

                    if (friend.isOnline()) {
                        friend.sendMessage(Component.text("📝 " + player.getName() + " removed you from their friend list.").color(NamedTextColor.GRAY));
                    }
                } else {
                    player.sendMessage(Component.text("You're not friends with " + friendName + ".").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }


    private boolean handleFriendList(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> friends = friendManager.getFriendList(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (friends.isEmpty()) {
                    player.sendMessage(Component.text("You don't have any friends yet. Use /friend add <player> to add friends!").color(NamedTextColor.GRAY));
                    return;
                }

                player.sendMessage(Component.text("=== Your Friends (" + friends.size() + ") ===").color(NamedTextColor.AQUA));

                for (Document friend : friends) {
                    String friendName = friend.getString("friendName");
                    Player onlineFriend = Bukkit.getPlayerExact(friendName);

                    Component statusIcon;
                    if (onlineFriend != null && onlineFriend.isOnline()) {
                        statusIcon = Component.text("🟢 ").color(NamedTextColor.GREEN);
                    } else {
                        statusIcon = Component.text("🔴 ").color(NamedTextColor.RED);
                    }

                    Component friendLine = statusIcon
                            .append(Component.text(friendName).color(NamedTextColor.WHITE))
                            .append(Component.text(" [Remove]")
                                    .color(NamedTextColor.RED)
                                    .clickEvent(ClickEvent.runCommand("/friend remove " + friendName)));

                    player.sendMessage(friendLine);
                }
            });
        });

        return true;
    }


    private boolean handlePendingRequests(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> requests = friendManager.getPendingRequests(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (requests.isEmpty()) {
                    player.sendMessage(Component.text("You don't have any pending friend requests.").color(NamedTextColor.GRAY));
                    return;
                }

                player.sendMessage(Component.text("=== Pending Friend Requests (" + requests.size() + ") ===").color(NamedTextColor.AQUA));

                for (Document request : requests) {
                    String senderName = request.getString("senderName");

                    Component requestLine = Component.text("🤝 " + senderName + " ")
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("[Accept]")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/friend accept " + senderName)))
                            .append(Component.text(" "))
                            .append(Component.text("[Deny]")
                                    .color(NamedTextColor.RED)
                                    .clickEvent(ClickEvent.runCommand("/friend deny " + senderName)));

                    player.sendMessage(requestLine);
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "accept", "deny", "remove", "list", "requests", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            Player player = (Player) sender;

            if (subCommand.equals("add")) {
                // Tab complete players who aren't friends and don't have pending requests
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(sender)) {
                        // Quick check - not perfect but good enough for tab completion
                        if (!friendManager.areFriends(player.getUniqueId(), onlinePlayer.getUniqueId())) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            } else if (subCommand.equals("accept") || subCommand.equals("deny")) {
                // Tab complete players who sent pending requests
                try {
                    List<Document> requests = friendManager.getPendingRequests(player.getUniqueId());
                    for (Document request : requests) {
                        String senderName = request.getString("senderName");
                        if (senderName != null) {
                            completions.add(senderName);
                        }
                    }
                } catch (Exception e) {
                    // Fallback to online players if database lookup fails
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(sender)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            } else if (subCommand.equals("remove")) {
                // Tab complete current friends
                try {
                    List<Document> friends = friendManager.getFriendList(player.getUniqueId());
                    for (Document friend : friends) {
                        String friendName = friend.getString("friendName");
                        if (friendName != null) {
                            completions.add(friendName);
                        }
                    }
                } catch (Exception e) {
                    // Fallback to online players if database lookup fails
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(sender)) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                }
            }
        }

        return completions;
    }
}
