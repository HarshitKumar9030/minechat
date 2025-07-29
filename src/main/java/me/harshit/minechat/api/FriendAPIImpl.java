package me.harshit.minechat.api;

import me.harshit.minechat.database.FriendManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Impl of the friend api
public class FriendAPIImpl implements FriendAPI {

    private final FriendManager friendManager;
    private final JavaPlugin plugin;

    public FriendAPIImpl(FriendManager friendManager, JavaPlugin plugin) {
        this.friendManager = friendManager;
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Boolean> sendFriendRequest(Player sender, Player target) {
        return sendFriendRequest(sender.getUniqueId(), sender.getName(),
                                target.getUniqueId(), target.getName());
    }

    @Override
    public CompletableFuture<Boolean> sendFriendRequest(UUID senderUUID, String senderName,
                                                       UUID targetUUID, String targetName) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.sendFriendRequest(senderUUID, senderName, targetUUID, targetName));
    }

    @Override
    public CompletableFuture<Boolean> acceptFriendRequest(Player player, Player requester) {
        return acceptFriendRequest(player.getUniqueId(), requester.getUniqueId());
    }

    @Override
    public CompletableFuture<Boolean> acceptFriendRequest(UUID playerUUID, UUID requesterUUID) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.acceptFriendRequest(playerUUID, requesterUUID));
    }

    @Override
    public CompletableFuture<Boolean> denyFriendRequest(Player player, Player requester) {
        return denyFriendRequest(player.getUniqueId(), requester.getUniqueId());
    }

    @Override
    public CompletableFuture<Boolean> denyFriendRequest(UUID playerUUID, UUID requesterUUID) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.denyFriendRequest(playerUUID, requesterUUID));
    }

    @Override
    public CompletableFuture<Boolean> removeFriend(Player player, Player friend) {
        return removeFriend(player.getUniqueId(), friend.getUniqueId());
    }

    @Override
    public CompletableFuture<Boolean> removeFriend(UUID playerUUID, UUID friendUUID) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.removeFriend(playerUUID, friendUUID));
    }

    @Override
    public CompletableFuture<Boolean> areFriends(Player player1, Player player2) {
        return areFriends(player1.getUniqueId(), player2.getUniqueId());
    }

    @Override
    public CompletableFuture<Boolean> areFriends(UUID player1UUID, UUID player2UUID) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.areFriends(player1UUID, player2UUID));
    }

    @Override
    public CompletableFuture<List<FriendInfo>> getFriendList(Player player) {
        return getFriendList(player.getUniqueId());
    }

    @Override
    public CompletableFuture<List<FriendInfo>> getFriendList(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> friendDocs = friendManager.getFriendList(playerUUID);
            List<FriendInfo> friends = new ArrayList<>();

            for (Document doc : friendDocs) {
                String friendName = doc.getString("friendName");
                UUID friendUUID = UUID.fromString(doc.getString("friendUUID"));
                long timestamp = doc.getLong("timestamp");
                LocalDateTime friendsSince = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);

                // Check if friend is online
                Player onlineFriend = Bukkit.getPlayer(friendUUID);
                boolean isOnline = onlineFriend != null && onlineFriend.isOnline();

                friends.add(new FriendInfo(playerUUID, "", friendUUID, friendName, friendsSince, isOnline));
            }

            return friends;
        });
    }

    @Override
    public CompletableFuture<List<FriendRequest>> getPendingRequests(Player player) {
        return getPendingRequests(player.getUniqueId());
    }

    @Override
    public CompletableFuture<List<FriendRequest>> getPendingRequests(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> requestDocs = friendManager.getPendingRequests(playerUUID);
            List<FriendRequest> requests = new ArrayList<>();

            for (Document doc : requestDocs) {
                UUID senderUUID = UUID.fromString(doc.getString("senderUUID"));
                String senderName = doc.getString("senderName");
                String targetName = doc.getString("targetName");
                long timestamp = doc.getLong("timestamp");
                LocalDateTime requestTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);

                requests.add(new FriendRequest(senderUUID, senderName, playerUUID, targetName,
                                             requestTime, FriendRequest.RequestStatus.PENDING));
            }

            return requests;
        });
    }

    @Override
    public CompletableFuture<Integer> getFriendCount(Player player) {
        return getFriendCount(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Integer> getFriendCount(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() ->
            friendManager.getFriendCount(playerUUID));
    }

    @Override
    public CompletableFuture<Boolean> isAtFriendLimit(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            int maxFriends = plugin.getConfig().getInt("friends.max-friends", 50);
            int currentFriends = friendManager.getFriendCount(player.getUniqueId());
            return currentFriends >= maxFriends;
        });
    }

    @Override
    public CompletableFuture<List<Player>> getOnlineFriends(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> friendDocs = friendManager.getFriendList(player.getUniqueId());
            List<Player> onlineFriends = new ArrayList<>();

            for (Document doc : friendDocs) {
                String friendName = doc.getString("friendName");
                Player onlineFriend = Bukkit.getPlayerExact(friendName);

                if (onlineFriend != null && onlineFriend.isOnline()) {
                    onlineFriends.add(onlineFriend);
                }
            }

            return onlineFriends;
        });
    }
}
