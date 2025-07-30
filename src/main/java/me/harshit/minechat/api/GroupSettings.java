package me.harshit.minechat.api;

// group settings class
public class GroupSettings {

    private final boolean allowInvites;
    private final boolean friendsOnly;
    private final boolean muteNonMembers;
    private final boolean logMessages;
    private final boolean webAccessEnabled;
    private final String joinMessage;
    private final String leaveMessage;

    public GroupSettings(boolean allowInvites, boolean friendsOnly, boolean muteNonMembers,
                        boolean logMessages, boolean webAccessEnabled, String joinMessage, String leaveMessage) {
        this.allowInvites = allowInvites;
        this.friendsOnly = friendsOnly;
        this.muteNonMembers = muteNonMembers;
        this.logMessages = logMessages;
        this.webAccessEnabled = webAccessEnabled;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
    }

    // Static factory method for default settings
    public static GroupSettings getDefault() {
        return new GroupSettings(
            true,  // allowInvites
            false, // friendsOnly
            false, // muteNonMembers
            true,  // logMessages
            true,  // webAccessEnabled
            "Welcome to the group!", // joinMessage
            "Thanks for being part of the group!" // leaveMessage
        );
    }

    public boolean isAllowInvites() { return allowInvites; }
    public boolean isFriendsOnly() { return friendsOnly; }
    public boolean isMuteNonMembers() { return muteNonMembers; }
    public boolean isLogMessages() { return logMessages; }
    public boolean isWebAccessEnabled() { return webAccessEnabled; }
    public String getJoinMessage() { return joinMessage; }
    public String getLeaveMessage() { return leaveMessage; }
}
