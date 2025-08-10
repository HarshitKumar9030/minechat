// =============================================================================
// SERVER CONFIGURATION - CUSTOMIZE THESE VALUES FOR YOUR SERVER
// =============================================================================

// Basic Server Information
export const SERVER_NAME = 'Minechat';
export const SERVER_DESCRIPTION = 'A cross-platform chat, friend, and group management plugin for Minecraft';
export const SERVER_IP = 'minechat.shriju.me'; // Set your own IP here
export const SERVER_VERSION = '1.21.8';

// Homepage Content
export const HOMEPAGE_HEADING = SERVER_NAME;
export const HOMEPAGE_SUBHEADING = SERVER_DESCRIPTION;
export const HOMEPAGE_WELCOME_MESSAGE = `Welcome to ${SERVER_NAME}! Connect with friends, manage groups, and chat with your friends on ${SERVER_NAME} from your browser.`;

// API Configuration
export const API_BASE_URL = 'http://localhost:8080/api';
export const WS_URL = 'ws://localhost:8081/ws';



// For production, update these to your domain:
// export const API_BASE_URL = 'https://yourserver.com/api';
// export const WS_URL = 'wss://yourserver.com/ws';

// change colors from globals.css

// Social Links (set to null to hide)
export const SOCIAL_LINKS = {
  discord: 'https://discord.gg/yourserver',
  website: 'https://minechat.shriju.me',
  twitter: null, // Set to null to hide
  youtube: null, // Set to null to hide
  instagram: null, // Set to null to hide
  github: 'https://github.com/harshitkumar9030/minechat'
};

// Features Configuration
export const FEATURES = {
  friendSystem: true,
  groupChat: true,
  privateMessages: true,
  onlineStatus: true,
  messageHistory: true,
  notifications: true,
  fileSharing: false, // Set to true if you want to enable file sharing
  voiceChat: false, // Set to true if you want to enable voice chat
};

// Chat Configuration
export const CHAT_CONFIG = {
  maxMessageLength: 256,
  maxGroupMembers: 50,
  maxFriends: 100,
  messageHistoryLimit: 100,
  typingIndicatorTimeout: 3000, // 3 seconds
  reconnectInterval: 5000, // 5 seconds
  pingInterval: 30000, // 30 seconds
};

// UI Configuration
export const UI_CONFIG = {
  darkMode: true,
  showPlayerAvatars: true,
  showOnlineStatus: true,
  showTimestamps: true,
  showRanks: true,
  compactMode: false,
  soundNotifications: true,
  desktopNotifications: true,
};



// Legal & Policy Links
export const LEGAL_LINKS = {
  privacyPolicy: '/privacy',
  termsOfService: '/terms',
  rules: '/rules',
  support: '/support',
};

// Error Messages
export const ERROR_MESSAGES = {
  connectionFailed: 'Failed to connect to the server. Please try again.',
  authenticationFailed: 'Invalid username or password.',
  sessionExpired: 'Your session has expired. Please log in again.',
  messageNotSent: 'Failed to send message. Please try again.',
  friendRequestFailed: 'Failed to send friend request.',
  groupJoinFailed: 'Failed to join group.',
  unknownError: 'An unexpected error occurred.',
};

// Success Messages
export const SUCCESS_MESSAGES = {
  loginSuccess: 'Successfully logged in!',
  messageSent: 'Message sent successfully!',
  friendRequestSent: 'Friend request sent!',
  friendRequestAccepted: 'Friend request accepted!',
  groupJoined: 'Successfully joined group!',
  groupCreated: 'Group created successfully!',
};

// Default Settings for New Users
export const DEFAULT_USER_SETTINGS = {
  theme: 'dark',
  notifications: true,
  soundEnabled: true,
  autoConnect: true,
  showOnlineStatus: true,
  compactMode: false,
};

// Rate Limiting (matches server-side limits)
export const RATE_LIMITS = {
  messagesPerMinute: 30,
  friendRequestsPerHour: 10,
  groupJoinsPerHour: 5,
  loginAttemptsPerHour: 10,
};

// Footer Information
export const FOOTER_TEXT = `© 2025 ${SERVER_NAME}. All rights reserved.`;
export const FOOTER_LINKS = [
  { name: 'Home', href: '/' },
  { name: 'Rules', href: '/rules' },
  { name: 'Support', href: '/support' },
  { name: 'Discord', href: SOCIAL_LINKS.discord },
].filter(link => link.href); // Remove links with null href

// =============================================================================
// ADVANCED CONFIGURATION (Usually don't need to change these)
// =============================================================================

export const WS_EVENTS = {
  AUTH: 'auth',
  AUTH_RESPONSE: 'auth_response',
  FRIEND_MESSAGE: 'friend_message',
  GROUP_MESSAGE: 'group_message',
  MESSAGE_SENT: 'message_sent',
  GET_FRIENDS: 'get_friends',
  FRIENDS_LIST: 'friends_list',
  SEND_FRIEND_REQUEST: 'send_friend_request',
  ACCEPT_FRIEND_REQUEST: 'accept_friend_request',
  REJECT_FRIEND_REQUEST: 'reject_friend_request',
  GET_FRIEND_REQUESTS: 'get_friend_requests',
  FRIEND_REQUESTS: 'friend_requests',
  GET_GROUPS: 'get_groups',
  GROUPS_LIST: 'groups_list',
  GET_GROUP_MESSAGES: 'get_group_messages',
  GROUP_MESSAGES: 'group_messages',
  GET_ONLINE_PLAYERS: 'get_online_players',
  ONLINE_PLAYERS: 'online_players',
  PING: 'ping',
  PONG: 'pong',
  ERROR: 'error',
};


export const STORAGE_KEYS = {
  AUTH_TOKEN: 'minechat_auth_token',
  USER_DATA: 'minechat_user_data',
  SETTINGS: 'minechat_settings',
  CHAT_HISTORY: 'minechat_chat_history',
  FRIEND_LIST: 'minechat_friends',
  GROUP_LIST: 'minechat_groups',
};

export const VALIDATION = {
  username: {
    minLength: 3,
    maxLength: 16,
    pattern: /^[a-zA-Z0-9_]+$/,
  },
  password: {
    minLength: 4,
    maxLength: 32,
  },
  message: {
    minLength: 1,
    maxLength: CHAT_CONFIG.maxMessageLength,
  },
  groupName: {
    minLength: 3,
    maxLength: 32,
    pattern: /^[a-zA-Z0-9_\s]+$/,
  },
};