import React, { useState, useEffect } from 'react';
import { Plus, Users } from 'lucide-react';
import { api, GroupInfo } from '@/lib/api';

interface DiscoverGroupsProps {
  user?: { playerUUID: string; playerName: string } | null;
  onGroupJoined?: (group: GroupInfo) => void;
  onAlreadyMember?: (group: GroupInfo) => void;
}

interface PublicGroup extends GroupInfo {
  canJoin: boolean;
  joinMessage?: string;
}

const DiscoverGroups: React.FC<DiscoverGroupsProps> = ({
  user,
  onGroupJoined,
  onAlreadyMember
}) => {
  const [groups, setGroups] = useState<PublicGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [joiningGroupId, setJoiningGroupId] = useState<string | null>(null);

  useEffect(() => {
    loadPublicGroups();
  }, []);

  const loadPublicGroups = async () => {
    try {
      setLoading(true);
      const response = await api.getAllPublicGroups();
      if (response.groups) {
        const groupsWithJoinStatus: PublicGroup[] = response.groups.map((group: GroupInfo) => ({
          ...group,
          canJoin: group.memberCount < group.maxMembers,
          joinMessage: group.memberCount >= group.maxMembers ? 'Group is full' : undefined
        }));
        setGroups(groupsWithJoinStatus);
      }
    } catch (error) {
      console.error('Failed to load public groups:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleJoinGroup = async (group: PublicGroup) => {
    if (!user || joiningGroupId) return;

    try {
      setJoiningGroupId(group.groupId);
      const response = await api.joinGroup(user.playerUUID, user.playerName, group.groupId);
      
      if (response && typeof response === 'object' && 'success' in response && response.success) {
        setGroups(prev => prev.map(g => 
          g.groupId === group.groupId 
            ? { ...g, memberCount: g.memberCount + 1, canJoin: g.memberCount + 1 < g.maxMembers }
            : g
        ));
        
        if (onGroupJoined) {
          onGroupJoined(group);
        }
      }
    } catch (error: unknown) {
      console.error('Failed to join group:', error);
      
      // Check if it's an already member error
      if (error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 
          'error' in error.response && error.response.error === 'ALREADY_MEMBER') {
        if (onAlreadyMember) {
          onAlreadyMember(group);
        }
        return;
      }
      
      const errorMessage = error instanceof Error ? error.message : String(error);
      if (errorMessage.includes('Internal server error')) {
        if (onAlreadyMember) {
          onAlreadyMember(group);
        }
      }
    } finally {
      setJoiningGroupId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold pt-2 text-neutral-100 font-minecraftia">
            Discover Groups
          </h2>
          <p className="text-neutral-400 mt-1">
            Find and join groups that interest you
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 animate-pulse">
              <div className="h-6 bg-neutral-700 rounded mb-3"></div>
              <div className="h-4 bg-neutral-700 rounded mb-2"></div>
              <div className="h-4 bg-neutral-700 rounded w-3/4 mb-4"></div>
              <div className="flex justify-between items-center">
                <div className="h-4 bg-neutral-700 rounded w-16"></div>
                <div className="h-8 bg-neutral-700 rounded w-20"></div>
              </div>
            </div>
          ))
        ) : groups.length === 0 ? (
          <div className="col-span-full text-center py-12">
            <Users className="w-16 h-16 text-neutral-600 mx-auto mb-4" />
            <h3 className="text-xl font-minecraftia text-neutral-300 mb-2">
              No public groups available
            </h3>
            <p className="text-neutral-500">
              Check back later for new public groups
            </p>
          </div>
        ) : (
          groups.map((group) => (
            <div
              key={group.groupId}
              className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 hover:border-neutral-600 transition-all duration-300 group"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1 min-w-0">
                  <h3 className="font-minecraftia text-lg text-neutral-100 truncate">
                    {group.groupName}
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-sm text-neutral-400">
                      {group.memberCount}/{group.maxMembers} members
                    </span>
                    {!group.canJoin && (
                      <span className="text-xs bg-red-900 text-red-300 px-2 py-1 rounded-full">
                        Full
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <p className="text-neutral-300 text-sm mb-4 line-clamp-3">
                {group.description || 'No description available'}
              </p>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 bg-neutral-700 rounded flex items-center justify-center">
                    <Users className="w-3 h-3 text-neutral-400" />
                  </div>
                  <span className="text-xs text-neutral-500">
                    {group.memberCount} members
                  </span>
                </div>

                <button
                  onClick={() => handleJoinGroup(group)}
                  disabled={!group.canJoin || joiningGroupId === group.groupId || !user}
                  className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-neutral-600 disabled:cursor-not-allowed text-neutral-900 disabled:text-neutral-400 rounded-lg transition-colors text-sm font-medium flex items-center gap-2"
                >
                  {joiningGroupId === group.groupId ? (
                    <>
                      <div className="w-4 h-4 border-2 border-neutral-400 border-t-transparent rounded-full animate-spin"></div>
                      Joining...
                    </>
                  ) : (
                    <>
                      <Plus className="w-4 h-4" />
                      {group.canJoin ? 'Join' : 'Full'}
                    </>
                  )}
                </button>
              </div>

              {group.joinMessage && (
                <div className="mt-3 text-xs text-neutral-500 bg-neutral-700 rounded p-2">
                  {group.joinMessage}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {!loading && (
        <div className="text-center">
          <button
            onClick={loadPublicGroups}
            className="px-6 py-2 bg-neutral-700 hover:bg-neutral-600 text-neutral-300 rounded-lg transition-colors text-sm"
          >
            Refresh Groups
          </button>
        </div>
      )}
    </div>
  );
};

export default DiscoverGroups;
