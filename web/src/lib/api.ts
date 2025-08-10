/* eslint-disable @typescript-eslint/no-explicit-any */
import { API_BASE_URL } from './constants';

export interface Player {
  playerName: string;
  playerUUID: string;
  rank: string;
  formattedRank: string;
  online: boolean;
  lastSeen?: number;        // Add last seen timestamp
  firstJoin?: number;       // Add first join timestamp
  webAccessEnabled?: boolean; // Add web access status
}

export interface AuthResponse {
  success: boolean;
  user?: {
    playerUUID: string;
    playerName: string;
    webAccessEnabled: boolean;
    rank: string;
    formattedRank: string;
    online: boolean;
    loginTime: number;
    lastSeen?: number;     
    firstJoin?: number;     
  };
  sessionToken?: string;
  error?: string;
}

export interface FriendInfo {
  friendName: string;
  friendUUID: string;
  timestamp: number;
  online: boolean;
  lastSeen?: number;        
  rank?: string;            
  formattedRank?: string;   
}

export interface FriendRequest {
  senderUUID: string;
  senderName: string;
  targetUUID?: string;      
  targetName?: string;     
  timestamp: number;
  status: string;
  isOutgoing?: boolean;    
}

export interface GroupInfo {
  groupId: string;
  groupName: string;
  description: string;
  memberCount: number;
  maxMembers: number;
  role: string;
  createdAt?: number;
  ownerId?: string;
  ownerName?: string;
  isPrivate?: boolean;
  motd?: string; 
  announcements?: string[]; 
}

export interface GroupMember {
  playerUUID: string;
  playerName: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: number;
  online: boolean;
  rank?: string;
  formattedRank?: string;
}

export interface GroupMessage {
  messageId: string;
  groupId: string;
  senderUUID: string;
  senderName: string;
  content: string;
  timestamp: number;
  messageType?: 'TEXT' | 'SYSTEM' | 'ANNOUNCEMENT';
  editedAt?: number;
  rank?: string;
  formattedRank?: string;
}

export interface GroupInvite {
  inviteId: string;
  groupId: string;
  groupName: string;
  inviterUUID: string;
  inviterName: string;
  inviteeUUID: string;
  inviteeName: string;
  timestamp: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';
  message?: string;
}

export interface GroupSettings {
  groupId: string;
  maxMembers: number;
  isPrivate: boolean;
  joinRequiresApproval: boolean;
  membersCanInvite: boolean;
  allowedRanks: string[];
  mutedMembers: string[];
  bannedMembers: string[];
}

