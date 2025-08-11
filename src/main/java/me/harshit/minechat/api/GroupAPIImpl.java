package me.harshit.minechat.api;

import me.harshit.minechat.database.GroupManager;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GroupAPIImpl implements GroupAPI {
    private final GroupManager groupManager;
    private final JavaPlugin plugin;

    public GroupAPIImpl(GroupManager groupManager, JavaPlugin plugin) {
        this.groupManager = groupManager;
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<GroupInfo> getGroupById(UUID groupId) {
        return CompletableFuture.supplyAsync(() -> groupManager.getGroupById(groupId));
    }

    @Override
    public CompletableFuture<GroupInfo> getGroupByInviteCode(String inviteCode) {
        return CompletableFuture.supplyAsync(() -> groupManager.getGroupByInviteCode(inviteCode));
    }

    @Override
    public CompletableFuture<List<GroupInfo>> getPlayerGroups(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> groupManager.getPlayerGroups(playerId));
    }

    @Override
    public CompletableFuture<Boolean> createGroup(UUID ownerId, String ownerName, String groupName, String description, int maxMembers, boolean isPrivate) {
        return CompletableFuture.supplyAsync(() -> groupManager.createGroup(ownerId, ownerName, groupName, description, maxMembers, isPrivate) != null);
    }

    @Override
    public CompletableFuture<Boolean> joinGroup(UUID groupId, UUID playerId, String playerName) {
        return CompletableFuture.supplyAsync(() -> groupManager.joinGroup(groupId, playerId, playerName));
    }

    @Override
    public CompletableFuture<Boolean> leaveGroup(UUID groupId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> groupManager.leaveGroup(playerId, groupId));
    }

    @Override
    public CompletableFuture<Boolean> canInvite(UUID groupId, UUID inviterId) {
        return CompletableFuture.supplyAsync(() -> groupManager.canInviteToGroup(groupId, inviterId));
    }

    @Override
    public CompletableFuture<Boolean> sendInvite(UUID groupId, UUID inviterId, String inviterName, UUID targetId, String targetName) {
        return CompletableFuture.supplyAsync(() -> groupManager.sendGroupInvite(groupId, inviterId, inviterName, targetId, targetName));
    }

    @Override
    public CompletableFuture<List<GroupInvite>> getInvites(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> docs = groupManager.getGroupInvites(playerId);
            List<GroupInvite> invites = new ArrayList<>();
            for (Document d : docs) {
                try {
                    invites.add(new GroupInvite(
                        d.getString("inviteId"),
                        UUID.fromString(d.getString("groupId")),
                        d.getString("groupName"),
                        UUID.fromString(d.getString("inviterUUID")),
                        d.getString("inviterName"),
                        UUID.fromString(d.getString("inviteeUUID")),
                        d.getString("inviteeName"),
                        d.getLong("timestamp"),
                        d.getString("status")
                    ));
                } catch (Exception ignored) {}
            }
            return invites;
        });
    }

    @Override
    public CompletableFuture<Boolean> acceptInvite(String inviteId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> groupManager.acceptGroupInviteById(inviteId, playerId));
    }

    @Override
    public CompletableFuture<Boolean> rejectInvite(String inviteId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> groupManager.rejectGroupInviteById(inviteId, playerId));
    }
}
