/* eslint-disable @typescript-eslint/no-explicit-any */
import { WS_URL } from './constants';

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: number;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  sessionId?: string;
  username?: string;
}

export interface WebSocketError {
  message: string;
  code?: number;
  type?: string;
}

export interface ChatMessage {
  messageId: string;
  groupId?: string;
  groupName?: string;
  senderUUID: string;
  senderName: string;
  content: string;
  timestamp: number;
  messageType: 'TEXT' | 'SYSTEM' | 'ANNOUNCEMENT';
  source: 'minecraft' | 'web';
  rank?: string;
  formattedRank?: string;
}

export interface FriendMessage {
  senderUUID: string;
  senderName: string;
  targetUUID: string;
  targetName: string;
  content: string;
  timestamp: number;
  source: 'minecraft' | 'web';
}

export interface PlayerStatus {
  playerUUID: string;
  playerName: string;
  online: boolean;
  rank?: string;
  formattedRank?: string;
}

export interface GroupNotification {
  type: 'member_joined' | 'member_left' | 'member_kicked' | 'member_banned' | 'member_promoted' | 'member_demoted' | 'group_settings_changed' | 'group_announcement';
  groupId: string;
  groupName: string;
  actorUUID?: string;
  actorName?: string;
  targetUUID?: string;
  targetName?: string;
  message: string;
  timestamp: number;
  data?: any;
}

export interface GroupInviteNotification {
  inviteId: string;
  groupId: string;
  groupName: string;
  inviterUUID: string;
  inviterName: string;
  message?: string;
  timestamp: number;
}

export interface GroupMemberUpdate {
  groupId: string;
  memberUUID: string;
  memberName: string;
  action: 'joined' | 'left' | 'kicked' | 'banned' | 'promoted' | 'demoted' | 'muted' | 'unmuted';
  actorUUID?: string;
  actorName?: string;
  newRole?: string;
  reason?: string;
  timestamp: number;
}

export class MinechatWebSocket {
  private ws: WebSocket | null = null;
  private url: string;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 3000;
  private isAuthenticated = false;
  private authData: { username: string; password: string } | null = null;
  private listeners: Map<string, ((data: any) => void)[]> = new Map();
  private heartbeatInterval: NodeJS.Timeout | null = null;