export class MinechatAPI {
  private baseURL: string;

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        const data = await response.json().catch(() => ({ error: `HTTP ${response.status}: ${response.statusText}` }));
        const error = new Error(data.message || data.error || `HTTP ${response.status}: ${response.statusText}`);
        (error as any).response = data;
        (error as any).status = response.status;
        throw error;
      }
      
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('API request failed:', error);
      
      if (error instanceof TypeError && error.message.includes('fetch')) {
        const connectionError = new Error('Unable to connect to the Minechat server. Please ensure the server is running and accessible.');
        (connectionError as any).isConnectionError = true;
        throw connectionError;
      }
      
      throw error;
    }
  }

  async authenticate(username: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  }

  async getOnlinePlayers(): Promise<{ players: Player[]; count: number }> {
    const response = await this.request<{ 
      players: any[]; 
      totalPlayers: number; 
      onlinePlayers: number; 
    }>('/players');

    // filter only online players and transform the data
    const onlinePlayers = response.players
      .filter(player => player.online)
      .map(player => ({
        playerName: player.playerName,
        playerUUID: player.playerUUID,
        rank: player.currentRank || player.rank || '[Player]',
        formattedRank: player.formattedRank || '§7[Player]',
        online: true
      }));

    return {
      players: onlinePlayers,
      count: onlinePlayers.length
    };
  }

  async getAllUsers(): Promise<{ users: Player[] }> {
    const response = await this.request<{ 
      players: any[]; 
      totalPlayers: number; 
      onlinePlayers: number; 
    }>('/players');
    
    // Transform the data to match expected format with enhanced info
    const users = response.players.map(player => ({
      playerName: player.playerName,
      playerUUID: player.playerUUID,
      rank: player.currentRank || player.rank || '[Player]',
      formattedRank: player.formattedRank || '§7[Player]',
      online: player.online || false,
      lastSeen: player.lastSeen,
      firstJoin: player.firstJoin,
      webAccessEnabled: player.webAccessEnabled || false
    }));

    return { users };
  }

  async getServerRanks(): Promise<{ ranks: Player[] }> {
    return this.request<{ ranks: Player[] }>('/ranks');
  }

  async getUserDetails(playerUUID: string): Promise<{ user: Player }> {
    const response = await this.request<any>(`/users?playerUUID=${playerUUID}`);
    
    console.log('API getUserDetails raw response:', response);

    const user = {
      playerName: response.user?.playerName || response.playerName,
      playerUUID: response.user?.playerUUID || response.playerUUID,
      rank: response.user?.cleanRank || response.user?.rank || response.cleanRank || response.rank || '[Player]',
      formattedRank: response.user?.formattedRank || response.formattedRank || '§7[Player]',
      online: response.user?.online || response.online || false,
      lastSeen: response.user?.lastSeen || response.lastSeen,
      firstJoin: response.user?.firstJoin || response.firstJoin,
      webAccessEnabled: response.user?.webAccessEnabled || response.webAccessEnabled || false
    };

    console.log('API getUserDetails mapped user:', user);
    return { user };
  }

  async searchPlayers(query: string, limit: number = 10): Promise<{ players: Player[] }> {
    const response = await this.request<{
      players: any[];
    }>(`/search-players?query=${encodeURIComponent(query)}&limit=${limit}`);

    const players = response.players.map(player => ({
      playerName: player.playerName,
      playerUUID: player.playerUUID,
      rank: player.rank || '[Player]',
      formattedRank: player.formattedRank || '§7[Player]',
      online: player.online || false,
      lastSeen: player.lastSeen,
      firstJoin: player.firstJoin,
      webAccessEnabled: player.webAccessEnabled || false
    }));

    return { players };
  }

  async getFriends(playerUUID: string): Promise<{ friends: FriendInfo[] }> {
    const response = await this.request<{ friends: any[] }>(`/friends?playerUUID=${playerUUID}`);

    const friends = response.friends.map(friend => ({
      friendName: friend.friendName,
      friendUUID: friend.friendUUID,
      timestamp: friend.timestamp,
      online: friend.online || false,
      lastSeen: friend.lastSeen,
      rank: friend.rank,
      formattedRank: friend.formattedRank
    }));

    return { friends };
  }

  async getFriendRequests(playerUUID: string): Promise<{ requests: FriendRequest[] }> {
    const response = await this.request<{ requests: any[] }>(`/friend-requests?playerUUID=${playerUUID}`);

    const requests = response.requests.map(request => ({
      senderUUID: request.senderUUID,
      senderName: request.senderName,
      targetUUID: request.targetUUID,
      targetName: request.targetName,
      timestamp: request.timestamp,
      status: request.status,
      isOutgoing: !!request.targetUUID
    }));

    return { requests };
  }

  async getIncomingFriendRequests(playerUUID: string): Promise<{ requests: FriendRequest[] }> {
    const response = await this.request<{ requests: any[] }>(`/friend-requests/incoming?playerUUID=${playerUUID}`);

    const requests = response.requests.map(request => ({
      senderUUID: request.senderUUID,
      senderName: request.senderName,
      targetUUID: request.targetUUID,
      targetName: request.targetName,
      timestamp: request.timestamp,
      status: request.status,
      isOutgoing: false
    }));

    return { requests };
  }

  async getOutgoingFriendRequests(playerUUID: string): Promise<{ requests: FriendRequest[] }> {
    const response = await this.request<{ requests: any[] }>(`/friend-requests/outgoing?playerUUID=${playerUUID}`);

    const requests = response.requests.map(request => ({
      senderUUID: request.senderUUID,
      senderName: request.senderName,
      targetUUID: request.targetUUID,
      targetName: request.targetName,
      timestamp: request.timestamp,
      status: request.status,
      isOutgoing: true
    }));

    return { requests };
  }

  async getFriendStats(playerUUID: string): Promise<{
    friendCount: number;
    pendingRequests: number;
    sentRequests: number;
    maxFriends: number;
  }> {
    return this.request<{
      friendCount: number;
      pendingRequests: number;
      sentRequests: number;
      maxFriends: number;
    }>(`/friend-stats?playerUUID=${playerUUID}`);
  }

  async sendFriendRequest(senderUUID: string, senderName: string, targetName: string) {
    return this.request('/send-friend-request', {
      method: 'POST',
      body: JSON.stringify({ senderUUID, senderName, targetName }),
    });
  }

  async acceptFriendRequest(playerUUID: string, requesterUUID: string) {
    return this.request('/accept-friend-request', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, requesterUUID }),
    });
  }

  async rejectFriendRequest(playerUUID: string, requesterUUID: string) {
    return this.request('/reject-friend-request', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, requesterUUID }),
    });
  }

  async removeFriend(playerUUID: string, friendUUID: string) {
    return this.request('/remove-friend', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, friendUUID }),
    });
  }

  async cancelFriendRequest(senderUUID: string, targetUUID: string) {
    return this.request('/cancel-friend-request', {
      method: 'POST',
      body: JSON.stringify({ senderUUID, targetUUID }),
    });
  }

  async sendFriendMessage(data: {
    senderUUID: string;
    senderName: string;
    targetName?: string;
    targetUUID?: string;
    message: string;
  }) {
    return this.request('/send-friend-message', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getPrivateMessages(player1: string, player2: string, limit = 100): Promise<{ messages: any[] }> {
    return this.request(`/private-messages?player1=${encodeURIComponent(player1)}&player2=${encodeURIComponent(player2)}&limit=${limit}`);
  }


  async getUserGroups(playerUUID: string): Promise<{ groups: GroupInfo[] }> {
    const response = await this.request<{ groups: any[] }>(`/groups?playerUUID=${playerUUID}`);

    const groups = response.groups.map(group => ({
      groupId: group.groupId,
      groupName: group.groupName,
      description: group.description || '',
      memberCount: group.memberCount || 0,
      maxMembers: group.maxMembers || 20,
      role: (group.role || 'MEMBER').toUpperCase(),
      createdAt: group.createdAt,
      ownerId: group.ownerId,
      ownerName: group.ownerName,
      isPrivate: group.isPrivate || false
    }));

    return { groups };
  }

  async getGroupDetails(groupId: string): Promise<{ group: GroupInfo }> {
    return this.request<{ group: GroupInfo }>(`/group-details?groupId=${groupId}`);
  }

  async getAllPublicGroups(): Promise<{ groups: GroupInfo[] }> {
    return this.request<{ groups: GroupInfo[] }>('/public-groups');
  }

  async searchGroups(query: string, limit: number = 10): Promise<{ groups: GroupInfo[] }> {
    const response = await this.request<{ groups: any[] }>(`/search-groups?query=${encodeURIComponent(query)}&limit=${limit}`);

    const groups = response.groups.map(group => ({
      groupId: group.groupId,
      groupName: group.groupName,
      description: group.description || '',
      memberCount: group.memberCount || 0,
      maxMembers: group.maxMembers || 20,
      role: 'GUEST', 
      createdAt: group.createdAt,
      ownerId: group.ownerId,
      ownerName: group.ownerName,
      isPrivate: group.isPrivate || false
    }));

    return { groups };
  }

  async createGroup(data: {
    ownerUUID: string;
    ownerName: string;
    groupName: string;
    description: string;
    maxMembers: number;
    isPrivate?: boolean;
  }) {
    return this.request('/create-group', {
      method: 'POST',
      body: JSON.stringify({
        creatorUUID: data.ownerUUID,
        creatorName: data.ownerName,
        groupName: data.groupName,
        description: data.description,
        maxMembers: data.maxMembers,
        isPrivate: data.isPrivate || false,
      }),
    });
  }

  async updateGroup(groupId: string, data: {
    groupName?: string;
    description?: string;
    maxMembers?: number;
    isPrivate?: boolean;
    motd?: string;
    announcements?: string[];
  }) {
    return this.request(`/update-group`, {
      method: 'POST',
      body: JSON.stringify({ groupId, ...data }),
    });
  }

  async deleteGroup(groupId: string, ownerUUID: string) {
    return this.request('/delete-group', {
      method: 'POST',
      body: JSON.stringify({ groupId, ownerUUID }),
    });
  }

  async getGroupMembers(groupId: string): Promise<{ members: GroupMember[] }> {
    const response = await this.request<{ members: any[] }>(`/group-members?groupId=${groupId}`);

    const members = response.members.map(member => ({
      playerUUID: member.playerUUID,
      playerName: member.playerName,
      role: (member.role || 'MEMBER').toUpperCase(),
      joinedAt: member.joinedAt,
      online: member.online || false,
      rank: member.rank,
      formattedRank: member.formattedRank
    }));

    return { members };
  }

  async joinGroup(playerUUID: string, playerName: string, groupId: string) {
    return this.request('/join-group', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, playerName, groupId }),
    });
  }

  async joinGroupByCode(playerUUID: string, playerName: string, inviteCode: string) {
    return this.request('/join-group-by-code', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, playerName, inviteCode }),
    });
  }

  async leaveGroup(playerUUID: string, groupId: string) {
    return this.request('/leave-group', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, groupId }),
    });
  }

  async kickMember(groupId: string, adminUUID: string, targetUUID: string, reason?: string) {
    return this.request('/kick-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID, reason }),
    });
  }

  async banMember(groupId: string, adminUUID: string, targetUUID: string, reason?: string) {
    return this.request('/ban-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID, reason }),
    });
  }

  async unbanMember(groupId: string, adminUUID: string, targetUUID: string) {
    return this.request('/unban-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID }),
    });
  }

  async updateMemberRole(groupId: string, adminUUID: string, targetUUID: string, newRole: string) {
    return this.request('/update-member-role', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID, newRole }),
    });
  }

  async promoteMember(groupId: string, adminUUID: string, targetUUID: string) {
    return this.request('/promote-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID }),
    });
  }

  async demoteMember(groupId: string, adminUUID: string, targetUUID: string) {
    return this.request('/demote-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID }),
    });
  }

  async getGroupInvites(playerUUID: string): Promise<{ invites: GroupInvite[] }> {
    return this.request<{ invites: GroupInvite[] }>(`/group-invites?playerUUID=${playerUUID}`);
  }

  async sendGroupInvite(data: {
    groupId: string;
    inviterUUID: string;
    inviterName: string;
    targetName: string;
    message?: string;
  }) {
    return this.request('/send-group-invite', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async acceptGroupInvite(inviteId: string, playerUUID: string) {
    return this.request('/accept-group-invite', {
      method: 'POST',
      body: JSON.stringify({ inviteId, playerUUID }),
    });
  }

  async rejectGroupInvite(inviteId: string, playerUUID: string) {
    return this.request('/reject-group-invite', {
      method: 'POST',
      body: JSON.stringify({ inviteId, playerUUID }),
    });
  }

  async cancelGroupInvite(inviteId: string, inviterUUID: string) {
    return this.request('/cancel-group-invite', {
      method: 'POST',
      body: JSON.stringify({ inviteId, inviterUUID }),
    });
  }

  async getGroupMessages(groupId: string, limit: number = 50, before?: number): Promise<{ messages: GroupMessage[] }> {
    let url = `/group-messages?groupId=${groupId}&limit=${limit}`;
    if (before) {
      url += `&before=${before}`;
    }

    const response = await this.request<{ messages: any[] }>(url);

    const messages = response.messages.map(msg => ({
      messageId: msg.messageId,
      groupId: msg.groupId,
      senderUUID: msg.senderUUID,
      senderName: msg.senderName,
      content: msg.content,
      timestamp: msg.timestamp,
      messageType: msg.messageType || 'TEXT',
      editedAt: msg.editedAt
    }));

    return { messages };
  }

  async sendGroupMessage(groupId: string, senderUUID: string, senderName: string, content: string, messageType: string = 'TEXT') {
    return this.request('/send-group-message', {
      method: 'POST',
      body: JSON.stringify({ groupId, senderUUID, senderName, content, messageType }),
    });
  }

  async sendGroupAnnouncement(groupId: string, senderUUID: string, senderName: string, content: string) {
    return this.sendGroupMessage(groupId, senderUUID, senderName, content, 'ANNOUNCEMENT');
  }

  async editGroupMessage(messageId: string, senderUUID: string, newContent: string) {
    return this.request('/edit-group-message', {
      method: 'POST',
      body: JSON.stringify({ messageId, senderUUID, newContent }),
    });
  }

  async deleteGroupMessage(messageId: string, senderUUID: string, groupId: string) {
    return this.request('/delete-group-message', {
      method: 'POST',
      body: JSON.stringify({ messageId, senderUUID, groupId }),
    });
  }

  async getGroupSettings(groupId: string): Promise<{ settings: GroupSettings }> {
    return this.request<{ settings: GroupSettings }>(`/group-settings?groupId=${groupId}`);
  }

  async updateGroupSettings(groupId: string, adminUUID: string, settings: Partial<GroupSettings>) {
    return this.request('/update-group-settings', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, ...settings }),
    });
  }

  async muteMember(groupId: string, adminUUID: string, targetUUID: string, duration?: number) {
    return this.request('/mute-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID, duration }),
    });
  }

  async unmuteMember(groupId: string, adminUUID: string, targetUUID: string) {
    return this.request('/unmute-member', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, targetUUID }),
    });
  }

  async setGroupPrivacy(groupId: string, adminUUID: string, isPrivate: boolean) {
    return this.request('/set-group-privacy', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID, isPrivate }),
    });
  }

  async getGroupInviteCode(groupId: string, playerUUID: string): Promise<{ inviteCode: string }> {
    return this.request<{ inviteCode: string }>(`/group-invite-code?groupId=${groupId}&playerUUID=${playerUUID}`);
  }

  async regenerateGroupInviteCode(groupId: string, adminUUID: string): Promise<{ inviteCode: string }> {
    return this.request<{ inviteCode: string }>('/regenerate-invite-code', {
      method: 'POST',
      body: JSON.stringify({ groupId, adminUUID }),
    });
  }

  async getGroupStats(groupId: string): Promise<{
    memberCount: number;
    messageCount: number;
    createdAt: number;
    mostActiveMembers: { playerName: string; messageCount: number }[];
    recentActivity: { date: string; messages: number; joins: number }[];
  }> {
    return this.request(`/group-stats?groupId=${groupId}`);
  }

  async getRecommendedGroups(playerUUID: string): Promise<{ groups: GroupInfo[] }> {
    return this.request<{ groups: GroupInfo[] }>(`/recommended-groups?playerUUID=${playerUUID}`);
  }

  async getTrendingGroups(): Promise<{ groups: GroupInfo[] }> {
    return this.request<{ groups: GroupInfo[] }>('/trending-groups');
  }

  async getPopularGroups(limit: number = 10): Promise<{ groups: GroupInfo[] }> {
    return this.request<{ groups: GroupInfo[] }>(`/popular-groups?limit=${limit}`);
  }
}

