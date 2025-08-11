/* eslint-disable @typescript-eslint/no-unused-vars */
'use client';
import React, { useState, useEffect, useCallback } from 'react';
import { api, GroupInfo, GroupMember, GroupMessage, GroupInvite } from '@/lib/api';
import { MinechatWebSocket } from '@/lib/websocket';
import { useRouter } from 'next/navigation';
import CreateGroupModal from '@/modals/CreateGroupModal';
import JoinGroupModal from '@/modals/JoinGroupModal';
import AlreadyMemberModal from '@/modals/AlreadyMemberModal';
import GroupSettingsModal from '@/modals/GroupSettingsModal';
import GroupSidebar, { ViewType } from '@/components/groups/GroupSidebar';
import GroupChatArea from '@/components/groups/GroupChatArea';
import GroupDetailsArea from '@/components/groups/GroupDetailsArea';
import DiscoverGroups from '@/components/groups/DiscoverGroups';
import GroupInvites from '@/components/groups/GroupInvites';
import ConnectionError from '@/components/groups/ConnectionError';
import GroupsHeader from '@/components/groups/GroupsHeader';
import GroupAnnouncements from '@/components/groups/GroupAnnouncements';
import GroupModeration from '@/components/groups/GroupModeration';

const GroupsPage = () => {
  const [groups, setGroups] = useState<GroupInfo[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<GroupInfo | null>(null);
  const [groupMembers, setGroupMembers] = useState<GroupMember[]>([]);
  const [allGroupMessages, setAllGroupMessages] = useState<Record<string, GroupMessage[]>>({});
  const [groupInvites, setGroupInvites] = useState<GroupInvite[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [userRole, setUserRole] = useState<string>('MEMBER');
  const [searchTerm, setSearchTerm] = useState('');
  const [user, setUser] = useState<{ playerUUID: string; playerName: string } | null>(null);
  const [view, setView] = useState<ViewType>('my-groups');
  const [publicGroups, setPublicGroups] = useState<GroupInfo[]>([]);
  const [recommendedGroups, setRecommendedGroups] = useState<GroupInfo[]>([]);
  const [trendingGroups, setTrendingGroups] = useState<GroupInfo[]>([]);
  const [groupStats, setGroupStats] = useState<{
    memberCount: number;
    messageCount: number;
    createdAt: number;
    mostActiveMembers: { playerName: string; messageCount: number }[];
    recentActivity: { date: string; messages: number; joins: number }[];
  } | null>(null);
  const [connectionError, setConnectionError] = useState(false);
  const [websocket, setWebsocket] = useState<MinechatWebSocket | null>(null);
  const [messageInput, setMessageInput] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [showAlreadyMemberModal, setShowAlreadyMemberModal] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [alreadyMemberGroup, setAlreadyMemberGroup] = useState<GroupInfo | null>(null);
  const [managementGroup, setManagementGroup] = useState<GroupInfo | null>(null);
  const [managementMembers, setManagementMembers] = useState<GroupMember[]>([]);
  const [managementRole, setManagementRole] = useState<string>('MEMBER');
  const [managementGroupDetails, setManagementGroupDetails] = useState<GroupInfo | null>(null);

  const groupMessages = selectedGroup ? (allGroupMessages[selectedGroup.groupId] || []) : [];

  const router = useRouter();

  useEffect(() => {
    const savedUser = localStorage.getItem('minechat_user');
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser({ 
          playerUUID: userData.playerUUID, 
          playerName: userData.playerName 
        });
      } catch (error) {
        console.error('Failed to parse user data:', error);
        router.push('/');
      }
    } else {
      router.push('/');
    }
  }, [router]);

  const initializeWebSocket = useCallback(async () => {
    if (!user || websocket?.isConnected()) return;

    try {
      const savedAuth = localStorage.getItem('minechat_auth');
      if (!savedAuth) {
        console.warn('No authentication data found. Please log in again.');
        router.push('/');
        return;
      }

      const { username, password } = JSON.parse(savedAuth);
      
      const ws = new MinechatWebSocket();
      await ws.connect();
      const authenticated = await ws.authenticate(username, password);
      
      if (authenticated) {
        setWebsocket(ws);
        setConnectionError(false);
        
        ws.onMessage((message) => {
          const groupMessage: GroupMessage = {
            messageId: message.messageId || `msg-${Date.now()}-${Math.random()}`,
            groupId: message.groupId || message.groupName || '', 
            senderUUID: message.senderUUID,
            senderName: message.senderName,
            content: message.content,
            timestamp: message.timestamp,
            messageType: message.messageType,
            editedAt: undefined,
            rank: message.rank,
            formattedRank: message.formattedRank
          };
          
          setAllGroupMessages(prev => {
            const messageGroupId = groupMessage.groupId;
            const currentMessages = prev[messageGroupId] || [];
            
            const withoutTemp = currentMessages.filter(msg => 
              !(msg.messageId && msg.messageId.startsWith('temp-') && 
                msg.content === groupMessage.content && 
                msg.senderName === groupMessage.senderName)
            );
            
            const exists = withoutTemp.some(msg => msg.messageId === groupMessage.messageId);
            if (exists) return prev;
            
            return {
              ...prev,
              [messageGroupId]: [...withoutTemp, groupMessage]
            };
          });
        });

        ws.onError((error) => {
          console.error('WebSocket error:', error);
          setConnectionError(true);
        });

        ws.onGroupNotification(() => {
        });

        ws.onMessageSent(() => {
          setAllGroupMessages(prev => {
            const updated: Record<string, GroupMessage[]> = {};
            Object.keys(prev).forEach(groupId => {
              updated[groupId] = prev[groupId].filter(msg => 
                !msg.messageId || !msg.messageId.startsWith('temp-')
              );
            });
            return updated;
          });
        });
      } else {
        console.error('WebSocket authentication failed');
        setConnectionError(true);
        localStorage.removeItem('minechat_auth');
        router.push('/');
      }
    } catch (error) {
      console.error('Failed to initialize WebSocket:', error);
      setConnectionError(true);
    }
  }, [user, websocket, router]);

  useEffect(() => {
    if (user) {
      initializeWebSocket();
    }
  }, [user, initializeWebSocket]);

  useEffect(() => {
    return () => {
      if (websocket) {
        websocket.disconnect();
      }
    };
  }, [websocket]);

  const sendMessage = async () => {
    if (!websocket || !selectedGroup || !messageInput.trim() || sendingMessage) return;
    
    if (!websocket.isConnected()) {
      console.warn('WebSocket not connected, attempting to reconnect...');
      try {
        await initializeWebSocket();
      } catch (error) {
        console.error('Failed to reconnect WebSocket:', error);
        return;
      }
    }
    
    try {
      setSendingMessage(true);
      
      const tempMessage: GroupMessage = {
        messageId: `temp-${Date.now()}`,
        groupId: selectedGroup.groupId,
        senderUUID: user?.playerUUID || '',
        senderName: user?.playerName || '',
        content: messageInput.trim(),
        timestamp: Date.now(),
        messageType: 'TEXT'
      };
      
      setAllGroupMessages(prev => ({
        ...prev,
        [selectedGroup.groupId]: [...(prev[selectedGroup.groupId] || []), tempMessage]
      }));
      
      websocket.sendGroupMessage(selectedGroup.groupId, messageInput.trim());
      setMessageInput('');
    } catch (error) {
      console.error('Failed to send message:', error);
      alert('Failed to send message. Please try again.');
    } finally {
      setSendingMessage(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleGroupDeleted = (groupId: string) => {
    setGroups(groups.filter(g => g.groupId !== groupId));
    if (selectedGroup?.groupId === groupId) {
      setSelectedGroup(null);
    }
  };

  const handleGroupLeft = (groupId: string) => {
    setGroups(groups.filter(g => g.groupId !== groupId));
    if (selectedGroup?.groupId === groupId) {
      setSelectedGroup(null);
    }
  };

  const loadGroups = useCallback(async () => {
    if (!user) return;
    
    try {
      setLoading(true);
      setConnectionError(false);
      
      const response = await api.getUserGroups(user.playerUUID);
      setGroups(response.groups || []);
      
      if (response.groups && response.groups.length > 0 && !selectedGroup) {
        setSelectedGroup(response.groups[0]);
      }
    } catch (error: unknown) {
      console.error('Failed to load groups:', error);
      if (error && typeof error === 'object' && 'isConnectionError' in error) {
        setConnectionError(true);
      }
    } finally {
      setLoading(false);
    }
  }, [user, selectedGroup]);

  const loadGroupMembers = useCallback(async (groupId: string) => {
    try {
      const response = await api.getGroupMembers(groupId);
      if (response.members) {
        setGroupMembers(response.members);
        
        if (user) {
          const currentUserMember = response.members.find(m => m.playerUUID === user.playerUUID);
          if (currentUserMember) {
            setUserRole(currentUserMember.role || 'MEMBER');
          }
        }
      }
    } catch (error) {
      console.error('Failed to load group members:', error);
    }
  }, [user]);

  const loadGroupDetails = useCallback(async (groupId: string) => {
    try {
      const response = await api.getGroupDetails(groupId);
      if (response.group) {
        setSelectedGroup(prev => {
          if (!prev || prev.groupId !== groupId) return prev;

          const prevMotd = prev.motd || '';
          const nextMotd = response.group.motd || '';

          const prevAnnouncements = Array.isArray(prev.announcements) ? prev.announcements : [];
          const nextAnnouncements = Array.isArray(response.group.announcements) ? response.group.announcements : [];

          const announcementsEqual =
            prevAnnouncements.length === nextAnnouncements.length &&
            prevAnnouncements.every((a, i) => a === nextAnnouncements[i]);

          if (prevMotd === nextMotd && announcementsEqual) {
            return prev; // no changes -> keep same reference to prevent loops
          }

          return { ...prev, ...response.group };
        });
      }
    } catch (error) {
      console.error('Failed to load group details:', error);
    }
  }, []);

  const loadManagementGroupMembers = useCallback(async (groupId: string) => {
    try {
      const response = await api.getGroupMembers(groupId);
      if (response.members) {
        setManagementMembers(response.members);
      }
    } catch (error) {
      console.error('Failed to load management group members:', error);
    }
  }, []);

  const loadGroupStats = useCallback(async (groupId: string) => {
    try {
      const response = await api.getGroupStats(groupId);
      setGroupStats(response);
    } catch (error) {
      console.error('Failed to load group stats:', error);
    }
  }, []);

  const loadGroupInvites = useCallback(async () => {
    if (!user) return;
    
    try {
      const response = await api.getGroupInvites(user.playerUUID);
      setGroupInvites(response.invites || []);
    } catch (error) {
      console.error('Failed to load group invites:', error);
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      loadGroups();
      loadGroupInvites();
    }
  }, [user, loadGroups, loadGroupInvites]);

  const loadPublicGroups = useCallback(async () => {
    if (!user) return;
    
    try {
      const response = await api.getAllPublicGroups();
      setPublicGroups(response.groups || []);
    } catch (error) {
      console.error('Failed to load public groups:', error);
    }
  }, [user]);

  const loadRecommendedGroups = useCallback(async () => {
    if (!user) return;
    
    try {
      const response = await api.getRecommendedGroups(user.playerUUID);
      setRecommendedGroups(response.groups || []);
    } catch (error) {
      console.error('Failed to load recommended groups:', error);
    }
  }, [user]);

  const loadTrendingGroups = useCallback(async () => {
    if (!user) return;
    
    try {
      const response = await api.getTrendingGroups();
      setTrendingGroups(response.groups || []);
    } catch (error) {
      console.error('Failed to load trending groups:', error);
    }
  }, [user]);

  useEffect(() => {
    if (view === 'discover') {
      loadPublicGroups();
      loadRecommendedGroups();
      loadTrendingGroups();
    }
    if (view === 'messages' && !selectedGroup && groups.length > 0) {
      setSelectedGroup(groups[0]);
    }
    if (view === 'management') {
      const manageable = groups.filter(g => g.role === 'OWNER' || g.role === 'ADMIN');
      if (manageable.length > 0) {
        const grp = manageable[0];
        setManagementGroup(grp);
        loadManagementGroupMembers(grp.groupId);
        setManagementRole(grp.role || 'MEMBER');
        api.getGroupDetails(grp.groupId)
          .then(res => setManagementGroupDetails(res.group))
          .catch(err => console.error('Failed to load management group details:', err));
      } else {
        setManagementGroup(null);
        setManagementGroupDetails(null);
      }
    }
  }, [view, loadPublicGroups, loadRecommendedGroups, loadTrendingGroups, selectedGroup, groups, loadManagementGroupMembers]);

  const [lastLoadedGroupId, setLastLoadedGroupId] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedGroup) return;
    if (lastLoadedGroupId === selectedGroup.groupId) return;

    setLastLoadedGroupId(selectedGroup.groupId);
    loadGroupMembers(selectedGroup.groupId);
    loadGroupStats(selectedGroup.groupId);
    loadGroupDetails(selectedGroup.groupId);
  }, [selectedGroup, lastLoadedGroupId, loadGroupMembers, loadGroupStats, loadGroupDetails]);

  const handleCreateGroup = async (groupData: {
    groupName: string;
    description: string;
    maxMembers: number;
    isPrivate: boolean;
  }) => {
    if (!user) return;

    try {
      setActionLoading('create');
      await api.createGroup({
        ownerUUID: user.playerUUID,
        ownerName: user.playerName,
        ...groupData
      });
      
      setShowCreateModal(false);
      await loadGroups();
    } catch (error) {
      console.error('Failed to create group:', error);
      alert('Failed to create group. Please try again.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleGroupJoined = (group: GroupInfo) => {
    setGroups(prev => [...prev, group]);
    setSelectedGroup(group);
    setView('my-groups');
  };

  const handleAlreadyMember = (group: GroupInfo) => {
    setAlreadyMemberGroup(group);
    setShowAlreadyMemberModal(true);
  };

  const handleInviteAccepted = () => {
    loadGroups();
    loadGroupInvites();
  };

  const handleLeaveGroup = async (groupId: string) => {
    if (!user) return;

    try {
      setActionLoading(`leave-${groupId}`);
      await api.leaveGroup(user.playerUUID, groupId);
      
      setGroups(groups.filter(g => g.groupId !== groupId));
      if (selectedGroup?.groupId === groupId) {
        setSelectedGroup(null);
      }
    } catch (error) {
      console.error('Failed to leave group:', error);
      alert('Failed to leave group. Please try again.');
    } finally {
      setActionLoading(null);
    }
  };

  if (connectionError) {
    return <ConnectionError onRetry={() => window.location.reload()} />;
  }

  return (
    <div className="min-h-screen bg-neutral-900">
      <GroupsHeader 
        view={view === 'management' ? 'my-groups' : view}
        onCreateGroup={() => setShowCreateModal(true)}
        onJoinGroup={() => setShowJoinModal(true)}
        groupCount={groups.length}
      />
      
      <div className="max-w-7xl mx-auto px-3 sm:px-4 py-4 sm:py-6">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-3 sm:gap-6 h-[calc(100vh-120px)] sm:h-[calc(100vh-140px)] overflow-hidden scrollbar-thin">
          <GroupSidebar
            groups={groups}
            selectedGroup={selectedGroup}
            setSelectedGroup={setSelectedGroup}
            view={view}
            setView={setView}
            searchTerm={searchTerm}
            setSearchTerm={setSearchTerm}
            loading={loading}
            user={user}
            onGroupDeleted={handleGroupDeleted}
            onGroupLeft={handleGroupLeft}
          />

          <div className="lg:col-span-3 min-h-0 flex flex-col">
            {view === 'messages' && (
              <>
                {selectedGroup ? (
                  <GroupChatArea
                    group={selectedGroup}
                    user={user}
                    websocket={websocket}
                    isConnected={websocket?.isConnected() || false}
                  />
                ) : (
                  <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
                    <div className="w-16 h-16 bg-neutral-700 rounded-full flex items-center justify-center mx-auto mb-4">
                      <svg className="w-8 h-8 text-neutral-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                      </svg>
                    </div>
                    <h3 className="text-lg font-bold text-neutral-200 font-minecraftia mb-2">
                      No Group Selected
                    </h3>
                    <p className="text-neutral-400 mb-4">
                      Select a group from the sidebar to start messaging
                    </p>
                    {groups.length === 0 && (
                      <button
                        onClick={() => setShowCreateModal(true)}
                        className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-black rounded-lg font-medium transition-colors"
                      >
                        Create Your First Group
                      </button>
                    )}
                  </div>
                )}
              </>
            )}

            {view === 'my-groups' && (
              <>
                {selectedGroup ? (
                  <GroupDetailsArea
                    selectedGroup={selectedGroup}
                    groupMembers={groupMembers}
                    userRole={userRole}
                    actionLoading={actionLoading}
                    groupStats={groupStats}
                    onSettingsClick={() => setShowSettingsModal(true)}
                    onLeaveGroup={handleLeaveGroup}
                    currentUser={user}
                  />
                ) : (
                  <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
                    <div className="w-16 h-16 bg-neutral-700 rounded-full flex items-center justify-center mx-auto mb-4">
                      <svg className="w-8 h-8 text-neutral-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                      </svg>
                    </div>
                    <h3 className="text-lg font-bold text-neutral-200 font-minecraftia mb-2">
                      No Group Selected
                    </h3>
                    <p className="text-neutral-400 mb-4">
                      {groups.length > 0 
                        ? "Select a group from the sidebar to view details"
                        : "You're not a member of any groups yet"
                      }
                    </p>
                    {groups.length === 0 && (
                      <div className="space-x-3">
                        <button
                          onClick={() => setShowCreateModal(true)}
                          className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-black rounded-lg font-medium transition-colors"
                        >
                          Create Group
                        </button>
                        <button
                          onClick={() => setShowJoinModal(true)}
                          className="px-4 py-2 bg-neutral-600 hover:bg-neutral-500 text-white rounded-lg font-medium transition-colors"
                        >
                          Join Group
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
            
            {view === 'discover' && (
              <DiscoverGroups
                user={user}
                onGroupJoined={handleGroupJoined}
                onAlreadyMember={handleAlreadyMember}
              />
            )}

            {view === 'invites' && (
              <GroupInvites
                user={user}
                onInviteAccepted={handleInviteAccepted}
              />
            )}

            {view === 'management' && (
              <div className="space-y-6 min-h-0 flex-1 overflow-y-auto pr-1 scrollbar-thin">
                <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
                  <h3 className="text-lg font-bold text-neutral-100 font-minecraftia mb-4">
                    Group Management
                  </h3>
                  
                  {groups.filter(group => group.role === 'OWNER' || group.role === 'ADMIN').length === 0 ? (
                    <div className="text-center py-8">
                      <div className="w-16 h-16 bg-neutral-700 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-8 h-8 text-neutral-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                        </svg>
                      </div>
                      <h4 className="text-lg font-bold text-neutral-200 font-minecraftia mb-2">
                        No Groups to Manage
                      </h4>
                      <p className="text-neutral-400 mb-4">
                        You need to be an owner or admin of a group to access management features
                      </p>
                      <div className="space-x-3">
                        <button
                          onClick={() => setShowCreateModal(true)}
                          className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-black rounded-lg font-medium transition-colors"
                        >
                          Create Group
                        </button>
                        <button
                          onClick={() => setView('discover')}
                          className="px-4 py-2 bg-neutral-600 hover:bg-neutral-500 text-white rounded-lg font-medium transition-colors"
                        >
                          Discover Groups
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="mb-6">
                      <label className="block text-sm font-medium text-neutral-300 mb-2">
                        Select Group to Manage
                      </label>
                      <select
                        value={managementGroup?.groupId || ''}
                        onChange={(e) => {
                          const group = groups.find(g => g.groupId === e.target.value && (g.role === 'OWNER' || g.role === 'ADMIN'));
                          setManagementGroup(group || null);
                          if (group) {
                            loadManagementGroupMembers(group.groupId);
                            setManagementRole(group.role || 'MEMBER');
                            api.getGroupDetails(group.groupId)
                              .then(res => setManagementGroupDetails(res.group))
                              .catch(err => console.error('Failed to load management group details:', err));
                          } else {
                            setManagementGroupDetails(null);
                          }
                        }}
                        className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 focus:outline-none focus:border-yellow-500"
                      >
                        <option value="">Choose a group...</option>
                        {groups
                          .filter(group => group.role === 'OWNER' || group.role === 'ADMIN')
                          .map(group => (
                            <option key={group.groupId} value={group.groupId}>
                              {group.groupName} ({group.role})
                            </option>
                          ))}
                      </select>
                    </div>
                  )}
                </div>

                {managementGroup && (
                  <>
                    <GroupAnnouncements
                      group={managementGroupDetails || managementGroup}
                      userRole={managementRole}
                      currentUser={user}
                      onUpdate={(() => {
                        let timeout: ReturnType<typeof setTimeout> | null = null;
                        return () => {
                          if (timeout) clearTimeout(timeout);
                          timeout = setTimeout(() => {
                            loadGroups();
                            if (managementGroup) {
                              api.getGroupDetails(managementGroup.groupId)
                                .then(res => setManagementGroupDetails(res.group))
                                .catch(err => console.error('Failed to refresh management group details:', err));
                            }
                          }, 250);
                        };
                      })()}
                    />
                    <GroupModeration
                      group={managementGroup}
                      members={managementMembers}
                      userRole={managementRole}
                      currentUser={user}
                      onMemberAction={() => {
                        if (managementGroup) {
                          loadManagementGroupMembers(managementGroup.groupId);
                        }
                      }}
                    />
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {showCreateModal && (
        <CreateGroupModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          onSubmit={handleCreateGroup}
          loading={actionLoading === 'create'}
          currentGroupCount={groups.length}
          maxGroupsAllowed={15}
        />
      )}

      {showJoinModal && (
        <JoinGroupModal
          isOpen={showJoinModal}
          onClose={() => setShowJoinModal(false)}
          onJoin={() => {
            setShowJoinModal(false);
            loadGroups();
          }}
        />
      )}

      {showAlreadyMemberModal && alreadyMemberGroup && (
        <AlreadyMemberModal
          isOpen={showAlreadyMemberModal}
          onClose={() => {
            setShowAlreadyMemberModal(false);
            setAlreadyMemberGroup(null);
          }}
          group={alreadyMemberGroup}
          onSwitchToGroup={(group) => {
            setSelectedGroup(group);
            setView('my-groups');
            loadGroupMembers(group.groupId);
          }}
        />
      )}

      {showSettingsModal && selectedGroup && (
        <GroupSettingsModal
          isOpen={showSettingsModal}
          onClose={() => setShowSettingsModal(false)}
          group={selectedGroup}
          onUpdate={() => {
            setShowSettingsModal(false);
            loadGroups(); 
          }}
        />
      )}
    </div>
  );
};

export default GroupsPage;
