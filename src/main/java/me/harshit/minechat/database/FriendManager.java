package me.harshit.minechat.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// This class manages friend requests and friendships between players
// It allows sending, accepting, denying friend requests, and managing friendships


public class FriendManager {

    private final MongoCollection<Document> friendsCollection;
    private final MongoCollection<Document> friendRequestsCollection;
    private final JavaPlugin plugin;

    public FriendManager(MongoDatabase database, JavaPlugin plugin) {
        this.friendsCollection = database.getCollection("friends");
        this.friendRequestsCollection = database.getCollection("friend_requests");
        this.plugin = plugin;
    }

    /**
     * Sends a friend request from one player to another
     * @param senderUUID UUID of the player sending the request
     * @param senderName Name of the sender
     * @param targetUUID UUID of the target player
     * @param targetName Name of the target player
     * @return true if request was sent successfully
     */


    public boolean sendFriendRequest(UUID senderUUID, String senderName, UUID targetUUID, String targetName) {
        try {
            if (areFriends(senderUUID, targetUUID)) {
                return false; // Already friends
            }

            Document existingRequest = friendRequestsCollection.find(
                new Document("senderUUID", senderUUID.toString())
                    .append("targetUUID", targetUUID.toString())
            ).first();

            if (existingRequest != null) {
                return false; // Request already exists
            }

            Document requestDoc = new Document()
                    .append("senderUUID", senderUUID.toString())
                    .append("senderName", senderName)
                    .append("targetUUID", targetUUID.toString())
                    .append("targetName", targetName)
                    .append("timestamp", System.currentTimeMillis())
                    .append("status", "pending");

            friendRequestsCollection.insertOne(requestDoc);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send friend request: " + e.getMessage());
            return false;
        }
    }


    public boolean acceptFriendRequest(UUID targetUUID, UUID senderUUID) {
        try {
            Document request = friendRequestsCollection.find(
                new Document("senderUUID", senderUUID.toString())
                    .append("targetUUID", targetUUID.toString())
                    .append("status", "pending")
            ).first();

            if (request == null) {
                return false; // no pending request found
            }

            // Create friendship (bidirectional)
            long timestamp = System.currentTimeMillis();

            Document friendship1 = new Document()
                    .append("playerUUID", senderUUID.toString())
                    .append("friendUUID", targetUUID.toString())
                    .append("playerName", request.getString("senderName"))
                    .append("friendName", request.getString("targetName"))
                    .append("timestamp", timestamp);

            Document friendship2 = new Document()
                    .append("playerUUID", targetUUID.toString())
                    .append("friendUUID", senderUUID.toString())
                    .append("playerName", request.getString("targetName"))
                    .append("friendName", request.getString("senderName"))
                    .append("timestamp", timestamp);

            friendsCollection.insertOne(friendship1);
            friendsCollection.insertOne(friendship2);

            // Remove the friend request
            friendRequestsCollection.deleteOne(
                new Document("senderUUID", senderUUID.toString())
                    .append("targetUUID", targetUUID.toString())
            );

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to accept friend request: " + e.getMessage());
            return false;
        }
    }


    public boolean denyFriendRequest(UUID targetUUID, UUID senderUUID) {
        try {
            friendRequestsCollection.deleteOne(
                new Document("senderUUID", senderUUID.toString())
                    .append("targetUUID", targetUUID.toString())
                    .append("status", "pending")
            );
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deny friend request: " + e.getMessage());
            return false;
        }
    }


    public boolean rejectFriendRequest(UUID targetUUID, UUID senderUUID) {
        return denyFriendRequest(targetUUID, senderUUID);
    }


    public boolean removeFriend(UUID playerUUID, UUID friendUUID) {
        try {
            // Remove both directions of the friendship
            friendsCollection.deleteOne(
                new Document("playerUUID", playerUUID.toString())
                    .append("friendUUID", friendUUID.toString())
            );

            friendsCollection.deleteOne(
                new Document("playerUUID", friendUUID.toString())
                    .append("friendUUID", playerUUID.toString())
            );

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove friend: " + e.getMessage());
            return false;
        }
    }


    public List<Document> getFriendList(UUID playerUUID) {
        try {
            List<Document> friends = new ArrayList<>();
            friendsCollection.find(new Document("playerUUID", playerUUID.toString()))
                    .into(friends);
            return friends;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get friend list: " + e.getMessage());
            return new ArrayList<>();
        }
    }



    public List<Document> getIncomingFriendRequests(UUID playerUUID) {
        try {
            List<Document> requests = new ArrayList<>();
            friendRequestsCollection.find(
                new Document("targetUUID", playerUUID.toString())
                    .append("status", "pending")
            ).into(requests);
            return requests;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get incoming friend requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public List<Document> getOutgoingFriendRequests(UUID playerUUID) {
        try {
            List<Document> requests = new ArrayList<>();
            friendRequestsCollection.find(
                new Document("senderUUID", playerUUID.toString())
                    .append("status", "pending")
            ).into(requests);
            return requests;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get outgoing friend requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }



    public List<Document> getPendingRequests(UUID playerUUID) {
        try {
            List<Document> requests = new ArrayList<>();

            // get incoming requests (requests sent TO this player)
            List<Document> incomingRequests = new ArrayList<>();
            friendRequestsCollection.find(
                new Document("targetUUID", playerUUID.toString())
                    .append("status", "pending")
            ).into(incomingRequests);

            // for incoming requests, already have all the info needed
            requests.addAll(incomingRequests);

            // Get outgoing requests (requests sent BY this player)
            List<Document> outgoingRequests = new ArrayList<>();
            friendRequestsCollection.find(
                new Document("senderUUID", playerUUID.toString())
                    .append("status", "pending")
            ).into(outgoingRequests);

            // the documents should already contain targetUUID and targetName from sendFriendRequest
            requests.addAll(outgoingRequests);

            return requests;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get pending friend requests: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public int getFriendCount(UUID playerUUID) {
        try {
            return (int) friendsCollection.countDocuments(
                new Document("playerUUID", playerUUID.toString())
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get friend count: " + e.getMessage());
            return 0;
        }
    }


    public boolean areFriends(UUID playerUUID, UUID friendUUID) {
        try {
            Document friendship = friendsCollection.find(
                new Document("playerUUID", playerUUID.toString())
                    .append("friendUUID", friendUUID.toString())
            ).first();

            return friendship != null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check friendship status: " + e.getMessage());
            return false;
        }
    }
}
