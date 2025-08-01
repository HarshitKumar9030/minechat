'use client';
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Users, UserPlus, Search, ArrowLeft, Plus, Mail } from 'lucide-react';
import { SERVER_NAME } from '@/lib/constants';
import { MinechatAPI, FriendInfo, FriendRequest, getPlayerHead } from '@/lib/api';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import AddFriendModal from '@/modals/AddFriendModal';
import FriendRequestsModal from '@/modals/FriendRequestsModal';
import { Trash2 } from 'lucide-react';

const FriendsPage = () => {
  const [friends, setFriends] = useState<FriendInfo[]>([]);
  const [friendRequests, setFriendRequests] = useState<FriendRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<FriendRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [user, setUser] = useState<{ playerUUID: string; playerName: string } | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRequestsModal, setShowRequestsModal] = useState(false);
  const router = useRouter();
  const api = useMemo(() => new MinechatAPI(), []);

  useEffect(() => {
    const savedUser = localStorage.getItem('minechat_user');
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser({ playerUUID: userData.playerUUID, playerName: userData.playerName });
      } catch (error) {
        console.error('Error parsing saved user data:', error);
        router.push('/');
      }
    } else {
      router.push('/');
    }
  }, [router]);

  const loadFriendsData = useCallback(async () => {
    if (!user) return;
    
    setLoading(true);
    try {
      const [friendsResponse, requestsResponse] = await Promise.all([
        api.getFriends(user.playerUUID),
        api.getFriendRequests(user.playerUUID)
      ]);
      
      setFriends(friendsResponse.friends || []);
      
      // Separate incoming and sent requests
      const allRequests = requestsResponse.requests || [];
      const incomingRequests = allRequests.filter(
        request => request.senderUUID !== user.playerUUID
      );
      const sentRequests = allRequests.filter(
        request => request.senderUUID === user.playerUUID
      );
      
      setFriendRequests(incomingRequests);
      setSentRequests(sentRequests);
    } catch (error) {
      console.error('Failed to load friends data:', error);
    } finally {
      setLoading(false);
    }
  }, [user, api]);

  useEffect(() => {
    if (user) {
      loadFriendsData();
    }
  }, [user, loadFriendsData]);

  const handleSendFriendRequest = async (friendName: string): Promise<{success: boolean; error?: string}> => {
    if (!user) return { success: false, error: 'User not authenticated' };

    setActionLoading('send');
    try {
      await api.sendFriendRequest(user.playerUUID, user.playerName, friendName);
      await loadFriendsData(); // Reload to get updated requests
      return { success: true };
    } catch (error) {
      console.error('Failed to send friend request:', error);
      // Extract error message from API response
      let errorMessage = 'Failed to send friend request';
      
      if (error instanceof Error) {
        errorMessage = error.message;
      } else if (typeof error === 'object' && error !== null) {
        const apiError = error as { response?: { data?: { error?: string } } };
        errorMessage = apiError.response?.data?.error || errorMessage;
      }
      
      return { success: false, error: errorMessage };
    } finally {
      setActionLoading(null);
    }
  };

  const handleAcceptRequest = async (requesterUUID: string) => {
    if (!user) return;
    
    setActionLoading(requesterUUID);
    try {
      await api.acceptFriendRequest(user.playerUUID, requesterUUID);
      await loadFriendsData(); // Reload data
    } catch (error) {
      console.error('Failed to accept friend request:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRejectRequest = async (requesterUUID: string) => {
    if (!user) return;
    
    setActionLoading(requesterUUID);
    try {
      await api.rejectFriendRequest(user.playerUUID, requesterUUID);
      await loadFriendsData(); // Reload data
    } catch (error) {
      console.error('Failed to reject friend request:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveFriend = async (friendUUID: string) => {
    if (!user) return;
    
    setActionLoading(friendUUID);
    try {
      await api.removeFriend(user.playerUUID, friendUUID);
      await loadFriendsData(); // Reload data
    } catch (error) {
      console.error('Failed to remove friend:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const filteredFriends = friends.filter(friend =>
    friend.friendName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getPlayerHeadUrl = (playerName: string, playerUUID?: string) => {
    return getPlayerHead(playerName, playerUUID);
  };

  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.src = 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
  };

  if (loading) {
    return (
      <main className='min-h-screen w-full flex flex-col gap-8 justify-center items-center p-4'>
        <div className="text-center animate-in fade-in duration-500">
          <div className="animate-spin h-8 w-8 border-2 border-yellow-600 border-t-transparent rounded-full mx-auto mb-4"></div>
          <p className="text-neutral-400 font-minecraftia text-sm leading-none mt-3">
            Loading friends...
          </p>
        </div>
      </main>
    );
  }

  return (
    <>
      <main className='min-h-screen w-full flex flex-col gap-4 md:gap-6 p-4 max-w-7xl mx-auto animate-in fade-in duration-500'>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-2 md:pt-4 animate-in slide-in-from-top-3 duration-300">
          <div className="flex items-center gap-3 md:gap-4">
            <button
              onClick={() => router.push('/dashboard')}
              className="p-2 rounded-lg bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-all duration-300 hover:scale-110 flex-shrink-0"
              aria-label="Back to Dashboard"
            >
              <ArrowLeft className="h-4 w-4 md:h-5 md:w-5 text-neutral-400" />
            </button>
            <div className="min-w-0">
              <h1 className="text-xl md:text-2xl lg:text-3xl font-minecraftia text-neutral-300 leading-none">
                Friends on <span className='text-yellow-600'>{SERVER_NAME}</span>
              </h1>
              <p className="text-neutral-500 font-minecraftia text-xs md:text-sm tracking-wide leading-none mt-3">
                Manage your friend list and connections
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 md:gap-3 flex-shrink-0">
            <button
              onClick={() => setShowRequestsModal(true)}
              className="relative p-[10px] md:p-3 rounded-lg bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-all duration-300 "
              aria-label="Friend Requests"
            >
              <Mail className="h-4 w-4 md:h-5 md:w-5 text-neutral-400" />
              {(friendRequests.length + sentRequests.length) > 0 && (
                <div className="absolute -top-1 -right-1 w-4 h-4 md:w-5 md:h-5 bg-yellow-600 text-neutral-900 text-xs font-bold rounded-full flex items-center justify-center font-inter leading-none">
                  {(friendRequests.length + sentRequests.length) > 9 ? '9+' : (friendRequests.length + sentRequests.length)}
                </div>
              )}
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-2 px-3 py-2 md:px-4 md:py-3 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 font-minecraftia text-xs md:text-sm rounded-lg transition-all duration-300 leading-none"
            >
              <Plus className="h-3 w-3 md:h-4 md:w-4" />
              <span className="hidden mt-2 sm:inline">Add Friend</span>
              <span className="sm:hidden mt-2">Add</span>
            </button>
          </div>
        </div>

        <div className="relative w-full max-w-md flex items-center animate-in slide-in-from-left-3 duration-300 delay-100">
          <Search className="absolute left-3 top-3/5  transform -translate-y-1/2 h-4 w-4 text-neutral-500" />
          <input
            type="text"
            placeholder="Search friends..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 font-inter md:py-3 bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-200 placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-yellow-600 focus:border-transparent  text-sm leading-none mt-3 transition-all duration-300"
          />
        </div>

        <div className="grid grid-cols-3 gap-3 md:gap-4 animate-in slide-in-from-bottom-3 duration-300 delay-200">
          <div className="p-3 md:p-4 bg-neutral-800 border border-neutral-700 rounded-lg hover:border-neutral-600 transition-all duration-300">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 md:gap-3">
              <Users className="h-6 w-6 md:h-8 md:w-8 text-yellow-600 flex-shrink-0" />
              <div className="min-w-0">
                <h3 className="text-lg md:text-2xl font-minecraftia text-neutral-200 mt-2 leading-none">
                  {friends.length}
                </h3>
                <p className="text-neutral-400 font-minecraftia text-xs md:text-sm leading-none mt-2 md:mt-3">
                  Total Friends
                </p>
              </div>
            </div>
          </div>

          <div className="p-3 md:p-4 bg-neutral-800 border border-neutral-700 rounded-lg hover:border-neutral-600 transition-all duration-300">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 md:gap-3">
              <div className="w-6 h-6 md:w-8 md:h-8 bg-green-600 rounded-full flex items-center justify-center flex-shrink-0">
                <div className="w-2 h-2 md:w-3 md:h-3 bg-white rounded-full animate-pulse" />
              </div>
              <div className="min-w-0">
                <h3 className="text-lg md:text-2xl font-minecraftia text-neutral-200 mt-2 leading-none">
                  {friends.filter(f => f.online).length}
                </h3>
                <p className="text-neutral-400 font-minecraftia text-xs md:text-sm leading-none mt-2 md:mt-3">
                  Online Now
                </p>
              </div>
            </div>
          </div>

          <div className="p-3 md:p-4 bg-neutral-800 border border-neutral-700 rounded-lg hover:border-neutral-600 transition-all duration-300">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 md:gap-3">
              <UserPlus className="h-6 w-6 md:h-8 md:w-8 text-blue-500 flex-shrink-0" />
              <div className="min-w-0">
                <h3 className="text-lg md:text-2xl font-minecraftia text-neutral-200 mt-2 leading-none">
                  {friendRequests.length + sentRequests.length}
                </h3>
                <p className="text-neutral-400 font-minecraftia text-xs md:text-sm leading-none mt-2 md:mt-3">
                  Friend Requests
                </p>
              </div>
            </div>
          </div>
        </div>

        {filteredFriends.length === 0 ? (
          <div className="text-center py-12 md:py-16 animate-in fade-in duration-500 delay-300">
            <Image 
              src="/friends.png" 
              alt="No friends" 
              width={96} 
              height={96} 
              className="mx-auto mb-4 md:mb-6 opacity-50 animate-in zoom-in-75 duration-700 delay-400 md:w-32 md:h-32"
            />
            <h3 className="text-lg md:text-xl font-minecraftia text-neutral-300 leading-none mb-2">
              {searchTerm ? 'No friends found' : 'No friends yet'}
            </h3>
            <p className="text-neutral-400 font-minecraftia text-sm tracking-wide mt-3 max-w-sm md:max-w-md mx-auto px-4">
              {searchTerm 
                ? 'Try adjusting your search terms' 
                : 'Start building your friend network by sending friend requests to other players!'
              }
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 md:gap-4 animate-in slide-in-from-bottom-3 duration-300 delay-300">
            {filteredFriends.map((friend, index) => (
              <div
                key={friend.friendUUID}
                className="group relative p-4 md:p-6 bg-neutral-800 border border-neutral-700 rounded-xl hover:border-neutral-600 transition-all duration-300 animate-in zoom-in-95 "
                style={{ animationDelay: `${300 + index * 50}ms`, animationDuration: '400ms' }}
              >
                <div className="text-center">
                  <div className="relative inline-block mb-3 md:mb-4">
                    <Image
                      src={getPlayerHeadUrl(friend.friendName, friend.friendUUID)}
                      alt={friend.friendName}
                      width={48}
                      height={48}
                      className="rounded-xl transition-transform duration-300 hover:scale-110 md:w-16 md:h-16"
                      onError={handleImageError}
                    />
                    <div className={`absolute -bottom-1 -right-1 md:-bottom-2 md:-right-2 w-4 h-4 md:w-6 md:h-6 rounded-full border-2 border-neutral-800 transition-all duration-300 ${
                      friend.online ? 'bg-green-500 animate-pulse' : 'bg-neutral-600'
                    }`} />
                  </div>

                  <h3 className="text-neutral-200 pt-2  font-minecraftia text-sm md:text-lg leading-none mb-1 truncate">
                    {friend.friendName}
                  </h3>
                  <p className="text-neutral-500 font-minecraftia text-xs leading-none mt-3">
                    {friend.online ? 'Online' : 'Offline'}
                  </p>
                  <p className="text-neutral-600 font-minecraftia text-xs leading-none mt-2 hidden md:block">
                    Since {new Date(friend.timestamp).toLocaleDateString()}
                  </p>
                </div>

                <button
                  onClick={() => handleRemoveFriend(friend.friendUUID)}
                  disabled={actionLoading === friend.friendUUID}
                  className="absolute top-2 right-2 md:top-3 md:right-3 p-1 md:p-2 rounded-lg bg-red-900/30 border border-red-800/50 hover:bg-red-900/50 transition-all duration-300 opacity-0 group-hover:opacity-100 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100"
                  aria-label="Remove Friend"
                >
                  {actionLoading === friend.friendUUID ? (
                    <div className="h-3 w-3 md:h-4 md:w-4 border border-red-400 border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <Trash2 className="h-3 w-3 md:h-4 md:w-4 text-red-400" />
                  )}
                </button>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Modals */}
      <AddFriendModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSubmit={handleSendFriendRequest}
        loading={actionLoading === 'send'}
      />

      <FriendRequestsModal
        isOpen={showRequestsModal}
        onClose={() => setShowRequestsModal(false)}
        incomingRequests={friendRequests}
        sentRequests={sentRequests}
        onAccept={handleAcceptRequest}
        onReject={handleRejectRequest}
        actionLoading={actionLoading}
      />
    </>
  );
};

export default FriendsPage;
