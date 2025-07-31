/* eslint-disable @typescript-eslint/no-unsafe-function-type */
import { WEBSOCKET_URL, WS_EVENTS } from './constants';

export interface WebSocketMessage {
  type: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any;
  timestamp?: number;
}

export interface AuthData {
  username: string;
  password: string;
}

export interface MessageData {
  target?: string;
  group?: string;
  message: string;
}

export interface OnlinePlayer {
  name: string;
  uuid: string;
  displayName: string;
}

export class MinechatWebSocket {
  private ws: WebSocket | null = null;
  private url: string;
  private authenticated: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;
  private reconnectDelay: number = 3000;
  private messageQueue: WebSocketMessage[] = [];
  private eventListeners: Map<string, Function[]> = new Map();

  constructor(url: string = WEBSOCKET_URL) {
    this.url = url;
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          console.log('Connected to Minechat WebSocket');
          this.reconnectAttempts = 0;
          this.processMessageQueue();
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
          this.authenticated = false;
          this.scheduleReconnect();
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

  private handleMessage(message: WebSocketMessage) {
    const { type, data } = message;

    // Emit to registered listeners
    const listeners = this.eventListeners.get(type) || [];
    listeners.forEach(listener => listener(data));

    // Handle specific message types
    switch (type) {
      case WS_EVENTS.AUTH_RESPONSE:
        this.authenticated = data.success;
        if (data.success) {
          console.log('WebSocket authentication successful');
        } else {
          console.error('WebSocket authentication failed:', data.message);
        }
        break;

      case WS_EVENTS.FRIEND_MESSAGE:
        console.log(`Friend message from ${data.sender}: ${data.message}`);
        break;

      case WS_EVENTS.GROUP_MESSAGE:
        console.log(`Group message in ${data.group} from ${data.sender}: ${data.message}`);
        break;

      case WS_EVENTS.ERROR:
        console.error('WebSocket error:', data.message);
        break;

      case WS_EVENTS.PONG:
        // Handle ping response
        break;
    }
  }

  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    setTimeout(() => {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      this.connect().catch(error => {
        console.error('Reconnection failed:', error);
      });
    }, this.reconnectDelay * this.reconnectAttempts);
  }

  private processMessageQueue() {
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift();
      if (message) {
        this.send(message);
      }
    }
  }

  send(message: WebSocketMessage) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  // Authentication
  authenticate(username: string, password: string) {
    this.send({
      type: WS_EVENTS.AUTH,
      data: { username, password }
    });
  }

  // Friend Management
  getFriends() {
    this.send({
      type: WS_EVENTS.GET_FRIENDS,
      data: {}
    });
  }

  sendFriendRequest(targetName: string) {
    this.send({
      type: WS_EVENTS.SEND_FRIEND_REQUEST,
      data: { targetName }
    });
  }

  acceptFriendRequest(requesterName: string) {
    this.send({
      type: WS_EVENTS.ACCEPT_FRIEND_REQUEST,
      data: { requesterName }
    });
  }

  rejectFriendRequest(requesterName: string) {
    this.send({
      type: WS_EVENTS.REJECT_FRIEND_REQUEST,
      data: { requesterName }
    });
  }

  getFriendRequests() {
    this.send({
      type: WS_EVENTS.GET_FRIEND_REQUESTS,
      data: {}
    });
  }

  // Messaging
  sendFriendMessage(target: string, message: string) {
    this.send({
      type: WS_EVENTS.FRIEND_MESSAGE,
      data: { target, message }
    });
  }

  sendGroupMessage(group: string, message: string) {
    this.send({
      type: WS_EVENTS.GROUP_MESSAGE,
      data: { group, message }
    });
  }

  // Group Management
  getGroups() {
    this.send({
      type: WS_EVENTS.GET_GROUPS,
      data: {}
    });
  }

  getGroupMessages(groupId: string, limit: number = 50) {
    this.send({
      type: WS_EVENTS.GET_GROUP_MESSAGES,
      data: { groupId, limit }
    });
  }

  // Online Players
  getOnlinePlayers() {
    this.send({
      type: WS_EVENTS.GET_ONLINE_PLAYERS,
      data: {}
    });
  }

  // Utility
  ping() {
    this.send({
      type: WS_EVENTS.PING,
      data: {}
    });
  }

  // Event Listeners
  on(event: string, listener: Function) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)!.push(listener);
  }

  off(event: string, listener: Function) {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  // Connection management
  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.authenticated = false;
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  isAuthenticated(): boolean {
    return this.authenticated;
  }
}

// Export default instance
export const websocket = new MinechatWebSocket();
