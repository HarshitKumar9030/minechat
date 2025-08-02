package me.harshit.minechat.commands;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.api.GroupInfo;
import me.harshit.minechat.api.GroupMember;
import me.harshit.minechat.api.GroupMessage;
import me.harshit.minechat.api.GroupSettings;
import me.harshit.minechat.database.GroupManager;
import me.harshit.minechat.ranks.RankManager;
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
import java.util.stream.Collectors;


public class GroupCommandHandler implements CommandExecutor, TabCompleter {

    private final Minechat plugin;
    private final GroupManager groupManager;
    private final RankManager rankManager;

    public GroupCommandHandler(Minechat plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
        this.rankManager = plugin.getRankManager();
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
            case "join":
                return handleJoinGroup(player, args);
            case "leave":
                return handleLeaveGroup(player, args);
            case "delete":
            case "disband":
                return handleDeleteGroup(player, args);

            // Member management
            case "invite":
                return handleInvitePlayer(player, args);
            case "accept":
                return handleAcceptInvite(player, args);
            case "deny":
            case "decline":
                return handleDenyInvite(player, args);
            case "kick":
                return handleKickMember(player, args);
            case "ban":
                return handleBanMember(player, args);
            case "unban":
                return handleUnbanMember(player, args);

            case "promote":
                return handlePromoteMember(player, args);
            case "demote":
                return handleDemoteMember(player, args);
            case "admin":
                return handleSetAdmin(player, args);
            case "mod":
            case "moderator":
                return handleSetModerator(player, args);

            case "chat":
            case "msg":
                return handleGroupMessage(player, args);
            case "announce":
            case "announcement":
                return handleAnnouncement(player, args);
            case "motd":
                return handleMotd(player, args);

            case "mute":
                return handleMuteMember(player, args);
            case "unmute":
                return handleUnmuteMember(player, args);
            case "clear":
                return handleClearChat(player, args);

            case "list":
                return handleGroupList(player);
            case "members":
                return handleMembersList(player, args);
            case "info":
                return handleGroupInfo(player, args);
            case "invites":
            case "pending":
                return handlePendingInvites(player);
            case "search":
                return handleSearchGroups(player, args);

            case "settings":
            case "config":
                return handleGroupSettings(player, args);
            case "private":
                return handleSetPrivate(player, args);
            case "public":
                return handleSetPublic(player, args);

            case "help":
                showGroupHelp(player);
                return true;
            case "code":
                return handleInviteCode(player, args);

            default:
                player.sendMessage(Component.text("Unknown group command. Use /group help for help.").color(NamedTextColor.RED));
                return true;
        }
    }

    private void showGroupHelp(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("════════ GROUP COMMANDS ════════").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("Basic Commands:", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("  /group create <name> [description] - Create a new group").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group join <name|code> - Join a public group or by invite code").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group leave [group] - Leave a group").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group list - Show your groups").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group search <query> - Search public groups").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("Messaging:", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("  /group chat <group> <message> - Send a message").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group announce <group> <message> - Send announcement (admin)").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("  /group motd <group> [message] - Set/view message of the day").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text(""));

        if (hasAdminGroups(player)) {
            player.sendMessage(Component.text("Management (Admin/Owner):", NamedTextColor.YELLOW, TextDecoration.BOLD));
            player.sendMessage(Component.text("  /group invite <group> <player> - Invite a player").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group kick <group> <player> - Kick a member").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group ban <group> <player> - Ban a player").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group promote <group> <player> - Promote to admin").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group settings <group> - Manage group settings").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group private <group> - Make group private").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /group public <group> - Make group public").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text(""));
        }

        player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
    }

    private boolean hasAdminGroups(Player player) {
        List<GroupInfo> groups = groupManager.getPlayerGroups(player.getUniqueId());
        return groups.stream().anyMatch(group -> {
            GroupMember member = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
            return member != null && (member.getRole() == GroupMember.GroupRole.OWNER ||
                    member.getRole() == GroupMember.GroupRole.ADMIN);
        });
    }

    private boolean handleCreateGroup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group create <name> [description]").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String description = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "A new group";

        if (groupName.length() < 3 || groupName.length() > 32) {
            player.sendMessage(Component.text("Group name must be between 3 and 32 characters!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupName.matches("^[a-zA-Z0-9_\\s]+$")) {
            player.sendMessage(Component.text("Group name can only contain letters, numbers, spaces, and underscores!").color(NamedTextColor.RED));
            return true;
        }

        if (groupManager.groupExists(groupName)) {
            player.sendMessage(Component.text("A group with that name already exists!").color(NamedTextColor.RED));
            return true;
        }

        int maxGroups = plugin.getConfig().getInt("chat-groups.max-groups-per-player", 5);
        List<GroupInfo> playerGroups = groupManager.getPlayerGroups(player.getUniqueId());
        if (playerGroups.size() >= maxGroups) {
            player.sendMessage(Component.text("You can only be in " + maxGroups + " groups at once!").color(NamedTextColor.RED));
            return true;
        }

        try {
            GroupInfo group = groupManager.createGroup(
                    groupName,
                    description,
                    player.getUniqueId(),
                    player.getName(),
                    plugin.getConfig().getInt("chat-groups.default-max-members", 20)
            );

            player.sendMessage(Component.text("Successfully created group '").color(NamedTextColor.GREEN)
                    .append(Component.text(groupName).color(NamedTextColor.YELLOW))
                    .append(Component.text("'!")));

            if (group.isPrivate()) {
                player.sendMessage(Component.text("Invite code: ").color(NamedTextColor.GRAY)
                        .append(Component.text(group.getSettings().getInviteCode()).color(NamedTextColor.AQUA)));
            }

            return true;
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to create group: " + e.getMessage()).color(NamedTextColor.RED));
            return true;
        }
    }

    private boolean handleJoinGroup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group join <name|code>").color(NamedTextColor.RED));
            return true;
        }

        String identifier = args[1];
        GroupInfo group = null;

        if (identifier.startsWith("GRP-")) {
            group = groupManager.getGroupByInviteCode(identifier);
            if (group == null) {
                player.sendMessage(Component.text("Invalid or expired invite code!").color(NamedTextColor.RED));
                return true;
            }
        } else {
            // Try to find by name
            group = groupManager.getGroupByName(identifier);
            if (group == null) {
                player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
                return true;
            }
        }

        try {
            boolean success = groupManager.joinGroup(group.getGroupId(), player.getUniqueId(), player.getName());
            if (success) {
                player.sendMessage(Component.text("Successfully joined group '").color(NamedTextColor.GREEN)
                        .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                        .append(Component.text("'!")));

                if (!group.getSettings().getJoinMessage().isEmpty()) {
                    groupManager.broadcastToGroup(group.getGroupId(),
                            GroupMessage.createSystemMessage(group.getGroupId(),
                                    player.getName() + " " + group.getSettings().getJoinMessage()));
                }
            } else {
                player.sendMessage(Component.text("Failed to join group. You may already be a member or the group is full.").color(NamedTextColor.RED));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error joining group: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleInvitePlayer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group invite <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(Component.text("You can't invite yourself!").color(NamedTextColor.RED));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Target player lookup from db
            UUID targetUUID = plugin.getUserDataManager().getPlayerUUIDByName(targetName);

            if (targetUUID == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Player '" + targetName + "' has never joined this server!").color(NamedTextColor.RED));
                });
                return;
            }

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
                    targetUUID, targetName);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(Component.text("✓ Invitation sent to " + targetName + "!").color(NamedTextColor.GREEN));

                    // Notify target player if they're online
                    Player target = Bukkit.getPlayerExact(targetName);
                    if (target != null && target.isOnline()) {
                        Component inviteMessage = Component.text("🎉 " + player.getName() + " invited you to group '" + groupName + "'! ")
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
                        player.sendMessage(Component.text("They will be notified when they come online.").color(NamedTextColor.GRAY));
                    }
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
        List<Document> playerGroups = groupManager.getPlayerGroupsAsDocuments(playerId);
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
            List<Document> groups = groupManager.getPlayerGroupsAsDocuments(player.getUniqueId());

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

    private boolean handlePromoteMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group promote <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember adminMember = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (adminMember == null || !adminMember.getRole().canManageGroup()) {
            player.sendMessage(Component.text("You don't have permission to promote members in this group!").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : groupManager.getPlayerUUID(targetName);

        if (targetUUID == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember targetMember = groupManager.getGroupMember(group.getGroupId(), targetUUID);
        if (targetMember == null) {
            player.sendMessage(Component.text("Player is not a member of this group!").color(NamedTextColor.RED));
            return true;
        }

        try {
            GroupMember.GroupRole newRole = targetMember.getRole() == GroupMember.GroupRole.MEMBER ?
                    GroupMember.GroupRole.ADMIN : GroupMember.GroupRole.ADMIN;

            boolean success = groupManager.updateMemberRole(group.getGroupId(), targetUUID, newRole);
            if (success) {
                player.sendMessage(Component.text("Successfully promoted ").color(NamedTextColor.GREEN)
                        .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" to " + newRole.getDisplayName())));

                if (target != null) {
                    target.sendMessage(Component.text("You have been promoted to ").color(NamedTextColor.GREEN)
                            .append(Component.text(newRole.getDisplayName()).color(NamedTextColor.YELLOW))
                            .append(Component.text(" in group "))
                            .append(Component.text(group.getGroupName()).color(NamedTextColor.AQUA)));
                }

                groupManager.broadcastToGroup(group.getGroupId(),
                        GroupMessage.createSystemMessage(group.getGroupId(),
                                targetName + " has been promoted to " + newRole.getDisplayName()));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to promote member: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleKickMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group kick <group> <player> [reason]").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember adminMember = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (adminMember == null || !adminMember.getRole().canModerate()) {
            player.sendMessage(Component.text("You don't have permission to kick members!").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : groupManager.getPlayerUUID(targetName);

        if (targetUUID == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can't kick yourself!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember targetMember = groupManager.getGroupMember(group.getGroupId(), targetUUID);
        if (targetMember == null) {
            player.sendMessage(Component.text("Player is not a member of this group!").color(NamedTextColor.RED));
            return true;
        }

        if (targetMember.getRole().getPriority() >= adminMember.getRole().getPriority()) {
            player.sendMessage(Component.text("You can't kick someone with equal or higher permissions!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.kickMember(group.getGroupId(), targetUUID, player.getUniqueId(), reason);
            if (success) {
                player.sendMessage(Component.text("Successfully kicked ").color(NamedTextColor.GREEN)
                        .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" from the group!")));

                if (target != null) {
                    target.sendMessage(Component.text("You have been kicked from group ").color(NamedTextColor.RED)
                            .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                            .append(Component.text("\nReason: " + reason).color(NamedTextColor.GRAY)));
                }

                groupManager.broadcastToGroup(group.getGroupId(),
                        GroupMessage.createSystemMessage(group.getGroupId(),
                                targetName + " has been kicked from the group"));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to kick member: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleBanMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group ban <group> <player> [reason]").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason provided";

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember adminMember = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (adminMember == null || !adminMember.getRole().canManageGroup()) {
            player.sendMessage(Component.text("You don't have permission to ban members!").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : groupManager.getPlayerUUID(targetName);

        if (targetUUID == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.banMember(group.getGroupId(), targetUUID, player.getUniqueId(), reason);
            if (success) {
                player.sendMessage(Component.text("Successfully banned ").color(NamedTextColor.GREEN)
                        .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" from the group!")));

                if (target != null) {
                    target.sendMessage(Component.text("You have been banned from group ").color(NamedTextColor.RED)
                            .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                            .append(Component.text("\nReason: " + reason).color(NamedTextColor.GRAY)));
                }

                groupManager.broadcastToGroup(group.getGroupId(),
                        GroupMessage.createSystemMessage(group.getGroupId(),
                                targetName + " has been banned from the group"));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to ban member: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleMuteMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group mute <group> <player> [duration]").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];
        int duration = args.length > 3 ? Integer.parseInt(args[3]) : 60; // Default 60 minutes

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember adminMember = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (adminMember == null || !adminMember.getRole().canModerate()) {
            player.sendMessage(Component.text("You don't have permission to mute members!").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : groupManager.getPlayerUUID(targetName);

        if (targetUUID == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.muteMember(group.getGroupId(), targetUUID, player.getUniqueId(), duration);
            if (success) {
                player.sendMessage(Component.text("Successfully muted ").color(NamedTextColor.GREEN)
                        .append(Component.text(targetName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" for " + duration + " minutes!")));

                if (target != null) {
                    target.sendMessage(Component.text("You have been muted in group ").color(NamedTextColor.YELLOW)
                            .append(Component.text(group.getGroupName()).color(NamedTextColor.AQUA))
                            .append(Component.text(" for " + duration + " minutes").color(NamedTextColor.GRAY)));
                }
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to mute member: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleDeleteGroup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group delete <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!group.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the group owner can delete the group!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.deleteGroup(group.getGroupId(), player.getUniqueId());
            if (success) {
                player.sendMessage(Component.text("Successfully deleted group '").color(NamedTextColor.GREEN)
                        .append(Component.text(groupName).color(NamedTextColor.YELLOW))
                        .append(Component.text("'!")));

                groupManager.broadcastToGroup(group.getGroupId(),
                        GroupMessage.createSystemMessage(group.getGroupId(),
                                "This group has been disbanded by " + player.getName()));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to delete group: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleSetPrivate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group private <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember member = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (member == null || !member.getRole().canManageGroup()) {
            player.sendMessage(Component.text("You don't have permission to change group settings!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.updateGroupPrivacy(group.getGroupId(), true);
            if (success) {
                player.sendMessage(Component.text("Group '").color(NamedTextColor.GREEN)
                        .append(Component.text(groupName).color(NamedTextColor.YELLOW))
                        .append(Component.text("' is now private!")));

                GroupSettings settings = group.getSettings();
                player.sendMessage(Component.text("Invite code: ").color(NamedTextColor.GRAY)
                        .append(Component.text(settings.getInviteCode()).color(NamedTextColor.AQUA)));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to update group: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleSetPublic(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group public <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember member = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (member == null || !member.getRole().canManageGroup()) {
            player.sendMessage(Component.text("You don't have permission to change group settings!").color(NamedTextColor.RED));
            return true;
        }

        try {
            boolean success = groupManager.updateGroupPrivacy(group.getGroupId(), false);
            if (success) {
                player.sendMessage(Component.text("Group '").color(NamedTextColor.GREEN)
                        .append(Component.text(groupName).color(NamedTextColor.YELLOW))
                        .append(Component.text("' is now public!")));
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to update group: " + e.getMessage()).color(NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleInviteCode(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group code <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        // Check if player is member
        GroupMember member = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (member == null) {
            player.sendMessage(Component.text("You're not a member of this group!").color(NamedTextColor.RED));
            return true;
        }

        // Check if group allows invites
        if (!group.getSettings().isAllowInvites() ||
                (!group.getSettings().isMembersCanInvite() && !member.getRole().canManageGroup())) {
            player.sendMessage(Component.text("Invites are disabled for this group!").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Invite code for '").color(NamedTextColor.GREEN)
                .append(Component.text(groupName).color(NamedTextColor.YELLOW))
                .append(Component.text("': "))
                .append(Component.text(group.getSettings().getInviteCode()).color(NamedTextColor.AQUA)));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "create", "join", "leave", "delete", "invite", "accept", "deny", "kick", "ban", "unban",
                    "promote", "demote", "admin", "mod", "chat", "announce", "motd", "mute", "unmute", "clear",
                    "list", "members", "info", "invites", "search", "settings", "private", "public", "help", "code"
            );

            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            List<String> groupCommands = Arrays.asList("join", "leave", "invite", "kick", "ban", "promote",
                    "chat", "announce", "info", "members", "settings", "private", "public");

            if (groupCommands.contains(args[0].toLowerCase())) {
                List<GroupInfo> groups = groupManager.getPlayerGroups(player.getUniqueId());
                for (GroupInfo group : groups) {
                    if (group.getGroupName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(group.getGroupName());
                    }
                }
            }
        } else if (args.length == 3) {
            // third argument, often player names for management commands
            List<String> playerCommands = Arrays.asList("invite", "kick", "ban", "promote", "demote", "mute", "unmute");

            if (playerCommands.contains(args[0].toLowerCase())) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }

        return completions;
    }

    private boolean handleUnbanMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group unban <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to unban members!").color(NamedTextColor.RED));
            return true;
        }

        UUID targetId = groupManager.getPlayerUUID(targetName);
        if (targetId == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        // Remove from banned list (implementation would need to be added to GroupManager)
        player.sendMessage(Component.text("Player unbanned from group!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleDemoteMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group demote <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isGroupOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("Only the group owner can demote members!").color(NamedTextColor.RED));
            return true;
        }

        UUID targetId = groupManager.getPlayerUUID(targetName);
        if (targetId == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        // Implementation would need to be added to GroupManager
        player.sendMessage(Component.text("Player demoted!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetAdmin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group admin <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isGroupOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("Only the group owner can set admins!").color(NamedTextColor.RED));
            return true;
        }

        UUID targetId = groupManager.getPlayerUUID(targetName);
        if (targetId == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        groupManager.promoteGroupMember(group.getGroupId(), targetId);
        player.sendMessage(Component.text("Player promoted to admin!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetModerator(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group mod <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to set moderators!").color(NamedTextColor.RED));
            return true;
        }

        UUID targetId = groupManager.getPlayerUUID(targetName);
        if (targetId == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        // Implementation would need to be added to GroupManager
        player.sendMessage(Component.text("Player promoted to moderator!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAnnouncement(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group announce <group> <message>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String announcement = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to make announcements!").color(NamedTextColor.RED));
            return true;
        }

        groupManager.updateGroupAnnouncement(group.getGroupId(), announcement);
        player.sendMessage(Component.text("Announcement updated!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMotd(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group motd <group> <message>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String motd = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to set MOTD!").color(NamedTextColor.RED));
            return true;
        }

        // todo: implementation would need to be added to GroupManager
        player.sendMessage(Component.text("MOTD updated!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleUnmuteMember(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /group unmute <group> <player>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];
        String targetName = args[2];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to unmute members!").color(NamedTextColor.RED));
            return true;
        }

        UUID targetId = groupManager.getPlayerUUID(targetName);
        if (targetId == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }

        // todo: implementation would need to be added to GroupManager
        player.sendMessage(Component.text("Player unmuted!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleClearChat(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group clear <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to clear chat!").color(NamedTextColor.RED));
            return true;
        }

        // Implementation would need to be added to GroupManager
        player.sendMessage(Component.text("Group chat cleared!").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMembersList(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group members <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        GroupMember memberCheck = groupManager.getGroupMember(group.getGroupId(), player.getUniqueId());
        if (memberCheck == null) {
            player.sendMessage(Component.text("You're not a member of this group!").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Members of ").color(NamedTextColor.GREEN)
                .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                .append(Component.text(":")));

        for (GroupMember member : group.getMembers()) {
            Component memberComponent = Component.text("  • ").color(NamedTextColor.GRAY)
                    .append(Component.text(member.getPlayerName()).color(member.isOnline() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .append(Component.text(" (").color(NamedTextColor.GRAY))
                    .append(Component.text(member.getRole().getDisplayName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(")").color(NamedTextColor.GRAY));
            player.sendMessage(memberComponent);
        }

        return true;
    }

    private boolean handleSearchGroups(Player player, String[] args) {
        String query = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";

        List<Document> groupDocs = groupManager.searchPublicGroups(query, 10);
        List<GroupInfo> groups = groupDocs.stream()
                .map(this::convertDocumentToGroupInfo)
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            player.sendMessage(Component.text("No public groups found!").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Public Groups:").color(NamedTextColor.GREEN));

        for (GroupInfo group : groups) {
            Component groupComponent = Component.text("  • ").color(NamedTextColor.GRAY)
                    .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" (").color(NamedTextColor.GRAY))
                    .append(Component.text(group.getMembers().size()).color(NamedTextColor.WHITE))
                    .append(Component.text("/").color(NamedTextColor.GRAY))
                    .append(Component.text(group.getMaxMembers()).color(NamedTextColor.WHITE))
                    .append(Component.text(")").color(NamedTextColor.GRAY));

            if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                groupComponent = groupComponent.append(Component.text(" - ").color(NamedTextColor.GRAY))
                        .append(Component.text(group.getDescription()).color(NamedTextColor.WHITE));
            }

            player.sendMessage(groupComponent);
        }

        return true;
    }

    private boolean handleGroupSettings(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /group settings <group>").color(NamedTextColor.RED));
            return true;
        }

        String groupName = args[1];

        GroupInfo group = groupManager.getGroupByName(groupName);
        if (group == null) {
            player.sendMessage(Component.text("Group not found!").color(NamedTextColor.RED));
            return true;
        }

        if (!groupManager.isAdminOrOwner(group.getGroupId(), player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have permission to view settings!").color(NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Settings for ").color(NamedTextColor.GREEN)
                .append(Component.text(group.getGroupName()).color(NamedTextColor.YELLOW))
                .append(Component.text(":")));

        player.sendMessage(Component.text("  Private: ").color(NamedTextColor.GRAY)
                .append(Component.text(group.isPrivate()).color(group.isPrivate() ? NamedTextColor.RED : NamedTextColor.GREEN)));

        player.sendMessage(Component.text("  Max Members: ").color(NamedTextColor.GRAY)
                .append(Component.text(group.getMaxMembers()).color(NamedTextColor.WHITE)));

        return true;
    }

    private GroupInfo convertDocumentToGroupInfo(Document doc) {
        if (doc == null) return null;

        try {
            UUID groupId = UUID.fromString(doc.getString("groupId"));
            String groupName = doc.getString("groupName");
            String description = doc.getString("description");
            UUID ownerId = UUID.fromString(doc.getString("ownerId"));
            String ownerName = doc.getString("ownerName");
            boolean isPrivate = doc.getBoolean("isPrivate", false);
            int maxMembers = doc.getInteger("maxMembers", 25);
            long createdDate = doc.getLong("createdDate");

            List<Document> memberDocs = doc.getList("members", Document.class);
            List<GroupMember> members = memberDocs.stream()
                    .map(this::convertDocumentToGroupMember)
                    .collect(Collectors.toList());

            Document settingsDoc = doc.get("settings", Document.class);
            GroupSettings settings = convertDocumentToGroupSettings(settingsDoc);

            return new GroupInfo(groupId, groupName, description, ownerId, ownerName,
                    members, java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(createdDate),
                    java.time.ZoneId.systemDefault()),
                    isPrivate, maxMembers, settings);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupInfo: " + e.getMessage());
            return null;
        }
    }

    private GroupMember convertDocumentToGroupMember(Document doc) {
        if (doc == null) return null;

        try {
            UUID playerId = UUID.fromString(doc.getString("playerId"));
            String playerName = doc.getString("playerName");
            String roleStr = doc.getString("role");
            long joinedDate = doc.getLong("joinedDate");

            GroupMember.GroupRole role = GroupMember.GroupRole.valueOf(roleStr.toUpperCase());
            boolean isOnline = Bukkit.getPlayer(playerId) != null;

            return new GroupMember(playerId, playerName, role,
                    java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(joinedDate),
                            java.time.ZoneId.systemDefault()),
                    isOnline);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupMember: " + e.getMessage());
            return null;
        }
    }

    private GroupSettings convertDocumentToGroupSettings(Document doc) {
        if (doc == null) {
            // Return default settings if document is null
            return GroupSettings.getDefault();
        }

        try {
            String inviteCode = doc.getString("inviteCode");
            boolean allowInvites = doc.getBoolean("allowInvites", true);
            boolean friendsOnly = doc.getBoolean("friendsOnly", false);
            boolean muteNonMembers = doc.getBoolean("muteNonMembers", false);
            boolean logMessages = doc.getBoolean("logMessages", true);
            boolean webAccessEnabled = doc.getBoolean("webAccessEnabled", true);
            boolean joinRequiresApproval = doc.getBoolean("joinRequiresApproval", false);
            boolean membersCanInvite = doc.getBoolean("membersCanInvite", true);
            boolean onlyAdminsCanMessage = doc.getBoolean("onlyAdminsCanMessage", false);
            boolean enableAnnouncements = doc.getBoolean("enableAnnouncements", true);
            String joinMessage = doc.getString("joinMessage");
            String leaveMessage = doc.getString("leaveMessage");
            String groupMotd = doc.getString("groupMotd");

            // Handle null values with defaults
            if (joinMessage == null) joinMessage = "Welcome to the group!";
            if (leaveMessage == null) leaveMessage = "Thanks for being part of the group!";
            if (groupMotd == null) groupMotd = "";
            if (inviteCode == null) inviteCode = "";

            // Handle lists
            List<String> allowedRanks = doc.getList("allowedRanks", String.class);
            if (allowedRanks == null) allowedRanks = new ArrayList<>();

            List<String> mutedMemberStrings = doc.getList("mutedMembers", String.class);
            List<UUID> mutedMembers = new ArrayList<>();
            if (mutedMemberStrings != null) {
                for (String uuidStr : mutedMemberStrings) {
                    try {
                        mutedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in mutedMembers: " + uuidStr);
                    }
                }
            }

            List<String> bannedMemberStrings = doc.getList("bannedMembers", String.class);
            List<UUID> bannedMembers = new ArrayList<>();
            if (bannedMemberStrings != null) {
                for (String uuidStr : bannedMemberStrings) {
                    try {
                        bannedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in bannedMembers: " + uuidStr);
                    }
                }
            }

            return new GroupSettings(
                allowInvites,
                friendsOnly,
                muteNonMembers,
                logMessages,
                webAccessEnabled,
                joinRequiresApproval,
                membersCanInvite,
                onlyAdminsCanMessage,
                enableAnnouncements,
                joinMessage,
                leaveMessage,
                allowedRanks,
                mutedMembers,
                bannedMembers,
                groupMotd,
                inviteCode
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to convert document to GroupSettings: " + e.getMessage());
            return GroupSettings.getDefault(); // Return default settings on error
        }
    }
}