  constructor(url: string = WS_URL) {
    this.url = url;
  }

  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
          this.startHeartbeat();
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data);
            this.handleMessage(message);
          } catch (error) {
            console.error('Failed to parse WebSocket message:', error);
          }
        };

        this.ws.onclose = (event) => {
          console.log('WebSocket connection closed:', event.code, event.reason);
          this.isAuthenticated = false;
          this.stopHeartbeat();

          if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
            console.log(`Attempting to reconnect... (${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);
            this.reconnectAttempts++;
            setTimeout(() => {
              if (this.authData) {
                this.connect().then(() => {
                  this.authenticate(this.authData!.username, this.authData!.password);
                }).catch(error => {
                  console.error('Reconnection failed:', error);
                });
              }
            }, this.reconnectInterval);
          }
          if (event.code === 1001 && this.authData) {
            setTimeout(() => {
              this.connect().then(() => {
                this.authenticate(this.authData!.username, this.authData!.password);
              }).catch(error => console.error('Reconnect after idle timeout failed:', error));
            }, 1000);
          }
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          reject(error);
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  async authenticate(username: string, password: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.authData = { username, password };

      const handleAuthResponse = (data: any) => {
        console.log('WebSocket auth response received:', JSON.stringify(data, null, 2));
        
        let success = false;
        if (data && typeof data === 'object') {
          success = data.success === true || data.authenticated === true;
        }
        
        this.isAuthenticated = success;
        this.removeListener('auth_response', handleAuthResponse);
        resolve(success);
      };

      this.addListener('auth_response', handleAuthResponse);

      this.send({
        type: 'auth',
        data: { username, password },
        timestamp: Date.now()
      });
    });
  }

  disconnect(): void {
    this.isAuthenticated = false;
  this.stopHeartbeat();
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
  }

  private handleMessage(message: WebSocketMessage): void {
    if (message.type === 'auth_response') {
      const authData = {
        success: (message as any).success,
        message: (message as any).message,
        username: (message as any).username,
        sessionId: (message as any).sessionId
      };
      this.emit(message.type, authData);
      return;
    }
    
    this.emit(message.type, message.data);

    switch (message.type) {
      case 'group_message':
        this.emit('message', {
          messageId: message.data.messageId || crypto.randomUUID(),
          groupId: message.data.groupId || message.data.group, 
          groupName: message.data.group, 
          senderUUID: message.data.senderUUID || crypto.randomUUID(),
          senderName: message.data.senderName || message.data.sender || message.data.username || message.data.playerName, 
          content: message.data.content ?? message.data.message ?? '',
          timestamp: message.data.timestamp || Date.now(),
          messageType: message.data.messageType || 'TEXT',
          source: message.data.source || 'minecraft',
          rank: message.data.rank,
          formattedRank: message.data.formattedRank
        } as ChatMessage);
        break;

      case 'group_announcement':
        this.emit('message', {
          messageId: message.data.messageId || crypto.randomUUID(),
          groupId: message.data.groupId,
          senderUUID: message.data.senderUUID,
          senderName: message.data.senderName,
          content: message.data.content,
          timestamp: message.data.timestamp,
          messageType: 'ANNOUNCEMENT',
          source: message.data.source || 'minecraft',
          rank: message.data.rank,
          formattedRank: message.data.formattedRank
        } as ChatMessage);
        break;

      case 'message_sent':

        this.emit('message_sent', message.data);
        break;

      case 'group_notification':
        this.emit('groupNotification', message.data as GroupNotification);
        break;

      case 'group_invite':
        this.emit('groupInvite', message.data as GroupInviteNotification);
        break;

      case 'group_member_update':
        this.emit('groupMemberUpdate', message.data as GroupMemberUpdate);
        break;

      case 'friend_message':
        this.emit('friendMessage', message.data as FriendMessage);
        break;

      case 'direct_message':
        this.emit('friendMessage', {
          senderUUID: message.data.senderUUID || '',
          senderName: message.data.sender || message.data.senderName,
          targetUUID: message.data.targetUUID || '',
          targetName: message.data.target || message.data.targetName,
          content: message.data.content || message.data.message,
          timestamp: message.timestamp || Date.now(),
          source: message.data.source || 'minecraft'
        } as FriendMessage);
        break;

      case 'player_join':
      case 'player_leave':
        this.emit('playerStatusChange', {
          playerUUID: message.data.playerUUID,
          playerName: message.data.playerName,
          online: message.type === 'player_join',
          rank: message.data.rank,
          formattedRank: message.data.formattedRank
        } as PlayerStatus);
        break;

      case 'pong':
        break;

      case 'error':
        console.error('WebSocket error from server:', message.data);
        this.emit('error', message.data);
        break;

      case 'connection':
        break;

      default:
        break;
    }
  }

  private send(message: WebSocketMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket not connected, message not sent:', message);
    }
  }

  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.send({
        type: 'ping',
        data: {},
        timestamp: Date.now()
      });
    }, 30000);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private addListener(event: string, callback: (data: any) => void): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event)!.push(callback);
  }

  private removeListener(event: string, callback: (data: any) => void): void {
    const eventListeners = this.listeners.get(event);
    if (eventListeners) {
      const index = eventListeners.indexOf(callback);
      if (index > -1) {
        eventListeners.splice(index, 1);
      }
    }
  }

  private emit(event: string, data: any): void {
    const eventListeners = this.listeners.get(event);
    if (eventListeners) {
      eventListeners.forEach(callback => callback(data));
    }
  }

  onMessage(callback: (message: ChatMessage) => void): () => void {
    this.addListener('message', callback as unknown as (data: any) => void);
    return () => this.removeListener('message', callback as unknown as (data: any) => void);
  }

  offMessage(callback: (message: ChatMessage) => void): void {
    this.removeListener('message', callback as unknown as (data: any) => void);
  }

  onFriendMessage(callback: (message: FriendMessage) => void): void {
    this.addListener('friendMessage', callback);
  }

  onPlayerStatusChange(callback: (status: PlayerStatus) => void): void {
    this.addListener('playerStatusChange', callback);
  }

  onGroupNotification(callback: (notification: GroupNotification) => void): void {
    this.addListener('groupNotification', callback);
  }

  onGroupInvite(callback: (invite: GroupInviteNotification) => void): void {
    this.addListener('groupInvite', callback);
  }

  onGroupMemberUpdate(callback: (update: GroupMemberUpdate) => void): void {
    this.addListener('groupMemberUpdate', callback);
  }

  onError(callback: (error: any) => void): void {
    this.addListener('error', callback);
  }

  onMessageSent(callback: (data: any) => void): void {
    this.addListener('message_sent', callback);
  }

  sendGroupMessage(groupId: string, content: string, groupName?: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot send group message');
      return;
    }

    this.send({
      type: 'group_message',
      data: {
        groupId: groupId,
        group: groupName ?? undefined,
        content: content,
        message: content
      },
      timestamp: Date.now()
    });
  }

  sendGroupAnnouncement(groupId: string, content: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot send group announcement');
      return;
    }

    this.send({
      type: 'group_announcement',
      data: {
        groupId,
        content,
        messageType: 'ANNOUNCEMENT'
      },
      timestamp: Date.now()
    });
  }

  sendFriendMessage(targetName: string, content: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot send friend message');
      return;
    }

    this.send({
      type: 'friend_message',
      data: {
        target: targetName,
        content
      },
      timestamp: Date.now()
    });
  }

  sendDirectMessage(targetName: string, content: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot send direct message');
      return;
    }

    this.send({
      type: 'direct_message',
      data: {
        target: targetName,
        content
      },
      timestamp: Date.now()
    });
  }

  kickGroupMember(groupId: string, targetUUID: string, reason?: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot kick member');
      return;
    }

    this.send({
      type: 'kick_group_member',
      data: {
        groupId,
        targetUUID,
        reason
      },
      timestamp: Date.now()
    });
  }

  promoteGroupMember(groupId: string, targetUUID: string): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot promote member');
      return;
    }

    this.send({
      type: 'promote_group_member',
      data: {
        groupId,
        targetUUID
      },
      timestamp: Date.now()
    });
  }

  muteGroupMember(groupId: string, targetUUID: string, duration?: number): void {
    if (!this.isAuthenticated) {
      console.warn('Not authenticated, cannot mute member');
      return;
    }

    this.send({
      type: 'mute_group_member',
      data: {
        groupId,
        targetUUID,
        duration
      },
      timestamp: Date.now()
    });
  }

  requestFriends(): void {
    this.send({
      type: 'get_friends',
      data: {},
      timestamp: Date.now()
    });
  }

  requestGroups(): void {
    this.send({
      type: 'get_groups',
      data: {},
      timestamp: Date.now()
    });
  }

  requestGroupMessages(groupId: string, limit: number = 50): void {
    this.send({
      type: 'get_group_messages',
      data: {
        groupId,
        limit
      },
      timestamp: Date.now()
    });
  }

  requestGroupMembers(groupId: string): void {
    this.send({
      type: 'get_group_members',
      data: {
        groupId
      },
      timestamp: Date.now()
    });
  }

  requestOnlinePlayers(): void {
    this.send({
      type: 'get_online_players',
      data: {},
      timestamp: Date.now()
    });
  }

  onOnlinePlayers(callback: (payload: { players: Array<{ name: string; uuid: string; displayName?: string }>; count: number }) => void): void {
    this.addListener('online_players', callback as unknown as (data: any) => void);
  }

  requestFriendRequests(): void {
    this.send({
      type: 'get_friend_requests',
      data: {},
      timestamp: Date.now()
    });
  }

  sendFriendRequest(targetName: string): void {
    this.send({
      type: 'send_friend_request',
      data: {
        targetName
      },
      timestamp: Date.now()
    });
  }

  acceptFriendRequest(requesterName: string): void {
    this.send({
      type: 'accept_friend_request',
      data: {
        requesterName
      },
      timestamp: Date.now()
    });
  }

  rejectFriendRequest(requesterName: string): void {
    this.send({
      type: 'reject_friend_request',
      data: {
        requesterName
      },
      timestamp: Date.now()
    });
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  isAuth(): boolean {
    return this.isAuthenticated;
  }
}

export async function connectWebSocket(username: string, password: string): Promise<MinechatWebSocket> {
  const ws = new MinechatWebSocket();
  await ws.connect();

  const authenticated = await ws.authenticate(username, password);
  if (!authenticated) {
    throw new Error('WebSocket authentication failed');
  }

  return ws;
}
