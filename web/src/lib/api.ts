/* eslint-disable @typescript-eslint/no-explicit-any */
import { API_BASE_URL } from './constants';

export interface Player {
  playerName: string;
  playerUUID: string;
  rank: string;
  formattedRank: string;
  online: boolean;
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
  };
  sessionToken?: string;
  error?: string;
}

export interface FriendInfo {
  friendName: string;
  friendUUID: string;
  timestamp: number;
  online: boolean;
}

export interface FriendRequest {
  senderUUID: string;
  senderName: string;
  targetUUID?: string;    // for outgoing requests - who you sent the request to
  targetName?: string;    // for outgoing requests - name of the person you sent request to
  timestamp: number;
  status: string;
}

export interface GroupInfo {
  groupId: string;
  groupName: string;
  description: string;
  memberCount: number;
  maxMembers: number;
  role: string;
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
      const data = await response.json();
      
      // Check if the response status indicates an error
      if (!response.ok) {
        // Create an error object that includes the response data
        const error = new Error(data.error || `HTTP ${response.status}: ${response.statusText}`);
        (error as any).response = { data, status: response.status };
        throw error;
      }
      
      return data;
    } catch (error) {
      console.error('API request failed:', error);
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
    
    // trasform the data to match expected format
    const users = response.players.map(player => ({
      playerName: player.playerName,
      playerUUID: player.playerUUID,
      rank: player.currentRank || player.rank || '[Player]',
      formattedRank: player.formattedRank || '§7[Player]',
      online: player.online || false
    }));

    return { users };
  }

  async getServerRanks(): Promise<{ ranks: Player[] }> {
    return this.request<{ ranks: Player[] }>('/ranks');
  }

  async getFriends(playerUUID: string): Promise<{ friends: FriendInfo[] }> {
    return this.request<{ friends: FriendInfo[] }>(`/friends?playerUUID=${playerUUID}`);
  }

  async getFriendRequests(playerUUID: string): Promise<{ requests: FriendRequest[] }> {
    return this.request<{ requests: FriendRequest[] }>(`/friend-requests?playerUUID=${playerUUID}`);
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

  async getUserGroups(playerUUID: string): Promise<{ groups: GroupInfo[] }> {
    return this.request<{ groups: GroupInfo[] }>(`/groups?playerUUID=${playerUUID}`);
  }

  async createGroup(ownerUUID: string, ownerName: string, groupName: string, description: string, maxMembers: number) {
    return this.request('/create-group', {
      method: 'POST',
      body: JSON.stringify({ ownerUUID, ownerName, groupName, description, maxMembers }),
    });
  }

  async joinGroup(playerUUID: string, playerName: string, groupName: string) {
    return this.request('/join-group', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, playerName, groupName }),
    });
  }

  async leaveGroup(playerUUID: string, groupName: string) {
    return this.request('/leave-group', {
      method: 'POST',
      body: JSON.stringify({ playerUUID, groupName }),
    });
  }

  async getGroupMessages(groupId: string, limit: number = 50) {
    return this.request(`/messages?groupId=${groupId}&limit=${limit}`);
  }
}

export const getPlayerSkin = (username: string, uuid?: string) => {
  if (uuid) {
    return `https://crafatar.com/avatars/${uuid}?size=64&overlay`;
  }
  
  if (username) {
    return `https://crafatar.com/avatars/${username}?size=64&overlay`;
  }
  
  return 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
};

export const getPlayerHead = (username: string, uuid?: string, size: number = 64) => {
  if (uuid && uuid.length === 36 && uuid.includes('-')) {
    return `https://crafatar.com/avatars/${uuid}?size=${size}&overlay`;
  }
  
  if (username) {
    return `https://crafatar.com/avatars/${username}?size=${size}&overlay`;
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

export const api = new MinechatAPI();
