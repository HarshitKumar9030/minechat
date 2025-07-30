package me.harshit.minechat.listeners;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.DatabaseManager;
import me.harshit.minechat.ranks.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.List;
import java.util.regex.Pattern;

// in a nut this class handles all the chat related events
public class ChatListener implements Listener {

    private final Minechat plugin;
    private final DatabaseManager databaseManager;
    private final RankManager rankManager;

    public ChatListener(Minechat plugin, DatabaseManager databaseManager, RankManager rankManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.rankManager = rankManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // cache the player's rank data when they join
        // this ensures offline players show their actual last known rank
        try {
            String cleanRank = rankManager.getCleanRank(player);
            String formattedRank = rankManager.getFormattedRank(player);

            plugin.getUserDataManager().cachePlayerRank(
                player.getUniqueId(),
                player.getName(),
                cleanRank,
                formattedRank
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache rank data for " + player.getName() + " on join: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        // convert the component message to plain text for processing
        String originalMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Check if chat logging is enabled
        if (!plugin.getConfig().getBoolean("chat.enable-logging", true)) {
            return;
        }

        // check the damn message length :p
        int maxLength = plugin.getConfig().getInt("chat.max-message-length", 256);
        if (originalMessage.length() > maxLength) {
            player.sendMessage(Component.text("Your message is too long! Maximum length is " + maxLength + " characters.")
                    .color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Apply chat filter if enabled
        // This is filter from the words that are defined in the config
        String filteredMessage = originalMessage;
        if (plugin.getConfig().getBoolean("chat.enable-filter", true)) {
            List<String> bannedWords = plugin.getConfig().getStringList("chat.filter.banned-words");
            String replacement = plugin.getConfig().getString("chat.filter.replacement", "***");

            for (String word : bannedWords) {
                Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(word));
                filteredMessage = pattern.matcher(filteredMessage).replaceAll(replacement);
            }
        }

        // if custom format is enabled
        if (plugin.getConfig().getBoolean("chat.enable-custom-format", true)) {
            // cancel original event and use the custom format
            event.setCancelled(true);

            // get format from config or use default
            String chatFormat = plugin.getConfig().getString("chat.format", "{rank}{player}: {message}");

            String playerRank = rankManager.getFormattedRank(player);

            String displayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());

            String formattedMessage = chatFormat
                    .replace("{rank}", playerRank)
                    .replace("{player}", player.getName())
                    .replace("{message}", filteredMessage)
                    .replace("{displayname}", displayName)
                    .replace("&", "§"); // Convert color codes

            Component finalMessage = Component.text(formattedMessage);

            // broadcast the custom formatted message
            plugin.getServer().broadcast(finalMessage);
        } else {
            // If custom format is disabled, just update the message with filtered content
            if (!filteredMessage.equals(originalMessage)) {
                event.message(Component.text(filteredMessage));
            }
        }

        // Store in db for web sync (async)
        if (databaseManager != null) {
            String finalFilteredMessage = filteredMessage;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    databaseManager.storeChatMessage(
                            player.getName(),
                            player.getUniqueId(),
                            finalFilteredMessage,
                            plugin.getServer().getName()
                    );
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to store chat message: " + e.getMessage());
                }
            });
        }
    }
}
