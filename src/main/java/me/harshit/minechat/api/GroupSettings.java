package me.harshit.minechat.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupSettings {

    private final boolean allowInvites;
    private final boolean friendsOnly;
    private final boolean muteNonMembers;
    private final boolean logMessages;
    private final boolean webAccessEnabled;
    private final boolean joinRequiresApproval;
    private final boolean membersCanInvite;
    private final boolean onlyAdminsCanMessage;
    private final boolean enableAnnouncements;
    private final String joinMessage;
    private final String leaveMessage;
    private final List<String> allowedRanks;
    private final List<UUID> mutedMembers;
    private final List<UUID> bannedMembers;
    private final String groupMotd; 
    private final String inviteCode; 

    public GroupSettings(boolean allowInvites, boolean friendsOnly, boolean muteNonMembers,
                        boolean logMessages, boolean webAccessEnabled, boolean joinRequiresApproval,
                        boolean membersCanInvite, boolean onlyAdminsCanMessage, boolean enableAnnouncements,
                        String joinMessage, String leaveMessage, List<String> allowedRanks,
                        List<UUID> mutedMembers, List<UUID> bannedMembers, String groupMotd, String inviteCode) {
        this.allowInvites = allowInvites;
        this.friendsOnly = friendsOnly;
        this.muteNonMembers = muteNonMembers;
        this.logMessages = logMessages;
        this.webAccessEnabled = webAccessEnabled;
        this.joinRequiresApproval = joinRequiresApproval;
        this.membersCanInvite = membersCanInvite;
        this.onlyAdminsCanMessage = onlyAdminsCanMessage;
        this.enableAnnouncements = enableAnnouncements;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
        this.allowedRanks = allowedRanks != null ? new ArrayList<>(allowedRanks) : new ArrayList<>();
        this.mutedMembers = mutedMembers != null ? new ArrayList<>(mutedMembers) : new ArrayList<>();
        this.bannedMembers = bannedMembers != null ? new ArrayList<>(bannedMembers) : new ArrayList<>();
        this.groupMotd = groupMotd;
        this.inviteCode = inviteCode;
    }

    // static factory method for default settings
    public static GroupSettings getDefault() {
        return new GroupSettings(
            true,  // allowInvites
            false, // friendsOnly
            false, // muteNonMembers
            true,  // logMessages
            true,  // webAccessEnabled
            false, // joinRequiresApproval
            true,  // membersCanInvite
            false, // onlyAdminsCanMessage
            true,  // enableAnnouncements
            "Welcome to the group!", // joinMessage
            "Thanks for being part of the group!", // leaveMessage
            new ArrayList<>(), // allowedRanks (empty = all ranks allowed)
            new ArrayList<>(), // mutedMembers
            new ArrayList<>(), // bannedMembers
            "", // groupMotd
            generateInviteCode() // inviteCode
        );
    }

    private static String generateInviteCode() {
        return "GRP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean isAllowInvites() { return allowInvites; }
    public boolean isFriendsOnly() { return friendsOnly; }
    public boolean isMuteNonMembers() { return muteNonMembers; }
    public boolean isLogMessages() { return logMessages; }
    public boolean isWebAccessEnabled() { return webAccessEnabled; }
    public boolean isJoinRequiresApproval() { return joinRequiresApproval; }
    public boolean isMembersCanInvite() { return membersCanInvite; }
    public boolean isOnlyAdminsCanMessage() { return onlyAdminsCanMessage; }
    public boolean isEnableAnnouncements() { return enableAnnouncements; }
    public String getJoinMessage() { return joinMessage; }
    public String getLeaveMessage() { return leaveMessage; }
    public List<String> getAllowedRanks() { return new ArrayList<>(allowedRanks); }
    public List<UUID> getMutedMembers() { return new ArrayList<>(mutedMembers); }
    public List<UUID> getBannedMembers() { return new ArrayList<>(bannedMembers); }
    public String getGroupMotd() { return groupMotd; }
    public String getInviteCode() { return inviteCode; }

    public boolean isPlayerMuted(UUID playerId) {
        return mutedMembers.contains(playerId);
    }

    public boolean isPlayerBanned(UUID playerId) {
        return bannedMembers.contains(playerId);
    }

    public boolean isRankAllowed(String rank) {
        return allowedRanks.isEmpty() || allowedRanks.contains(rank);
    }

    public static GroupSettings fromDocument(org.bson.Document settingsDoc) {
        if (settingsDoc == null) {
            return getDefault();
        }

        try {
            boolean allowInvites = settingsDoc.getBoolean("allowInvites", true);
            boolean friendsOnly = settingsDoc.getBoolean("friendsOnly", false);
            boolean muteNonMembers = settingsDoc.getBoolean("muteNonMembers", false);
            boolean logMessages = settingsDoc.getBoolean("logMessages", true);
            boolean webAccessEnabled = settingsDoc.getBoolean("webAccessEnabled", true);
            boolean joinRequiresApproval = settingsDoc.getBoolean("joinRequiresApproval", false);
            boolean membersCanInvite = settingsDoc.getBoolean("membersCanInvite", true);
            boolean onlyAdminsCanMessage = settingsDoc.getBoolean("onlyAdminsCanMessage", false);
            boolean enableAnnouncements = settingsDoc.getBoolean("enableAnnouncements", true);
            String joinMessage = settingsDoc.getString("joinMessage");
            String leaveMessage = settingsDoc.getString("leaveMessage");
            String groupMotd = settingsDoc.getString("groupMotd");
            String inviteCode = settingsDoc.getString("inviteCode");

            @SuppressWarnings("unchecked")
            List<String> allowedRanks = settingsDoc.getList("allowedRanks", String.class);
            if (allowedRanks == null) allowedRanks = new ArrayList<>();

            List<String> mutedMemberStrings = settingsDoc.getList("mutedMembers", String.class);
            List<UUID> mutedMembers = new ArrayList<>();
            if (mutedMemberStrings != null) {
                for (String uuidStr : mutedMemberStrings) {
                    try {
                        mutedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            List<String> bannedMemberStrings = settingsDoc.getList("bannedMembers", String.class);
            List<UUID> bannedMembers = new ArrayList<>();
            if (bannedMemberStrings != null) {
                for (String uuidStr : bannedMemberStrings) {
                    try {
                        bannedMembers.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return new GroupSettings(allowInvites, friendsOnly, muteNonMembers, logMessages,
                    webAccessEnabled, joinRequiresApproval, membersCanInvite, onlyAdminsCanMessage,
                    enableAnnouncements, joinMessage, leaveMessage, allowedRanks, mutedMembers,
                    bannedMembers, groupMotd, inviteCode);
        } catch (Exception e) {
            return getDefault();
        }
    }

    public static class Builder {
        private boolean allowInvites = true;
        private boolean friendsOnly = false;
        private boolean muteNonMembers = false;
        private boolean logMessages = true;
        private boolean webAccessEnabled = true;
        private boolean joinRequiresApproval = false;
        private boolean membersCanInvite = true;
        private boolean onlyAdminsCanMessage = false;
        private boolean enableAnnouncements = true;
        private String joinMessage = "Welcome to the group!";
        private String leaveMessage = "Thanks for being part of the group!";
        private List<String> allowedRanks = new ArrayList<>();
        private List<UUID> mutedMembers = new ArrayList<>();
        private List<UUID> bannedMembers = new ArrayList<>();
        private String groupMotd = "";
        private String inviteCode = generateInviteCode();

        public Builder allowInvites(boolean allowInvites) {
            this.allowInvites = allowInvites;
            return this;
        }

        public Builder friendsOnly(boolean friendsOnly) {
            this.friendsOnly = friendsOnly;
            return this;
        }

        public Builder muteNonMembers(boolean muteNonMembers) {
            this.muteNonMembers = muteNonMembers;
            return this;
        }

        public Builder logMessages(boolean logMessages) {
            this.logMessages = logMessages;
            return this;
        }

        public Builder webAccessEnabled(boolean webAccessEnabled) {
            this.webAccessEnabled = webAccessEnabled;
            return this;
        }

        public Builder joinRequiresApproval(boolean joinRequiresApproval) {
            this.joinRequiresApproval = joinRequiresApproval;
            return this;
        }

        public Builder membersCanInvite(boolean membersCanInvite) {
            this.membersCanInvite = membersCanInvite;
            return this;
        }

        public Builder onlyAdminsCanMessage(boolean onlyAdminsCanMessage) {
            this.onlyAdminsCanMessage = onlyAdminsCanMessage;
            return this;
        }

        public Builder enableAnnouncements(boolean enableAnnouncements) {
            this.enableAnnouncements = enableAnnouncements;
            return this;
        }

        public Builder joinMessage(String joinMessage) {
            this.joinMessage = joinMessage;
            return this;
        }

        public Builder leaveMessage(String leaveMessage) {
            this.leaveMessage = leaveMessage;
            return this;
        }

        public Builder allowedRanks(List<String> allowedRanks) {
            this.allowedRanks = new ArrayList<>(allowedRanks);
            return this;
        }

        public Builder mutedMembers(List<UUID> mutedMembers) {
            this.mutedMembers = new ArrayList<>(mutedMembers);
            return this;
        }

        public Builder bannedMembers(List<UUID> bannedMembers) {
            this.bannedMembers = new ArrayList<>(bannedMembers);
            return this;
        }

        public Builder groupMotd(String groupMotd) {
            this.groupMotd = groupMotd;
            return this;
        }

        public Builder inviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
            return this;
        }

        public GroupSettings build() {
            return new GroupSettings(allowInvites, friendsOnly, muteNonMembers, logMessages,
                    webAccessEnabled, joinRequiresApproval, membersCanInvite, onlyAdminsCanMessage,
                    enableAnnouncements, joinMessage, leaveMessage, allowedRanks, mutedMembers,
                    bannedMembers, groupMotd, inviteCode);
        }
    }
}
