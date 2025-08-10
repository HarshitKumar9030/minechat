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

public class DatabaseManager {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> chatCollection;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
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


    public List<Document> getRecentMessages(int limit) {
        try {
            List<Document> messages = new ArrayList<>();

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

    public void storePrivateMessage(String senderName, UUID senderUUID, String targetName, UUID targetUUID, String message, String source) {
        try {
            Document privateMessageDoc = new Document()
                    .append("type", "private_message")
                    .append("senderName", senderName)
                    .append("senderUUID", senderUUID.toString())
                    .append("targetName", targetName)
                    .append("targetUUID", targetUUID.toString())
                    .append("message", message)
                    .append("source", source) // "web" or "minecraft"
                    .append("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append("date", System.currentTimeMillis()); 

            chatCollection.insertOne(privateMessageDoc);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to store private message: " + e.getMessage());
        }
    }

    public List<Document> getPrivateMessages(String player1, String player2, int limit) {
        try {
            List<Document> messages = new ArrayList<>();

            Document query = new Document("$and", List.of(
                new Document("type", "private_message"),
                new Document("$or", List.of(
                    new Document("$and", List.of(
                        new Document("senderName", player1),
                        new Document("targetName", player2)
                    )),
                    new Document("$and", List.of(
                        new Document("senderName", player2),
                        new Document("targetName", player1)
                    ))
                ))
            ));

            chatCollection.find(query)
                    .sort(new Document("date", 1)) 
                    .limit(limit)
                    .into(messages);

            return messages;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to retrieve private messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("Disconnected from MongoDB");
        }
    }

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
