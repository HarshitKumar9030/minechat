package me.harshit.minechat.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Interface for managing friendships in the MineChat API, which is Public and can be used by any plugin
public interface FriendAPI {

    /**
     * Sends a friend request from one player to another
     * @param sender The player sending the request
     * @param target The player receiving the request
     * @return CompletableFuture<Boolean> true if request was sent successfully
     */

    CompletableFuture<Boolean> sendFriendRequest(Player sender, Player target);

    /**
     * Sends a friend request using UUIDs and names
     * @param senderUUID UUID of sender
     * @param senderName Name of sender
     * @param targetUUID UUID of target
     * @param targetName Name of target
     * @return CompletableFuture<Boolean> true if request was sent successfully
     */

    CompletableFuture<Boolean> sendFriendRequest(UUID senderUUID, String senderName, UUID targetUUID, String targetName);

    /**
     * Accepts a friend request
     * @param player The player accepting the request
     * @param requester The player who sent the request
     * @return CompletableFuture<Boolean> true if accepted successfully
     */

    CompletableFuture<Boolean> acceptFriendRequest(Player player, Player requester);

    /**
     * Accepts a friend request using UUIDs
     * @param playerUUID UUID of player accepting
     * @param requesterUUID UUID of requester
     * @return CompletableFuture<Boolean> true if accepted successfully
     */

    CompletableFuture<Boolean> acceptFriendRequest(UUID playerUUID, UUID requesterUUID);

    /**
     * Denies a friend request
     * @param player The player denying the request
     * @param requester The player who sent the request
     * @return CompletableFuture<Boolean> true if denied successfully
     */

    CompletableFuture<Boolean> denyFriendRequest(Player player, Player requester);

    /**
     * Denies a friend request using UUIDs
     * @param playerUUID UUID of player denying
     * @param requesterUUID UUID of requester
     * @return CompletableFuture<Boolean> true if denied successfully
     */

    CompletableFuture<Boolean> denyFriendRequest(UUID playerUUID, UUID requesterUUID);

    /**
     * Removes a friendship
     * @param player The player removing the friend
     * @param friend The friend to remove
     * @return CompletableFuture<Boolean> true if removed successfully
     */

    CompletableFuture<Boolean> removeFriend(Player player, Player friend);

    /**
     * Removes a friendship using UUIDs
     * @param playerUUID UUID of player
     * @param friendUUID UUID of friend
     * @return CompletableFuture<Boolean> true if removed successfully
     */

    CompletableFuture<Boolean> removeFriend(UUID playerUUID, UUID friendUUID);

    /**
     * Checks if two players are friends
     * @param player1 First player
     * @param player2 Second player
     * @return CompletableFuture<Boolean> true if they are friends
     */

    CompletableFuture<Boolean> areFriends(Player player1, Player player2);

    /**
     * Checks if two players are friends using UUIDs
     * @param player1UUID First player's UUID
     * @param player2UUID Second player's UUID
     * @return CompletableFuture<Boolean> true if they are friends
     */

    CompletableFuture<Boolean> areFriends(UUID player1UUID, UUID player2UUID);

    /**
     * Gets a player's friend list
     * @param player The player
     * @return CompletableFuture<List<FriendInfo>> List of friends with their info
     */

    CompletableFuture<List<FriendInfo>> getFriendList(Player player);

    /**
     * Gets a player's friend list using UUID
     * @param playerUUID Player's UUID
     * @return CompletableFuture<List<FriendInfo>> List of friends with their info
     */

    CompletableFuture<List<FriendInfo>> getFriendList(UUID playerUUID);

    /**
     * Gets pending friend requests for a player
     * @param player The player
     * @return CompletableFuture<List<FriendRequest>> List of pending requests
     */

    CompletableFuture<List<FriendRequest>> getPendingRequests(Player player);

    /**
     * Gets pending friend requests using UUID
     * @param playerUUID Player's UUID
     * @return CompletableFuture<List<FriendRequest>> List of pending requests
     */

    CompletableFuture<List<FriendRequest>> getPendingRequests(UUID playerUUID);

    /**
     * Gets the number of friends a player has
     * @param player The player
     * @return CompletableFuture<Integer> Number of friends
     */

    CompletableFuture<Integer> getFriendCount(Player player);

    /**
     * Gets the number of friends using UUID
     * @param playerUUID Player's UUID
     * @return CompletableFuture<Integer> Number of friends
     */

    CompletableFuture<Integer> getFriendCount(UUID playerUUID);

    /**
     * Checks if a player has reached their friend limit
     * @param player The player
     * @return CompletableFuture<Boolean> true if at limit
     */

    CompletableFuture<Boolean> isAtFriendLimit(Player player);

    /**
     * Gets online friends for a player
     * @param player The player
     * @return CompletableFuture<List<Player>> List of online friends
     */

    CompletableFuture<List<Player>> getOnlineFriends(Player player);
}
