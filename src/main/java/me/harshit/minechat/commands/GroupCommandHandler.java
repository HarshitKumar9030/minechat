package me.harshit.minechat.commands;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.GroupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.UUID;

// group command handler for managing chat groups

public class GroupCommandHandler implements CommandExecutor, TabCompleter {

    private final Minechat plugin;
    private final GroupManager groupManager;

    public GroupCommandHandler(Minechat plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use group commands!").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getConfig().getBoolean("chat-groups.enable", true)) {
            sender.sendMessage(Component.text("Chat groups are disabled!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showGroupHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreateGroup(player, args);

            case "invite":
                return handleInvitePlayer(player, args);

            case "accept":
                return handleAcceptInvite(player, args);

            case "deny":
            case "decline":
                return handleDenyInvite(player, args);

            case "leave":
                return handleLeaveGroup(player, args);

            case "list":
                return handleGroupList(player);

            case "invites":
            case "pending":
                return handlePendingInvites(player);

            case "chat":
            case "msg":
                return handleGroupMessage(player, args);

            case "info":
                return handleGroupInfo(player, args);

            case "help":
                showGroupHelp(player);
                return true;

            default:
                player.sendMessage(Component.text("Unknown group command. Use /group help for help.").color(NamedTextColor.RED));
                return true;
        }
    }

    // todo reduce "=" by 5 and add spaces

    private void showGroupHelp(Player player) {
        // Show comprehensive group help with better formatting
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("         Group System         ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("👪 Group Management:").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD)); // the emoji ded
        player.sendMessage(Component.text("  • /group create <name> [description]").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Create a new chat group").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /group invite <group> <player>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Invite a player to your group").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /group accept <group>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Accept a group invitation").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /group deny <group>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Deny a group invitation").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /group leave <group>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Leave a group you're in").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("💬 Communication:").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("  • /group chat <group> <message>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    Send a message to group members").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("📋 Information Commands:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("  • /group list").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    View your groups").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("  • /group info <group>").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("    View detailed group information").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("══════════════════════════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
    }

    private boolean handleCreateGroup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group create <name> [description]").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String description = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "A chat group";

        if (groupName.length() < 3 || groupName.length() > 20) {
            player.sendMessage(Component.text("Group name must be between 3-20 characters!").color(NamedTextColor.RED));
            return true;
        }

        int maxGroups = plugin.getConfig().getInt("chat-groups.max-groups-per-player", 10);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> playerGroups = groupManager.getPlayerGroups(player.getUniqueId());

            if (playerGroups.size() >= maxGroups && !player.hasPermission("minechat.groups.unlimited")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("You've reached the maximum number of groups (" + maxGroups + ")!").color(NamedTextColor.RED));
                });
                return;
            }

            boolean success = groupManager.createGroup(player.getUniqueId(), player.getName(), groupName, description, false);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Group '" + groupName + "' created successfully!").color(NamedTextColor.GREEN));
                    player.sendMessage(Component.text("Use /group invite " + groupName + " <player> to invite friends!").color(NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("✗ Failed to create group. Name might already exist.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleInvitePlayer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group invite <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player '" + targetName + "' is not online!").color(NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't invite yourself!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find the group
            Document group = findGroupByName(groupName, player.getUniqueId());
            if (group == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Group '" + groupName + "' not found or you're not a member!").color(NamedTextColor.RED));
                });
                return;
            }

            UUID groupId = UUID.fromString(group.getString("groupId"));
            boolean success = groupManager.sendGroupInvite(groupId, player.getUniqueId(), player.getName(),
                                                         target.getUniqueId(), target.getName());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Invitation sent to " + target.getName() + "!").color(NamedTextColor.GREEN));

                    // Notify the target player
                    Component inviteMessage = Component.text("🎉 " + player.getName() + " invited you to group '" + groupName + "'! ") // god I hate emojis T_T
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("[Accept]")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/group accept " + groupName)))
                            .append(Component.text(" "))
                            .append(Component.text("[Deny]")
                                    .color(NamedTextColor.RED)
                                    .clickEvent(ClickEvent.runCommand("/group deny " + groupName)));

                    target.sendMessage(inviteMessage);
                } else {
                    player.sendMessage(Component.text("✗ Failed to send invitation. Player might already be invited.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleGroupMessage(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group chat <group> <message>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document group = findGroupByName(groupName, player.getUniqueId());
            if (group == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Group '" + groupName + "' not found or you're not a member!").color(NamedTextColor.RED));
                });
                return;
            }

            UUID groupId = UUID.fromString(group.getString("groupId"));

            // Store message for web sync, which idk when I will implement todo
            groupManager.storeGroupMessage(groupId, player.getUniqueId(), player.getName(), message, "minecraft");

            // Format and send message to all group members
            String format = plugin.getConfig().getString("chat-groups.format",
                "&7[&aGroup: &b{group}&7] &f{player}&7: &f{message}");
            String formattedMessage = format
                    .replace("{group}", groupName)
                    .replace("{player}", player.getName())
                    .replace("{message}", message);

            Component messageComponent = Component.text(formattedMessage.replace("&", "§"));

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Send to all online group members
                List<Document> members = group.getList("members", Document.class);
                for (Document member : members) {
                    String memberName = member.getString("playerName");
                    Player onlineMember = Bukkit.getPlayerExact(memberName);
                    if (onlineMember != null && onlineMember.isOnline()) {
                        onlineMember.sendMessage(messageComponent);
                    }
                }
            });
        });

        return true;
    }

    private Document findGroupByName(String groupName, UUID playerId) {
        // used to find group by name of a player
        List<Document> playerGroups = groupManager.getPlayerGroups(playerId);
        return playerGroups.stream()
                .filter(group -> group.getString("groupName").equalsIgnoreCase(groupName))
                .findFirst()
                .orElse(null);
    }

    private boolean handleAcceptInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group accept <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find pending invite for this group
            List<Document> invites = groupManager.getPendingGroupInvites(player.getUniqueId());
            Document invite = invites.stream()
                    .filter(inv -> {
                        String groupId = inv.getString("groupId");
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        return group != null && group.getString("groupName").equalsIgnoreCase(groupName);
                    })
                    .findFirst()
                    .orElse(null);

            if (invite == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("No pending invite found for group '" + groupName + "'!").color(NamedTextColor.RED));
                });
                return;
            }

            UUID groupId = UUID.fromString(invite.getString("groupId"));
            boolean success = groupManager.acceptGroupInvite(player.getUniqueId(), groupId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ You've joined group '" + groupName + "'!").color(NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("✗ Failed to join group. It might be full.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleDenyInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group deny <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find and remove pending invite
            List<Document> invites = groupManager.getPendingGroupInvites(player.getUniqueId());
            boolean found = invites.stream()
                    .anyMatch(inv -> {
                        String groupId = inv.getString("groupId");
                        Document group = groupManager.getGroup(UUID.fromString(groupId));
                        return group != null && group.getString("groupName").equalsIgnoreCase(groupName);
                    });

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (found) {
                    player.sendMessage(Component.text("✓ Group invitation denied.").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("No pending invite found for group '" + groupName + "'!").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleLeaveGroup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group leave <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document group = findGroupByName(groupName, player.getUniqueId());
            if (group == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Group '" + groupName + "' not found or you're not a member!").color(NamedTextColor.RED));
                });
                return;
            }

            UUID groupId = UUID.fromString(group.getString("groupId"));
            boolean success = groupManager.leaveGroup(player.getUniqueId(), groupId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ You've left group '" + groupName + "'.").color(NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("✗ Failed to leave group. You might be the owner.").color(NamedTextColor.RED));
                }
            });
        });

        return true;
    }

    private boolean handleGroupList(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> groups = groupManager.getPlayerGroups(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (groups.isEmpty()) {
                    player.sendMessage(Component.text("You're not a member of any groups. Use /group create <name> to create one!").color(NamedTextColor.GRAY));
                    return;
                }

                player.sendMessage(Component.text("=== Your Groups (" + groups.size() + ") ===").color(NamedTextColor.AQUA));
                player.sendMessage("");
                for (Document group : groups) {
                    String groupName = group.getString("groupName");
                    List<Document> members = group.getList("members", Document.class);
                    int maxMembers = group.getInteger("maxMembers", 25);

                    Component groupLine = Component.text("📁 " + groupName + " (" + members.size() + "/" + maxMembers + ") ")
                            .color(NamedTextColor.WHITE)
                            .append(Component.text("[Chat]")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/group chat " + groupName + " ")))
                            .append(Component.text(" [Info]")
                                    .color(NamedTextColor.BLUE)
                                    .clickEvent(ClickEvent.runCommand("/group info " + groupName)));

                    player.sendMessage(groupLine);
                }
            });
        });

        return true;
    }

    private boolean handlePendingInvites(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Document> invites = groupManager.getPendingGroupInvites(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (invites.isEmpty()) {
                    player.sendMessage(Component.text("You don't have any pending group invitations.").color(NamedTextColor.GRAY));
                    return;
                }

                player.sendMessage(Component.text("=== Pending Group Invitations (" + invites.size() + ") ===").color(NamedTextColor.AQUA));

                for (Document invite : invites) {
                    String inviterName = invite.getString("inviterName");
                    String groupId = invite.getString("groupId");
                    Document group = groupManager.getGroup(UUID.fromString(groupId));

                    if (group != null) {
                        String groupName = group.getString("groupName");

                        Component inviteLine = Component.text("🎉 " + inviterName + " invited you to '" + groupName + "' ")
                                .color(NamedTextColor.YELLOW)
                                .append(Component.text("[Accept]")
                                        .color(NamedTextColor.GREEN)
                                        .clickEvent(ClickEvent.runCommand("/group accept " + groupName)))
                                .append(Component.text(" "))
                                .append(Component.text("[Deny]")
                                        .color(NamedTextColor.RED)
                                        .clickEvent(ClickEvent.runCommand("/group deny " + groupName)));

                        player.sendMessage(inviteLine);
                    }
                }
            });
        });

        return true;
    }

    private boolean handleGroupInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group info <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document group = findGroupByName(groupName, player.getUniqueId());
            if (group == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Group '" + groupName + "' not found or you're not a member!").color(NamedTextColor.RED));
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(Component.text("=== Group Info: " + groupName + " ===").color(NamedTextColor.AQUA));
                player.sendMessage("");
                player.sendMessage(Component.text("Description: " + group.getString("description")).color(NamedTextColor.GRAY));
                player.sendMessage(Component.text("Owner: " + group.getString("ownerName")).color(NamedTextColor.YELLOW));

                List<Document> members = group.getList("members", Document.class);
                int maxMembers = group.getInteger("maxMembers", 25);
                player.sendMessage(Component.text("Members: " + members.size() + "/" + maxMembers).color(NamedTextColor.WHITE));
                player.sendMessage("");

                player.sendMessage(Component.text("=== Members ===").color(NamedTextColor.AQUA));
                for (Document member : members) {
                    String memberName = member.getString("playerName");
                    String role = member.getString("role");
                    Player onlineMember = Bukkit.getPlayerExact(memberName);

                    Component statusIcon = onlineMember != null && onlineMember.isOnline() ?
                            Component.text("🟢 ").color(NamedTextColor.GREEN) :
                            Component.text("🔴 ").color(NamedTextColor.RED);

                    player.sendMessage(statusIcon
                            .append(Component.text(memberName + " (" + role + ")").color(NamedTextColor.WHITE)));
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "accept", "deny", "leave", "list", "invites", "chat", "info", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            Player player = (Player) sender;

            if (subCommand.equals("invite") || subCommand.equals("chat") || subCommand.equals("info")) {
                List<Document> playerGroups = groupManager.getPlayerGroups(player.getUniqueId());
                for (Document group : playerGroups) {
                    String groupName = group.getString("groupName");
                    if (groupName != null) {
                        completions.add(groupName);
                    }
                }
            } else if (subCommand.equals("accept") || subCommand.equals("deny")) {
                List<Document> invites = groupManager.getPendingGroupInvites(player.getUniqueId());
                for (Document invite : invites) {
                    String groupId = invite.getString("groupId");
                    Document group = groupManager.getGroup(UUID.fromString(groupId));
                    if (group != null) {
                        String groupName = group.getString("groupName");
                        if (groupName != null) {
                            completions.add(groupName);
                        }
                    }
                }
            } else if (subCommand.equals("leave")) {
                List<Document> playerGroups = groupManager.getPlayerGroups(player.getUniqueId());
                for (Document group : playerGroups) {
                    String groupName = group.getString("groupName");
                    if (groupName != null && !group.getString("ownerId").equals(player.getUniqueId().toString())) {
                        completions.add(groupName);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            // Tab complete online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