export const getPlayerSkin = (username: string, uuid?: string) => {
  if (!username && !uuid) {
    return 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
  }

  if (uuid && typeof uuid === 'string' && uuid.length === 36 && uuid.includes('-')) {
    return `https://crafatar.com/avatars/${uuid}?size=64&overlay`;
  }

  if (username && typeof username === 'string' && username !== 'undefined') {
    return `https://crafatar.com/avatars/${username}?size=64&overlay`;
  }

  return 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
};

export const getPlayerHead = (uuidOrUsername: string, fallbackUuid?: string, size: number = 64) => {
  if (!uuidOrUsername && !fallbackUuid) {
    return `https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=${size}&overlay`;
  }

  if (uuidOrUsername && typeof uuidOrUsername === 'string' && uuidOrUsername.length === 36 && uuidOrUsername.includes('-')) {
    return `https://crafatar.com/avatars/${uuidOrUsername}?size=${size}&overlay`;
  }

  if (fallbackUuid && typeof fallbackUuid === 'string' && fallbackUuid.length === 36 && fallbackUuid.includes('-')) {
    return `https://crafatar.com/avatars/${fallbackUuid}?size=${size}&overlay`;
  }

  if (uuidOrUsername && typeof uuidOrUsername === 'string' && uuidOrUsername !== 'undefined') {
    return `https://mc-heads.net/avatar/${uuidOrUsername}/${size}`;
  }

  return `https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=${size}&overlay`;
};

