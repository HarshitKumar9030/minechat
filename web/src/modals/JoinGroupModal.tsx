'use client';
import React, { useState } from 'react';
import { X, Search, UserPlus } from 'lucide-react';
import Image from 'next/image';
import { api } from '@/lib/api';

interface JoinGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onJoin: () => void;
}

const JoinGroupModal: React.FC<JoinGroupModalProps> = ({
  isOpen,
  onClose,
  onJoin
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [joinMethod, setJoinMethod] = useState<'search' | 'code'>('search');
  const [groupCode, setGroupCode] = useState('');

  const currentUser = {
    playerUUID: localStorage.getItem('playerUUID') || '',
    playerName: localStorage.getItem('playerName') || '',
  };

  const handleSearch = React.useCallback(async () => {
    if (!searchQuery.trim()) return;
    
    try {
      setLoading(true);
      const response = await api.searchGroups(searchQuery, 20);
      setSearchResults(response.groups || []);
    } catch (error) {
      console.error('Failed to search groups:', error);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  }, [searchQuery]);

  const handleJoinGroup = async (groupId: string) => {
    try {
      setActionLoading(groupId);
      await api.joinGroup(currentUser.playerUUID, currentUser.playerName, groupId);
      onJoin();
    } catch (error) {
      console.error('Failed to join group:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleJoinByCode = async () => {
    if (!groupCode.trim()) return;
    
    try {
      setActionLoading('code');
      onJoin();
    } catch (error) {
      console.error('Failed to join group by code:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  React.useEffect(() => {
    if (searchQuery.length >= 2) {
      const debounceTimer = setTimeout(handleSearch, 300);
      return () => clearTimeout(debounceTimer);
    } else {
      setSearchResults([]);
    }
  }, [searchQuery, handleSearch]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-6 w-full max-w-lg max-h-[80vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-minecraftia text-neutral-200 leading-none mt-2">
            Join Group
          </h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110"
          >
            <X className="h-5 w-5 text-neutral-400" />
          </button>
        </div>

        <div className="flex gap-1 bg-neutral-900 p-1 rounded-lg mb-4">
          <button
            onClick={() => setJoinMethod('search')}
            className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-md font-inter text-sm transition-all duration-200 ${
              joinMethod === 'search'
                ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                : 'text-neutral-400 hover:text-neutral-300'
            }`}
          >
            <Search className="h-4 w-4" />
            Search Groups
          </button>
          <button
            onClick={() => setJoinMethod('code')}
            className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-md font-inter text-sm transition-all duration-200 ${
              joinMethod === 'code'
                ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                : 'text-neutral-400 hover:text-neutral-300'
            }`}
          >
            <UserPlus className="h-4 w-4" />
            Join by Code
          </button>
        </div>

        <div className="flex-1 overflow-hidden">
          {joinMethod === 'search' ? (
            <>
              <div className="relative mb-4">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-400" />
                <input
                  type="text"
                  placeholder="Search for groups..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:border-yellow-600 focus:outline-none"
                />
              </div>

              <div className="flex-1 overflow-y-auto">
                {loading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-yellow-600 mx-auto mb-3"></div>
                    <p className="text-neutral-400 font-minecraftia text-sm">Searching...</p>
                  </div>
                ) : searchResults.length === 0 ? (
                  <div className="text-center py-8">
                    <Image 
                      src="/dashboard/misc.png" 
                      alt="No results" 
                      width={48} 
                      height={48} 
                      className="mx-auto mb-3 opacity-50"
                    />
                    <p className="text-neutral-400 font-minecraftia text-sm">
                      {searchQuery ? 'No groups found' : 'Start typing to search'}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {searchResults.map((group) => (
                      <div
                        key={group.groupId}
                        className="flex items-center gap-4 p-4 bg-neutral-700 border border-neutral-600 rounded-lg hover:border-neutral-500 transition-all duration-300"
                      >
                        <div className="flex-1 min-w-0">
                          <h3 className="font-minecraftia text-neutral-200 text-sm mb-1 leading-none">
                            {group.groupName}
                          </h3>
                          <p className="text-neutral-400 font-inter text-xs mb-2 line-clamp-2">
                            {group.description}
                          </p>
                          <div className="flex items-center gap-3 text-xs">
                            <span className="text-neutral-500 font-inter">
                              {group.memberCount}/{group.maxMembers} members
                            </span>
                            {group.isPrivate && (
                              <span className="px-2 py-1 bg-yellow-900/30 border border-yellow-800/50 rounded text-yellow-400 font-minecraftia text-xs">
                                Private
                              </span>
                            )}
                          </div>
                        </div>

                        <button
                          onClick={() => handleJoinGroup(group.groupId)}
                          disabled={actionLoading === group.groupId}
                          className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-neutral-900 transition-all duration-300 disabled:opacity-50 font-inter text-sm flex items-center gap-2"
                        >
                          {actionLoading === group.groupId ? (
                            <>
                              <div className="h-4 w-4 border border-neutral-900 border-t-transparent rounded-full animate-spin" />
                              Joining...
                            </>
                          ) : (
                            <>
                              <UserPlus className="h-4 w-4" />
                              Join
                            </>
                          )}
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="space-y-4">
              <div>
                <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none">
                  Group Invite Code
                </label>
                <input
                  type="text"
                  placeholder="Enter invite code..."
                  value={groupCode}
                  onChange={(e) => setGroupCode(e.target.value)}
                  className="w-full px-4 py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:border-yellow-600 focus:outline-none"
                />
                <p className="text-neutral-500 font-inter text-xs mt-2">
                  Ask a group admin for an invite code to join their private group
                </p>
              </div>

              <button
                onClick={handleJoinByCode}
                disabled={!groupCode.trim() || actionLoading === 'code'}
                className="w-full px-4 py-3 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-neutral-900 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed font-inter flex items-center justify-center gap-2"
              >
                {actionLoading === 'code' ? (
                  <>
                    <div className="h-4 w-4 border border-neutral-900 border-t-transparent rounded-full animate-spin" />
                    Joining...
                  </>
                ) : (
                  <>
                    <UserPlus className="h-4 w-4" />
                    Join Group
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default JoinGroupModal;
