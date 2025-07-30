package me.harshit.minechat.database;

import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

// manages user data related to web access and passwords
public class UserDataManager {

    private final MongoCollection<Document> userCollection;
    private final JavaPlugin plugin;

    public UserDataManager(MongoDatabase database, JavaPlugin plugin) {
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
            plugin.getLogger().warning("Failed to check if player exists " + playerName + ": " + e.getMessage());
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
}
