import React, { useState, useEffect } from 'react';
import { Users, Compass, Mail, MessageCircle, Search, Settings } from 'lucide-react';
import { GroupInfo, api } from '@/lib/api';

export type ViewType = 'my-groups' | 'discover' | 'invites' | 'messages' | 'management';

interface GroupSidebarProps {
  view: ViewType;
  setView: (view: ViewType) => void;
  groups: GroupInfo[];
  selectedGroup: GroupInfo | null;
  setSelectedGroup: (group: GroupInfo | null) => void;
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  loading: boolean;
  user?: { playerUUID: string; playerName: string } | null;
  onGroupDeleted?: (groupId: string) => void;
  onGroupLeft?: (groupId: string) => void;
}

const GroupSidebar: React.FC<GroupSidebarProps> = ({
  view,
  setView,
  groups,
  selectedGroup,
  setSelectedGroup,
  searchTerm,
  setSearchTerm,
  loading,
  user
}) => {
  const [inviteCount, setInviteCount] = useState(0);

  useEffect(() => {
    const loadInviteCount = async () => {
      if (user && view === 'invites') {
        try {
          const response = await api.getGroupInvites(user.playerUUID);
          setInviteCount(response.invites?.length || 0);
        } catch (error) {
          console.error('Failed to load invite count:', error);
        }
      }
    };

    loadInviteCount();
  }, [user, view]);

  const handleViewChange = (newView: ViewType) => {
    setView(newView);
    if (newView !== 'my-groups' && newView !== 'messages') {
      setSelectedGroup(null);
      return;
    }
    const list = filteredGroups.length > 0 ? filteredGroups : groups;
    if (!selectedGroup && list.length > 0) {
      setSelectedGroup(list[0]);
    }
  };

  const filteredGroups = groups.filter(group =>
    group.groupName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    group.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
  <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 h-full overflow-hidden flex flex-col">
      <div className="mb-4">
        <nav className="space-y-1">
          <button
            onClick={() => handleViewChange('my-groups')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-all duration-300 ${
              view === 'my-groups'
                ? 'bg-yellow-600 text-neutral-900'
                : 'text-neutral-300 hover:bg-neutral-700'
            }`}
          >
            <Users className="w-4 h-4" />
            <span className="font-minecraftia mt-3 text-sm">My Groups</span>
            <span className="ml-auto text-xs bg-transparent px-2 py-1 rounded-full">
              {groups.length}
            </span>
          </button>

          <button
            onClick={() => handleViewChange('messages')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-all duration-300 ${
              view === 'messages'
                ? 'bg-yellow-600 text-neutral-900'
                : 'text-neutral-300 hover:bg-neutral-700'
            }`}
          >
            <MessageCircle className="w-4 h-4" />
            <span className="font-minecraftia mt-3 text-sm">Messages</span>
          </button>

          <button
            onClick={() => handleViewChange('discover')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-all duration-300 ${
              view === 'discover'
                ? 'bg-yellow-600 text-neutral-900'
                : 'text-neutral-300 hover:bg-neutral-700'
            }`}
          >
            <Compass className="w-4 h-4" />
            <span className="font-minecraftia mt-3 text-sm">Discover</span>
          </button>

          <button
            onClick={() => handleViewChange('invites')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-all duration-300 ${
              view === 'invites'
                ? 'bg-yellow-600 text-neutral-900'
                : 'text-neutral-300 hover:bg-neutral-700'
            }`}
          >
            <Mail className="w-4 h-4" />
            <span className="font-minecraftia mt-3 text-sm">Invites</span>
            {inviteCount > 0 && (
              <span className="ml-auto text-xs bg-red-600 text-white px-2 py-1 rounded-full">
                {inviteCount}
              </span>
            )}
          </button>

          <button
            onClick={() => handleViewChange('management')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-all duration-300 ${
              view === 'management'
                ? 'bg-yellow-600 text-neutral-900'
                : 'text-neutral-300 hover:bg-neutral-700'
            }`}
          >
            <Settings className="w-4 h-4" />
            <span className="font-minecraftia mt-3 text-sm">Management</span>
          </button>
        </nav>
      </div>

      {(view === 'my-groups' || view === 'messages') && (
        <div className="min-h-0 flex-1 flex flex-col">
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search groups..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 text-sm focus:outline-none focus:border-yellow-600"
            />
          </div>

          <div className="space-y-2 overflow-y-auto min-h-0 flex-1 scrollbar-thin">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="w-6 h-6 border-2 border-neutral-400 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : filteredGroups.length === 0 ? (
              <div className="text-center py-8 text-neutral-400">
                <Users className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">
                  {searchTerm ? 'No groups found' : 'No groups yet'}
                </p>
              </div>
            ) : (
              filteredGroups.map((group) => (
                <button
                  key={group.groupId}
                  onClick={() => setSelectedGroup(group)}
                  className={`w-full p-3 rounded-lg text-left transition-all duration-300 ${
                    selectedGroup?.groupId === group.groupId
                      ? 'bg-yellow-600 text-neutral-900'
                      : 'bg-neutral-700 hover:bg-neutral-600 text-neutral-200'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <h3 className="font-minecraftia pt-1 text-sm truncate">
                      {group.groupName}
                    </h3>
                    <span className="text-xs opacity-75">
                      {group.memberCount}/{group.maxMembers}
                    </span>
                  </div>
                  <p className="text-xs opacity-75 truncate">
                    {group.description}
                  </p>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default GroupSidebar;
