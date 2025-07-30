package me.harshit.minechat.ranks;

import me.harshit.minechat.Minechat;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Manages player ranks and prefixes from various permission plugins
 * Supports: LuckPerms, PowerRanks, PermissionsEx, GroupManager, and more via Vault
 */
public class RankManager {

    private final Minechat plugin;
    private Chat vaultChat;
    private Permission vaultPermission;
    private boolean vaultEnabled = false;

    // Direct plugin support flags
    private boolean luckPermsEnabled = false;
    private boolean powerRanksEnabled = false;

    public RankManager(Minechat plugin) {
        this.plugin = plugin;
        setupVault();
        detectPlugins();
    }

    // Initialize Vault and check for chat/permission providers
    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found - rank support will be limited");
            return;
        }

        RegisteredServiceProvider<Chat> chatProvider = plugin.getServer().getServicesManager().getRegistration(Chat.class);
        RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(Permission.class);

        if (chatProvider != null) {
            vaultChat = chatProvider.getProvider();
            vaultEnabled = true;
        }

        if (permissionProvider != null) {
            vaultPermission = permissionProvider.getProvider();
        }
    }

    // detects which plugin is available
    private void detectPlugins() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsEnabled = true;
        }

        if (plugin.getServer().getPluginManager().getPlugin("PowerRanks") != null) {
            powerRanksEnabled = true;
        }

        if (vaultEnabled || luckPermsEnabled || powerRanksEnabled) {
             plugin.getLogger().info("✓ Rank system integration enabled");
        } else {
            plugin.getLogger().warning("No supported permission plugin found - using default ranks");
        }
    }



    public String getPlayerRank(Player player) {
        String prefix = getPlayerPrefix(player);
        String suffix = getPlayerSuffix(player);

        StringBuilder rankBuilder = new StringBuilder();

        if (!prefix.isEmpty()) {
            prefix = formatRankWithColors(prefix);
            if (!prefix.isEmpty()) {
                rankBuilder.append(prefix);
            }
        } else {
            String group = getPlayerGroup(player);
            if (!group.isEmpty() && !group.equalsIgnoreCase("default")) {
                String coloredRank = getDefaultRankColor(group) + group.toUpperCase();
                rankBuilder.append("§8[").append(coloredRank).append("§8]");
            } else {
                rankBuilder.append("§8[§7CHAT§8]");
            }
        }

        if (!suffix.isEmpty() && !suffix.equalsIgnoreCase("none")) {
            suffix = cleanAndFormatSuffix(suffix);
            if (!suffix.isEmpty() && !suffix.equalsIgnoreCase("none")) {
                rankBuilder.append(" ").append(suffix);
            }
        }

        return rankBuilder.toString();
    }


    public String getFormattedRank(Player player) {
        String rank = getPlayerRank(player);
        return rank.isEmpty() ? "" : rank + " ";
    }

    public String getFormattedPlayerName(Player player) {
        return "§7" + player.getName();
    }


    public String formatChatMessage(String message) {
        return "§f" + message;
    }


    public String getFormattedChatLine(Player player, String message) {
        StringBuilder chatLine = new StringBuilder();

        String rank = getFormattedRank(player);
        if (!rank.isEmpty()) {
            chatLine.append(rank);
        }

        chatLine.append(getFormattedPlayerName(player));

        chatLine.append(": ").append(formatChatMessage(message));

        return chatLine.toString();
    }


    private String formatRankWithColors(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "";

        prefix = prefix.trim();
        prefix = prefix.replace("&", "§");

        // remove existing brackets if present to add our custom ones
        prefix = prefix.replaceAll("[\\[\\]]", "");

        if (!prefix.contains("§")) {
            prefix = getDefaultRankColor(prefix) + prefix;
        }

        // custom dark gray brackets (ideally #1A1A1A which is closest to §8)
        return "§8[" + prefix + "§8]";
    }


    private String getDefaultRankColor(String rank) {
        String lowerRank = rank.toLowerCase();

        if (lowerRank.contains("owner") || lowerRank.contains("admin")) {
            return "§c";
        }
        else if (lowerRank.contains("mod") || lowerRank.contains("staff")) {
            return "§5";
        }
        else if (lowerRank.contains("vip") || lowerRank.contains("mvp") || lowerRank.contains("plus") || lowerRank.contains("premium")) {
            return "§6";
        }
        else if (lowerRank.contains("helper") || lowerRank.contains("support")) {
            return "§a";
        }
        else if (lowerRank.contains("builder") || lowerRank.contains("architect")) {
            return "§b";
        }
        else if (lowerRank.contains("youtube") || lowerRank.contains("creator")) {
            return "§c";
        }
        else if (lowerRank.contains("donator") || lowerRank.contains("donor")) {
            return "§e";
        }
        else if (lowerRank.contains("beta") || lowerRank.contains("tester")) {
            return "§d";
        }
        else if (lowerRank.contains("member") || lowerRank.contains("player")) {
            return "§f";
        }
        else {
            return "§7";
        }
    }


    private String cleanAndFormatSuffix(String suffix) {
        if (suffix == null) return "";

        suffix = suffix.trim();
        suffix = suffix.replace("&", "§");
        suffix = suffix.replaceAll("\\s+", " ");

        return suffix;
    }


    public boolean isRankSystemAvailable() {
        return vaultEnabled || luckPermsEnabled || powerRanksEnabled;
    }


    public String getDebugInfo() {
        return "Rank System Status:\n" +
                "- Vault: " + (vaultEnabled ? "✓" : "✗") + "\n" +
                "- LuckPerms: " + (luckPermsEnabled ? "✓" : "✗") + "\n" +
                "- PowerRanks: " + (powerRanksEnabled ? "✓" : "✗") + "\n";
    }


    private String getPlayerPrefix(Player player) {
        try {
            if (luckPermsEnabled) {
                String prefix = getLuckPermsPrefix(player);
                if (!prefix.isEmpty()) {
                    return prefix;
                }
            }

            if (powerRanksEnabled) {
                String prefix = getPowerRanksPrefix(player);
                if (!prefix.isEmpty()) {
                    return prefix;
                }
            }

            if (vaultEnabled && vaultChat != null) {
                String prefix = vaultChat.getPlayerPrefix(player);
                if (prefix != null && !prefix.isEmpty()) {
                    return prefix;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player prefix for " + player.getName() + ": " + e.getMessage());
        }

        return "";
    }


    private String getPlayerSuffix(Player player) {
        try {
            if (luckPermsEnabled) {
                String suffix = getLuckPermsSuffix(player);
                if (!suffix.isEmpty()) {
                    return suffix;
                }
            }

            if (powerRanksEnabled) {
                String suffix = getPowerRanksSuffix(player);
                if (!suffix.isEmpty()) {
                    return suffix;
                }
            }

            if (vaultEnabled && vaultChat != null) {
                String suffix = vaultChat.getPlayerSuffix(player);
                if (suffix != null && !suffix.isEmpty()) {
                    return suffix;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player suffix for " + player.getName() + ": " + e.getMessage());
        }

        return "";
    }


    private String getPlayerGroup(Player player) {
        try {
            if (luckPermsEnabled) {
                String group = getLuckPermsGroup(player);
                if (!group.isEmpty()) {
                    return group;
                }
            }

            if (powerRanksEnabled) {
                String group = getPowerRanksGroup(player);
                if (!group.isEmpty()) {
                    return group;
                }
            }

            if (vaultEnabled && vaultPermission != null) {
                String group = vaultPermission.getPrimaryGroup(player);
                if (group != null && !group.isEmpty()) {
                    return group;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player group for " + player.getName() + ": " + e.getMessage());
        }

        return "default";
    }


    private String getLuckPermsPrefix(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                Class<?> luckPermsProvider = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object luckPerms = luckPermsProvider.getMethod("get").invoke(null);

                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());

                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String prefix = (String) metaData.getClass().getMethod("getPrefix").invoke(metaData);
                    return prefix != null ? prefix : "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting LuckPerms prefix: " + e.getMessage());
        }
        return "";
    }


    private String getLuckPermsSuffix(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                Class<?> luckPermsProvider = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object luckPerms = luckPermsProvider.getMethod("get").invoke(null);

                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());

                if (user != null) {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
                    String suffix = (String) metaData.getClass().getMethod("getSuffix").invoke(metaData);
                    return suffix != null ? suffix : "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting LuckPerms suffix: " + e.getMessage());
        }
        return "";
    }


    private String getLuckPermsGroup(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                Class<?> luckPermsProvider = Class.forName("net.luckperms.api.LuckPermsProvider");
                Object luckPerms = luckPermsProvider.getMethod("get").invoke(null);

                Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
                Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());

                if (user != null) {
                    String primaryGroup = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
                    return primaryGroup != null ? primaryGroup : "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting LuckPerms group: " + e.getMessage());
        }
        return "";
    }


    private String getPowerRanksPrefix(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PowerRanks")) {
                Class<?> powerRanksAPI = Class.forName("nl.svenar.powerranks.PowerRanks");
                Object instance = powerRanksAPI.getMethod("getInstance").invoke(null);
                Object playerManager = powerRanksAPI.getMethod("getPlayerManager").invoke(instance);

                Class<?> playerManagerClass = Class.forName("nl.svenar.powerranks.players.PowerRanksPlayer");
                Object powerPlayer = playerManagerClass.getMethod("getPlayerExact", String.class).invoke(playerManager, player.getName());

                if (powerPlayer != null) {
                    String prefix = (String) powerPlayer.getClass().getMethod("getPrefix").invoke(powerPlayer);
                    return prefix != null ? prefix : "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting PowerRanks prefix: " + e.getMessage());
        }
        return "";
    }


    private String getPowerRanksSuffix(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PowerRanks")) {
                Class<?> powerRanksAPI = Class.forName("nl.svenar.powerranks.PowerRanks");
                Object instance = powerRanksAPI.getMethod("getInstance").invoke(null);
                Object playerManager = powerRanksAPI.getMethod("getPlayerManager").invoke(instance);

                Class<?> playerManagerClass = Class.forName("nl.svenar.powerranks.players.PowerRanksPlayer");
                Object powerPlayer = playerManagerClass.getMethod("getPlayerExact", String.class).invoke(playerManager, player.getName());

                if (powerPlayer != null) {
                    String suffix = (String) powerPlayer.getClass().getMethod("getSuffix").invoke(powerPlayer);
                    return suffix != null ? suffix : "";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting PowerRanks suffix: " + e.getMessage());
        }
        return "";
    }


    private String getPowerRanksGroup(Player player) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PowerRanks")) {
                Class<?> powerRanksAPI = Class.forName("nl.svenar.powerranks.PowerRanks");
                Object instance = powerRanksAPI.getMethod("getInstance").invoke(null);
                Object playerManager = powerRanksAPI.getMethod("getPlayerManager").invoke(instance);

                Class<?> playerManagerClass = Class.forName("nl.svenar.powerranks.players.PowerRanksPlayer");
                Object powerPlayer = playerManagerClass.getMethod("getPlayerExact", String.class).invoke(playerManager, player.getName());

                if (powerPlayer != null) {
                    Object rank = powerPlayer.getClass().getMethod("getRank").invoke(powerPlayer);
                    if (rank != null) {
                        String rankName = (String) rank.getClass().getMethod("getName").invoke(rank);
                        return rankName != null ? rankName : "";
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting PowerRanks group: " + e.getMessage());
        }
        return "";
    }

    // clean rank for api
    public String getCleanRank(Player player) {
        String prefix = getPlayerPrefix(player);

        if (!prefix.isEmpty()) {
            // Clean the prefix of color codes and brackets
            String cleanPrefix = prefix.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\[\\]]", "").trim();
            if (!cleanPrefix.isEmpty()) {
                return "[" + cleanPrefix.toUpperCase() + "]";
            }
        }

        // Fallback: try to get group name as rank
        String group = getPlayerGroup(player);
        if (!group.isEmpty() && !group.equalsIgnoreCase("default")) {
            return "[" + group.toUpperCase() + "]";
        }

        // Final fallback
        return "[CHAT]";
    }


    public String getCleanFormattedRank(Player player) {
        return getCleanRank(player) + " ";
    }
}
