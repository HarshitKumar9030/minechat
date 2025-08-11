# Minechat Plugin API (Build on top)

Minechat exposes public services so other plugins can integrate with friends and groups without depending on internal classes.

- Package: `me.harshit.minechat.api`
- Services: `FriendAPI`, `GroupAPI`
- Wiring: Bukkit Services API (no shading; use compileOnly)

## Runtime dependency
Add Minechat as a dependency so it loads before your plugin.

plugin.yml (of your plugin)
- softdepend:
  - Minechat

## Compile dependency
Reference the Minechat jar for interfaces only (do not shade/relocate).

Gradle (example)
- dependencies:
  - compileOnly files('libs/minechat-1.0-SNAPSHOT-shaded.jar')

Maven (example)
- dependency:
  - groupId: local
  - artifactId: minechat
  - version: 0.1.0
  - scope: provided
  - systemPath: ${project.basedir}/libs/minechat-1.0-SNAPSHOT-shaded.jar

Note: You can also extract the `me.harshit.minechat.api` classes into a thin API jar for cleaner distribution if desired.

## Getting the services

Java
- FriendAPI friendApi = Bukkit.getServicesManager().load(FriendAPI.class);
- GroupAPI groupApi = Bukkit.getServicesManager().load(GroupAPI.class);

or with null checks:
- RegisteredServiceProvider<FriendAPI> fr = Bukkit.getServicesManager().getRegistration(FriendAPI.class);
- if (fr != null) { friendApi = fr.getProvider(); }

When to resolve: in onEnable() (Minechat is a softdepend), or lazily on first use. You can also listen for ServiceRegisterEvent if you want hot registration.

## Threading model
Most methods are asynchronous and return `CompletableFuture<T>`. Don’t call Bukkit API from those completion threads; switch back to main thread when needed.

```
friendApi.getFriendList(playerUuid).thenAccept(list -> {
  Bukkit.getScheduler().runTask(this, () -> {
    // safe to use Bukkit API here
  });
});
```

## FriendAPI quick reference
- sendFriendRequest(Player sender, Player target): CompletableFuture<Boolean>
- sendFriendRequest(UUID senderUUID, String senderName, UUID targetUUID, String targetName): CompletableFuture<Boolean>
- acceptFriendRequest(Player/UUID…): CompletableFuture<Boolean>
- denyFriendRequest(Player/UUID…): CompletableFuture<Boolean>
- removeFriend(Player/UUID…): CompletableFuture<Boolean>
- areFriends(Player/UUID…): CompletableFuture<Boolean>
- getFriendList(Player/UUID…): CompletableFuture<List<FriendInfo>>
- getPendingRequests(Player/UUID…): CompletableFuture<List<FriendRequest>>
- getFriendCount(Player/UUID…): CompletableFuture<Integer>
- isAtFriendLimit(Player): CompletableFuture<Boolean>
- getOnlineFriends(Player): CompletableFuture<List<Player>>

Data classes: `FriendInfo`, `FriendRequest`

Example: check and send a request
```
friendApi.areFriends(a, b).thenAccept(already -> {
  if (!already) {
    friendApi.sendFriendRequest(aUuid, aName, bUuid, bName);
  }
});
```

## GroupAPI quick reference
- getGroupById(UUID): CompletableFuture<GroupInfo>
- getGroupByInviteCode(String): CompletableFuture<GroupInfo>
- getPlayerGroups(UUID): CompletableFuture<List<GroupInfo>>
- createGroup(UUID ownerId, String ownerName, String groupName, String description, int maxMembers, boolean isPrivate): CompletableFuture<Boolean>
- joinGroup(UUID groupId, UUID playerId, String playerName): CompletableFuture<Boolean>
- leaveGroup(UUID groupId, UUID playerId): CompletableFuture<Boolean>
- canInvite(UUID groupId, UUID inviterId): CompletableFuture<Boolean>
- sendInvite(UUID groupId, UUID inviterId, String inviterName, UUID targetId, String targetName): CompletableFuture<Boolean>
- getInvites(UUID playerId): CompletableFuture<List<GroupInvite>>
- acceptInvite(String inviteId, UUID playerId): CompletableFuture<Boolean>
- rejectInvite(String inviteId, UUID playerId): CompletableFuture<Boolean>

Data classes: `GroupInfo`, `GroupInvite`, `GroupMember`, `GroupMessage`, `GroupSettings`

Example: create and invite
```
groupApi.createGroup(owner, ownerName, "Builders", "Public build crew", 20, false)
  .thenCompose(ok -> ok ? groupApi.getGroupByInviteCode("some-code") : CompletableFuture.completedFuture(null))
  .thenCompose(group -> group != null
    ? groupApi.sendInvite(group.getGroupId(), owner, ownerName, target, targetName)
    : CompletableFuture.completedFuture(false));
```

## Error handling
- Futures may complete exceptionally; add `.exceptionally(ex -> { /* log */ return fallback; })`
- Validate input UUIDs/names; most methods return false on invalid flows (e.g., duplicate invites) but you should handle this gracefully.

## Service availability check
```
if (friendApi == null || groupApi == null) {
  getLogger().warning("Minechat APIs not available");
}
```

That’s it—use the services, keep Bukkit calls on the main thread, and treat results as async.