export const formatMinecraftRank = (formattedRank: string): string => {
  if (!formattedRank) return '';

  return formattedRank
    .replace(/§[0-9a-fk-or]/gi, '')
    .trim();
};

export const getRankColor = (formattedRank: string): string => {
  if (!formattedRank) return 'text-neutral-400';

  const colorMap: { [key: string]: string } = {
    '§0': 'text-neutral-900',
    '§1': 'text-neutral-700',
    '§2': 'text-neutral-500',
    '§3': 'text-neutral-400',
    '§4': 'text-neutral-600',
    '§5': 'text-neutral-300',
    '§6': 'text-yellow-500', // Gold
    '§7': 'text-neutral-400',
    '§8': 'text-neutral-600',
    '§9': 'text-neutral-300',
    '§a': 'text-neutral-200',
    '§b': 'text-neutral-300',
    '§c': 'text-red-400',
    '§d': 'text-yellow-400',
    '§e': 'text-yellow-300',
    '§f': 'text-white',
  };

  const colorMatches = formattedRank.match(/§[0-9a-f]/gi);
  if (colorMatches && colorMatches.length > 0) {
    const priorityColors = ['§c', '§6', '§e', '§d', '§a', '§b', '§9', '§5', '§f'];

    for (const priority of priorityColors) {
        // this works as it checks if the color code exists in the matches
        // find the first color code that matches the priority
        // this will return the first match that is found in the colorMatches array
      const found = colorMatches.find(code => code.toLowerCase() === priority);
      if (found && colorMap[found.toLowerCase()]) {
        return colorMap[found.toLowerCase()];
      }
    }

    const firstColor = colorMatches[0].toLowerCase();
    if (colorMap[firstColor]) {
      return colorMap[firstColor];
    }
  }

  const lowerRank = formattedRank.toLowerCase();
  if (lowerRank.includes('admin') || lowerRank.includes('owner')) {
    return 'text-red-400';
  } else if (lowerRank.includes('mod') || lowerRank.includes('staff')) {
    return 'text-purple-400';
  } else if (lowerRank.includes('vip') || lowerRank.includes('mvp')) {
    return 'text-yellow-500';
  }

  return 'text-neutral-400';
};

