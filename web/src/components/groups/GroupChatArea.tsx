/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Send, ChevronDown, Crown, Shield } from 'lucide-react';
import { GroupInfo, api, GroupMember as ApiGroupMember } from '@/lib/api';
import RankBadge from './RankBadge';
import { MinechatWebSocket, ChatMessage } from '@/lib/websocket';

interface GroupChatAreaProps {
  group: GroupInfo | null;
  user?: { playerUUID: string; playerName: string } | null;
  websocket?: MinechatWebSocket | null;
  isConnected: boolean;
}

interface GroupMessage {
  messageId: string;
  senderId: string;
  senderName: string;
  message: string;
  timestamp: number;
  source: 'minecraft' | 'web';
}

interface GroupMember extends ApiGroupMember {
  online: boolean;
  rank?: string;
  formattedRank?: string;
}

const GroupChatArea: React.FC<GroupChatAreaProps> = ({
  group,
  user,
  websocket,
  isConnected
}) => {
  const [messages, setMessages] = useState<GroupMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [showMembers, setShowMembers] = useState(false);
  const [loading, setLoading] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  const loadMessages = useCallback(async () => {
    if (!group) return;
    
    try {
      setLoading(true);
    const response = await api.getGroupMessages(group.groupId, 50);
      if (response.messages) {
        const formattedMessages: GroupMessage[] = response.messages.map((msg) => ({
          messageId: msg.messageId || `${msg.timestamp}-${Math.random()}`,
          senderId: msg.senderUUID,
          senderName: msg.senderName,
      message: msg.content,
          timestamp: msg.timestamp,
          source: 'minecraft' // def to mc, websocket will handle web messages
        }));
        setMessages(formattedMessages);
      }
    } catch (error) {
      console.error('Failed to load messages:', error);
    } finally {
      setLoading(false);
    }
  }, [group]);

  const loadMembers = useCallback(async () => {
    if (!group) return;
    
    try {
      const response = await api.getGroupMembers(group.groupId);
      if (response.members) {
        setMembers(response.members);
      }
    } catch (error) {
      console.error('Failed to load members:', error);
    }
  }, [group]);

  useEffect(() => {
    if (group) {
      loadMessages();
      loadMembers();
      
      const memberRefreshInterval = setInterval(() => {
        loadMembers();
      }, 30000);
      
      return () => {
        clearInterval(memberRefreshInterval);
      };
    }
  }, [group, loadMessages, loadMembers]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const isSameGroup = (msg: ChatMessage, grp: GroupInfo) => {
    const candidatesFromMsg = [msg.groupId, (msg as any).groupName].filter(Boolean);
    const candidatesFromGroup = [grp.groupId, grp.groupName];
    return candidatesFromMsg.some(v => candidatesFromGroup.includes(v as string));
  };

  useEffect(() => {
    if (!websocket || !group) return;

  const handleMessage = (message: ChatMessage) => {
      if (isSameGroup(message, group)) {
        const newMsg: GroupMessage = {
          messageId: message.messageId || `${Date.now()}-${Math.random()}`,
          senderId: message.senderUUID || '',
      senderName: message.senderName || (message as any).sender || (message as any).username || 'Unknown',
      message: message.content || (message as any).message || '',
          timestamp: message.timestamp || Date.now(),
          source: message.source || 'web'
        };
        setMessages(prev => [...prev, newMsg]);
      }
    };

    const unsubscribe = websocket.onMessage(handleMessage);

    return () => {
      unsubscribe?.();
    };
  }, [websocket, group]);

  const handleSendMessage = async () => {
    if (!newMessage.trim() || !user || !websocket || !isConnected || !group) return;

    try {
  websocket.sendGroupMessage(group.groupId, newMessage.trim(), group.groupName);
      setNewMessage('');
    } catch (error) {
      console.error('Failed to send message:', error);
    }
  };

  if (!group) {
    return (
      <div className="flex flex-col h-full items-center justify-center bg-neutral-900 border border-neutral-700 rounded-lg">
        <div className="text-center text-neutral-400">
          <div className="w-16 h-16 bg-neutral-800 rounded-lg flex items-center justify-center mb-4 mx-auto">
            <Crown className="w-8 h-8 text-neutral-600" />
          </div>
          <h3 className="text-lg font-minecraftia mb-2">No Group Selected</h3>
          <p className="text-sm">Select a group from the sidebar to start chatting</p>
        </div>
      </div>
    );
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) return 'now';
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'OWNER':
        return <Crown className="w-3 h-3 text-yellow-400" />;
      case 'MODERATOR':
        return <Shield className="w-3 h-3 text-blue-400" />;
      default:
        return null;
    }
  };

  const onlineMembers = members.filter(m => m.online);
  const offlineMembers = members.filter(m => !m.online);

  return (
    <div className="flex flex-col h-full min-h-0">
      {/* Header */}
      <div className="bg-neutral-800 border border-neutral-700 rounded-t-lg p-4 flex items-center justify-between">
        <div>
          <h2 className="font-minecraftia text-lg text-neutral-100">
            #{group.groupName}
          </h2>
          <p className="text-sm text-neutral-400">
            {group.memberCount} members
          </p>
        </div>
        <button
          onClick={() => setShowMembers(!showMembers)}
          className="px-3 py-2 bg-neutral-700 hover:bg-neutral-600 rounded-lg transition-colors flex items-center gap-2"
        >
          <span className="text-sm text-neutral-300">Members</span>
          <ChevronDown className={`w-4 h-4 text-neutral-400 transition-transform ${showMembers ? 'rotate-180' : ''}`} />
        </button>
      </div>

      <div className="flex flex-1 min-h-0">
  <div className="flex-1 flex flex-col min-h-0">
          <div 
            ref={chatContainerRef}
            className="flex-1 bg-neutral-900 border-x border-neutral-700 p-4 overflow-y-auto min-h-0 scrollbar-thin"
          >
            {loading ? (
              <div className="flex items-center justify-center h-full">
                <div className="w-8 h-8 border-2 border-neutral-400 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : messages.length === 0 ? (
              <div className="flex items-center justify-center h-full text-neutral-400">
                <div className="text-center">
                  <p className="text-lg font-minecraftia mb-2">No messages yet</p>
                  <p className="text-sm">Be the first to send a message!</p>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((message) => (
                  <div key={message.messageId} className="group">
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 w-8 h-8 bg-neutral-700 rounded-lg flex items-center justify-center">
                        <span className="text-xs font-bold text-neutral-300">
                          {message.senderName.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-minecraftia text-sm text-neutral-200">
                            {message.senderName}
                          </span>
                          {(() => {
                            const member = members.find(m => m.playerName === message.senderName);
                            if (member?.formattedRank) {
                              return <RankBadge rank={member.formattedRank} />;
                            }
                            return null;
                          })()}
                          <span className="text-xs text-neutral-500">
                            {formatTimestamp(message.timestamp)}
                          </span>
                          {message.source === 'minecraft' && (
                            <span className="text-xs bg-green-600 text-white px-1.5 py-0.5 rounded">
                              MC
                            </span>
                          )}
                        </div>
                        <p className="text-neutral-300 text-sm break-words">
                          {message.message}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <div className="bg-neutral-800 border-x border-b border-neutral-700 rounded-b-lg p-4">
            {!isConnected ? (
              <div className="bg-red-900/20 border border-red-600 rounded-lg p-3 text-center">
                <p className="text-red-400 text-sm">
                  Disconnected from server. Reconnecting...
                </p>
              </div>
            ) : (
              <div className="flex items-end gap-3">
                <div className="flex-1">
                  <textarea
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyDown={handleKeyPress}
                    placeholder={`Message #${group.groupName}...`}
                    rows={1}
                    className="w-full px-4 py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 resize-none focus:outline-none focus:border-yellow-600"
                    style={{ minHeight: '44px', maxHeight: '120px' }}
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleSendMessage}
                    disabled={!newMessage.trim() || !isConnected}
                    className="px-4 py-4 bg-yellow-600 hover:bg-yellow-700 mb-2 disabled:bg-neutral-600 disabled:cursor-not-allowed text-neutral-900 disabled:text-neutral-400 rounded-lg transition-colors flex items-center gap-2"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {showMembers && (
          <div className="w-64 bg-neutral-800 border-r border-b border-neutral-700 rounded-br-lg">
            <div className="p-4">
              <h3 className="font-minecraftia text-sm text-neutral-200 mb-3">
                Members ({members.length})
              </h3>
              
              <div className="space-y-3 max-h-96 overflow-y-auto scrollbar-thin">
                {onlineMembers.length > 0 && (
                  <div>
                    <h4 className="text-xs text-neutral-400 uppercase tracking-wide mb-2">
                      Online — {onlineMembers.length}
                    </h4>
                    <div className="space-y-1">
                      {onlineMembers.map((member) => (
                        <div key={member.playerUUID} className="flex items-center gap-2 p-2 rounded hover:bg-neutral-700">
                          <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                          <div className="flex items-center gap-1">
                            {getRoleIcon(member.role)}
                            <span className="text-sm text-neutral-200">
                              {member.playerName}
                            </span>
                          </div>
                          {member.formattedRank && (
                            <RankBadge rank={member.formattedRank} />
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {offlineMembers.length > 0 && (
                  <div>
                    <h4 className="text-xs text-neutral-400 uppercase tracking-wide mb-2">
                      Offline — {offlineMembers.length}
                    </h4>
                    <div className="space-y-1">
                      {offlineMembers.map((member) => (
                        <div key={member.playerUUID} className="flex items-center gap-2 p-2 rounded hover:bg-neutral-700 opacity-60">
                          <div className="w-2 h-2 bg-neutral-500 rounded-full"></div>
                          <div className="flex items-center gap-1">
                            {getRoleIcon(member.role)}
                            <span className="text-sm text-neutral-300">
                              {member.playerName}
                            </span>
                          </div>
                          {member.formattedRank && (
                            <RankBadge rank={member.formattedRank} />
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default GroupChatArea;
