package me.harshit.minechat.listeners;

import me.harshit.minechat.Minechat;
import me.harshit.minechat.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.List;
import java.util.regex.Pattern;

// in a nut this class handles all the chat related events
public class ChatListener implements Listener {

    private final Minechat plugin;
    private final DatabaseManager databaseManager;

    public ChatListener(Minechat plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        // Convert the Component message to plain text for processing
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
            filteredMessage = applyChatFilter(originalMessage);
        }

        // Make final for lambda expression
        final String finalFilteredMessage = filteredMessage;

        // Store in database (async to not block the server)
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

        // Create a beautiful chat format
        String chatFormat = plugin.getConfig().getString("chat.format",
            "&7[&bChat&7] &f{player}&7: &f{message}");

        // Replace placeholders
        // Todo: Mention these placeholders in the config and the main file
        String formattedMessage = chatFormat
            .replace("{player}", player.getName())
            .replace("{message}", finalFilteredMessage);

        // Convert color codes and create Component message
        Component finalMessage = Component.text(formattedMessage.replace("&", "§"));

        // Cancel the original and set the new message
        event.setCancelled(true);

        // Send to all online players with beautiful formatting
        plugin.getServer().broadcast(finalMessage);

        // Log to console with timestamp
        // This could be optional but I think it's useful for debugging
        plugin.getLogger().info(String.format("[CHAT] %s: %s", player.getName(), finalFilteredMessage));
    }

    // Apply chat filter to the message
    private String applyChatFilter(String message) {
        List<String> filteredWords = plugin.getConfig().getStringList("chat.filtered-words");

        String filtered = message;
        for (String word : filteredWords) {
            if (word != null && !word.trim().isEmpty()) {
                // Replace with asterisks (case insensitive)
                String replacement = "*".repeat(word.length());
                filtered = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE)
                        .matcher(filtered)
                        .replaceAll(replacement);
            }
        }

        return filtered;
    }
}