function getCurrentUser() {
  if (typeof window === 'undefined') return null;
  const savedUser = localStorage.getItem('minechat_user');
  return savedUser ? JSON.parse(savedUser) : null;
}

export const moderationApi = {
  kickMember: async (groupId: string, memberUUID: string, reason?: string) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/kick-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID,
        reason: reason || 'No reason provided'
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to kick member');
    }
    
    return response.json();
  },

  banMember: async (groupId: string, memberUUID: string, reason?: string) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/ban-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID,
        reason: reason || 'No reason provided'
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to ban member');
    }
    
    return response.json();
  },

  muteMember: async (groupId: string, memberUUID: string, duration: number) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/mute-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID,
        duration
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to mute member');
    }
    
    return response.json();
  },

  unmuteMember: async (groupId: string, memberUUID: string) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/unmute-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to unmute member');
    }
    
    return response.json();
  },

  promoteMember: async (groupId: string, memberUUID: string) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/promote-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to promote member');
    }
    
    return response.json();
  },

  demoteMember: async (groupId: string, memberUUID: string) => {
    const currentUser = getCurrentUser();
    if (!currentUser) throw new Error('User not authenticated');
    
    const response = await fetch(`${API_BASE_URL}/demote-member`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        adminUUID: currentUser.playerUUID,
        targetUUID: memberUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to demote member');
    }
    
    return response.json();
  },

  updateGroupMOTD: async (groupId: string, motd: string) => {
    const response = await fetch(`${API_BASE_URL}/update-group-motd`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        motd
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update MOTD');
    }
    
    return response.json();
  },

  addAnnouncement: async (groupId: string, announcement: string, adminUUID: string) => {
    const response = await fetch(`${API_BASE_URL}/add-announcement`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        announcement,
        adminUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to add announcement');
    }
    
    return response.json();
  },

  removeAnnouncement: async (groupId: string, index: number, adminUUID: string) => {
    const response = await fetch(`${API_BASE_URL}/remove-announcement`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        index,
        adminUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to remove announcement');
    }
    
    return response.json();
  },

  updateAnnouncement: async (groupId: string, index: number, announcement: string, adminUUID: string) => {
    const response = await fetch(`${API_BASE_URL}/update-announcement`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        groupId,
        index,
        announcement,
        adminUUID
      }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update announcement');
    }
    
    return response.json();
  }
};

export const api = new MinechatAPI();
