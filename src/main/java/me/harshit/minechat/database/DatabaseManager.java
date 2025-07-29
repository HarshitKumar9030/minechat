package me.harshit.minechat.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// This class manages the connection to MongoDB and provides methods to store and retrieve chat messages
public class DatabaseManager {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> chatCollection;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // connect using the conn string from config
    public boolean connect() {
        try {
            // Get connection details from config
            String connectionString = plugin.getConfig().getString("mongodb.connection-string");
            String databaseName = plugin.getConfig().getString("mongodb.database-name");
            String collectionName = plugin.getConfig().getString("mongodb.collection-name");

            plugin.getLogger().info("Connecting to MongoDB...");

            mongoClient = MongoClients.create(connectionString);

            database = mongoClient.getDatabase(databaseName);
            chatCollection = database.getCollection(collectionName); // get the collection and db we defined in the config

            plugin.getLogger().info("Successfully connected to MongoDB!");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MongoDB: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stores a chat message in the database
     * @param playerName Name of the player who sent the message
     * @param playerUUID UUID of the player
     * @param message The chat message content
     * @param serverName Name of the server
     */

    public void storeChatMessage(String playerName, UUID playerUUID, String message, String serverName) {
        try {
            Document chatDoc = new Document()
                    .append("playerName", playerName)
                    .append("playerUUID", playerUUID.toString())
                    .append("message", message)
                    .append("serverName", serverName)
                    .append("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append("date", System.currentTimeMillis()); // For easy sorting by date

            chatCollection.insertOne(chatDoc);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to store chat message: " + e.getMessage());
        }
    }

    /**
     * Retrieves recent chat messages
     * @param limit Maximum number of messages to retrieve
     * @return List of chat message documents
     */

    public List<Document> getRecentMessages(int limit) {
        try {
            List<Document> messages = new ArrayList<>();

            // Get messages sorted by date (newest first)
            chatCollection.find()
                    .sort(new Document("date", -1))
                    .limit(limit)
                    .into(messages);

            return messages;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to retrieve chat messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Gets chat messages from a specific player
     * @param playerName Name of the player
     * @param limit Maximum number of messages
     * @return List of messages from that player
     */

    public List<Document> getPlayerMessages(String playerName, int limit) {
        try {
            List<Document> messages = new ArrayList<>();

            chatCollection.find(new Document("playerName", playerName))
                    .sort(new Document("date", -1))
                    .limit(limit)
                    .into(messages);

            return messages;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to retrieve player messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

   // close db connection
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("Disconnected from MongoDB");
        }
    }

    // true if connected else false
    public boolean isConnected() {
        try {
            if (mongoClient == null) return false;

            // try: ping db
            database.runCommand(new Document("ping", 1));
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // this is mainly for the UserDataManager to get the db instance
    // so it can access the user collection
    public MongoDatabase getDatabase() {
        return database;
    }
}
