package me.harshit.minechat.listeners;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.UserDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// This class was much needed tbh as we need to cache player data when they join and update it when they leave.
public class PlayerDataListener implements Listener {

    private final Minechat plugin;
    private final UserDataManager userDataManager;

    public PlayerDataListener(Minechat plugin, UserDataManager userDataManager) {
        this.plugin = plugin;
        this.userDataManager = userDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // cache player data when they join (without web password initially)
        String playerName = event.getPlayer().getName();
        String playerUUID = event.getPlayer().getUniqueId().toString();

        String cleanRank = plugin.getRankManager().getCleanRank(event.getPlayer());
        String formattedRank = plugin.getRankManager().getFormattedRank(event.getPlayer());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            userDataManager.cachePlayerData(
                event.getPlayer().getUniqueId(),
                playerName,
                cleanRank,
                formattedRank
            );
        });

        plugin.getLogger().info("Cached player data for " + playerName);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // update last seen timestamp when player leaves
        String playerName = event.getPlayer().getName();

        String cleanRank = plugin.getRankManager().getCleanRank(event.getPlayer());
        String formattedRank = plugin.getRankManager().getFormattedRank(event.getPlayer());

        // data to be updated asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            userDataManager.updatePlayerData(
                event.getPlayer().getUniqueId(),
                playerName,
                cleanRank,
                formattedRank,
                System.currentTimeMillis() // lastSeen timestamp
            );
        });
    }
}
