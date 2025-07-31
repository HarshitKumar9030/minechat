package me.harshit.minechat.database;

import me.harshit.minechat.Minechat;
import org.bson.Document;
import org.bukkit.Bukkit;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bukkit.entity.Player;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// manages user data related to web access and passwords
public class UserDataManager {

    private final MongoCollection<Document> userCollection;
    private final Minechat plugin;

    public UserDataManager(MongoDatabase database, Minechat plugin) {
        this.userCollection = database.getCollection("user_data");
        this.plugin = plugin;
    }

    /**
     * @param playerUUID Player's UUID
     * @param playerName Player's name
     * @param password Plain text password (hashed later)
     * @return true if successful
     */


    public boolean setWebPassword(UUID playerUUID, String playerName, String password) {
        try {
            // Hash
            String hashedPassword = hashPassword(password);

            Document userDoc = new Document()
                    .append("playerUUID", playerUUID.toString())
                    .append("playerName", playerName)
                    .append("webPassword", hashedPassword)
                    .append("webAccessEnabled", true)
                    .append("lastUpdated", System.currentTimeMillis());

            userCollection.replaceOne(
                new Document("playerUUID", playerUUID.toString()),
                userDoc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set web password for " + playerName + ": " + e.getMessage());
            return false;
        }
    }

   // Verifies the web password for a player

    public boolean verifyWebPassword(String playerName, String password) {
        try {
            Document userDoc = userCollection.find(new Document("playerName", playerName)).first();

            if (userDoc == null || !userDoc.getBoolean("webAccessEnabled", false)) {
                return false;
            }

            String storedHash = userDoc.getString("webPassword");
            String inputHash = hashPassword(password);

            return storedHash != null && storedHash.equals(inputHash);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to verify web password for " + playerName + ": " + e.getMessage());
            return false;
        }
    }

   // Checks if a player has web access enabled

    public boolean hasWebAccess(String playerName) {
        try {
            Document userDoc = userCollection.find(new Document("playerName", playerName)).first();
            return userDoc != null && userDoc.getBoolean("webAccessEnabled", false);
        } catch (Exception e) {
            return false;
        }
    }

    // Disables web access for a player
    // Returns true if successful, false otherwise

    public boolean disableWebAccess(UUID playerUUID) {
        try {
            userCollection.updateOne(
                new Document("playerUUID", playerUUID.toString()),
                new Document("$set", new Document("webAccessEnabled", false))
            );
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to disable web access: " + e.getMessage());
            return false;
        }
    }


    public UUID getPlayerUUIDByName(String playerName) {
        try {
            Document userDoc = userCollection.find(new Document("playerName", playerName)).first();
            if (userDoc != null) {
                return UUID.fromString(userDoc.getString("playerUUID"));
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to look up player UUID for " + playerName + ": " + e.getMessage());
            return null;
        }
    }

    // returns true if player has joined even once means they exist in the db
    public boolean playerExists(String playerName) {
        try {
            Document userDoc = userCollection.find(new Document("playerName", playerName)).first();
            return userDoc != null;
        } catch (Exception e) {
            plugin.getLogger().warning( "Failed to check if player exists " + playerName + ": " + e.getMessage());
            return false;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to hash password: " + e.getMessage());
            return null;
        }
    }

    // cache player data  when they join
    public void cachePlayerRank(UUID playerUUID, String playerName, String cleanRank, String formattedRank) {
        try {
            Document rankData = new Document()
                    .append("cleanRank", cleanRank)
                    .append("formattedRank", formattedRank)
                    .append("lastUpdated", System.currentTimeMillis());

            userCollection.updateOne(
                new Document("playerUUID", playerUUID.toString()),
                new Document("$set", rankData),
                new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache rank data for " + playerName + ": " + e.getMessage());
        }
    }


    private Document getCachedRankData(Document userDoc) {
        String cleanRank = userDoc.getString("cleanRank");
        String formattedRank = userDoc.getString("formattedRank");

        // If no cached data exists, use default
        if (cleanRank == null || formattedRank == null) {
            return new Document()
                    .append("cleanRank", "[CHAT]")
                    .append("formattedRank", "§8[§7CHAT§8] ");
        }

        return new Document()
                .append("cleanRank", cleanRank)
                .append("formattedRank", formattedRank);
    }

    public List<Document> getAllRanks() {
        try {
            List<Document> users = new ArrayList<>();
            for (Document userDoc : userCollection.find()) {
                String playerName = userDoc.getString("playerName");
                String playerUUID = userDoc.getString("playerUUID");
                Player player = Bukkit.getPlayerExact(playerName);
                
                Document rankDoc = new Document()
                        .append("playerName", playerName)
                        .append("playerUUID", playerUUID);

                if (player != null) {
                    String cleanRank = plugin.getRankManager().getCleanRank(player);
                    String coloredFormattedRank = plugin.getRankManager().getFormattedRank(player);

                    cachePlayerRank(UUID.fromString(playerUUID), playerName, cleanRank, coloredFormattedRank);

                    rankDoc.append("rank", cleanRank)
                           .append("formattedRank", coloredFormattedRank)
                           .append("online", true);
                } else {
                    Document cachedRank = getCachedRankData(userDoc);

                    rankDoc.append("rank", cachedRank.getString("cleanRank"))
                           .append("formattedRank", cachedRank.getString("formattedRank"))
                           .append("online", false);
                }

                users.add(rankDoc);
            }
            return users;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get all ranks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    
    public Document getUserData(UUID playerUUID) {
        try {
            Document userDoc = userCollection.find(new Document("playerUUID", playerUUID.toString())).first();
            if (userDoc != null) {
                String playerName = userDoc.getString("playerName");
                Player player = Bukkit.getPlayerExact(playerName);
                
                // Add rank information
                if (player != null) {
                    String cleanRank = plugin.getRankManager().getCleanRank(player);
                    String coloredFormattedRank = plugin.getRankManager().getFormattedRank(player);

                    cachePlayerRank(playerUUID, playerName, cleanRank, coloredFormattedRank);

                    userDoc.append("rank", cleanRank)
                           .append("formattedRank", coloredFormattedRank)
                           .append("online", true);
                } else {
                    Document cachedRank = getCachedRankData(userDoc);
                    userDoc.append("rank", cachedRank.getString("cleanRank"))
                           .append("formattedRank", cachedRank.getString("formattedRank"))
                           .append("online", false);
                }
            }
            return userDoc;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get user data for " + playerUUID + ": " + e.getMessage());
            return new Document();
        }
    }


    public String getPlayerNameByUUID(UUID playerUUID) {
        try {
            Document userDoc = userCollection.find(new Document("playerUUID", playerUUID.toString())).first();
            if (userDoc != null) {
                return userDoc.getString("playerName");
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player name for UUID " + playerUUID + ": " + e.getMessage());
            return null;
        }
    }

    public List<Document> getAllPlayers() {
        try {
            List<Document> players = new ArrayList<>();
            for (Document userDoc : userCollection.find()) {
                String playerName = userDoc.getString("playerName");
                String playerUUID = userDoc.getString("playerUUID");
                Player player = Bukkit.getPlayerExact(playerName);

                Document playerDoc = new Document()
                        .append("playerName", playerName)
                        .append("playerUUID", playerUUID)
                        .append("webAccessEnabled", userDoc.getBoolean("webAccessEnabled", false))
                        .append("lastUpdated", userDoc.getLong("lastUpdated"));

                if (player != null && player.isOnline()) {
                    String cleanRank = plugin.getRankManager().getCleanRank(player);
                    String coloredFormattedRank = plugin.getRankManager().getFormattedRank(player);

                    // cache the current rank data
                    cachePlayerRank(UUID.fromString(playerUUID), playerName, cleanRank, coloredFormattedRank);

                    playerDoc.append("rank", cleanRank)
                           .append("formattedRank", coloredFormattedRank)
                           .append("lastSeen", System.currentTimeMillis());
                } else {
                    // use cached rank data for offline players
                    Document cachedRank = getCachedRankData(userDoc);
                    playerDoc.append("rank", cachedRank.getString("cleanRank"))
                           .append("formattedRank", cachedRank.getString("formattedRank"))
                           .append("lastSeen", userDoc.getLong("lastSeen"));
                }

                players.add(playerDoc);
            }
            return players;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get all players: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
