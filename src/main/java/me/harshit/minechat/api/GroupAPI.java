package me.harshit.minechat.api;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GroupAPI {
    CompletableFuture<GroupInfo> getGroupById(UUID groupId);
    CompletableFuture<GroupInfo> getGroupByInviteCode(String inviteCode);
    CompletableFuture<List<GroupInfo>> getPlayerGroups(UUID playerId);

    CompletableFuture<Boolean> createGroup(UUID ownerId, String ownerName, String groupName, String description, int maxMembers, boolean isPrivate);
    CompletableFuture<Boolean> joinGroup(UUID groupId, UUID playerId, String playerName);
    CompletableFuture<Boolean> leaveGroup(UUID groupId, UUID playerId);

    CompletableFuture<Boolean> canInvite(UUID groupId, UUID inviterId);
    CompletableFuture<Boolean> sendInvite(UUID groupId, UUID inviterId, String inviterName, UUID targetId, String targetName);
    CompletableFuture<List<GroupInvite>> getInvites(UUID playerId);
    CompletableFuture<Boolean> acceptInvite(String inviteId, UUID playerId);
    CompletableFuture<Boolean> rejectInvite(String inviteId, UUID playerId);

    default CompletableFuture<Boolean> joinGroup(UUID groupId, Player player) {
        return joinGroup(groupId, player.getUniqueId(), player.getName());
    }
}
