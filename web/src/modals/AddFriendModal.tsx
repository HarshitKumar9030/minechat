'use client';
import React, { useState, useEffect, useCallback } from 'react';
import { X, Search, User } from 'lucide-react';
import Image from 'next/image';
import { api, Player, getPlayerHead } from '@/lib/api';

interface AddFriendModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (name: string) => Promise<{ success: boolean; error?: string }>;
  loading: boolean;

}

const AddFriendModal: React.FC<AddFriendModalProps> = ({ isOpen, onClose, onSubmit, loading }) => {
  const [friendName, setFriendName] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchResults, setSearchResults] = useState<Player[]>([]);
  const [searching, setSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);

  const searchPlayers = useCallback(async (query: string) => {
    if (query.length < 2) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    setSearching(true);
    try {
      const response = await api.searchPlayers(query, 5);
      setSearchResults(response.players);
      setShowResults(true);
    } catch (error) {
      console.error('Failed to search players:', error);
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (friendName.trim()) {
        searchPlayers(friendName.trim());
      } else {
        setSearchResults([]);
        setShowResults(false);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [friendName, searchPlayers]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!friendName.trim()) return;
    
    setError('');
    setSuccess('');
    setShowResults(false);
    
    const result = await onSubmit(friendName.trim());
    
    if (result.success) {
      setSuccess('Friend request sent successfully!');
      setFriendName('');
      setSearchResults([]);
      // Auto close after success with delay
      setTimeout(() => {
        setSuccess('');
        onClose();
      }, 2000);
    } else {
      setError(result.error || 'Failed to send friend request');
    }
  };

  const handleSelectPlayer = (playerName: string) => {
    setFriendName(playerName);
    setShowResults(false);
  };

  const handleClose = () => {
    setFriendName('');
    setError('');
    setSuccess('');
    setSearchResults([]);
    setShowResults(false);
    onClose();
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      handleClose();
    }
  };

  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.src = 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-6 w-full max-w-md animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-minecraftia text-neutral-200">
            Add Friend
          </h2>
          <button
            onClick={handleClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-colors duration-300"
          >
            <X className="h-4 w-4 text-neutral-400" />
          </button>
        </div>
        
        <p className="text-neutral-400 font-minecraftia text-sm mb-6">
          Enter a player name to send them a friend request
        </p>

        {error && (
          <div className="mb-4 p-3 bg-red-900/30 border border-red-800/50 rounded-lg animate-in slide-in-from-top-2 duration-300">
            <p className="text-red-400 font-minecraftia text-sm">
              {error}
            </p>
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 bg-green-900/30 border border-green-800/50 rounded-lg animate-in slide-in-from-top-2 duration-300">
            <p className="text-green-400 font-minecraftia text-sm">
              {success}
            </p>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="relative">
            <label className="block text-neutral-300 font-minecraftia text-sm mb-2">
              Player Name
            </label>
            <div className="relative">
              <input
                type="text"
                placeholder="Start typing to search players..."
                value={friendName}
                onChange={(e) => setFriendName(e.target.value)}
                className="w-full px-4 py-3 pl-10 bg-neutral-900 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-yellow-600 focus:border-transparent text-sm transition-all duration-300"
                required
                autoFocus
              />
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-500" />
              {searching && (
                <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                  <div className="h-4 w-4 border-2 border-neutral-600 border-t-yellow-600 rounded-full animate-spin" />
                </div>
              )}
            </div>
            
            {showResults && searchResults.length > 0 && (
              <div className="absolute z-10 w-full mt-1 bg-neutral-800 border border-neutral-600 rounded-lg shadow-lg max-h-48 overflow-y-auto">
                {searchResults.map((player, index) => (
                  <button
                    key={player.playerUUID || index}
                    type="button"
                    onClick={() => handleSelectPlayer(player.playerName)}
                    className="w-full flex items-center gap-3 p-3 hover:bg-neutral-700 transition-colors duration-200 text-left first:rounded-t-lg last:rounded-b-lg"
                  >
                    <div className="relative flex-shrink-0">
                      <Image
                        src={getPlayerHead(player.playerName, player.playerUUID, 32)}
                        alt={player.playerName}
                        width={32}
                        height={32}
                        className="rounded-md"
                        onError={handleImageError}
                      />
                      <div className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-neutral-800 ${
                        player.online ? 'bg-green-500' : 'bg-neutral-600'
                      }`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-minecraftia text-neutral-200 text-xs truncate">
                          {player.playerName}
                        </span>
                        {player.rank && (
                          <span className="font-minecraftia text-yellow-500 text-xs truncate">
                            {player.rank}
                          </span>
                        )}
                      </div>
                      <p className="text-neutral-500 font-minecraftia text-xs mt-1">
                        {player.online ? 'Online' : 'Offline'}
                        {player.lastSeen && !player.online && (
                          <span> • Last seen {new Date(player.lastSeen).toLocaleDateString()}</span>
                        )}
                      </p>
                    </div>
                    <User className="h-4 w-4 text-neutral-500 flex-shrink-0" />
                  </button>
                ))}
              </div>
            )}

            {showResults && searchResults.length === 0 && !searching && friendName.length >= 2 && (
              <div className="absolute z-10 w-full mt-1 bg-neutral-800 border border-neutral-600 rounded-lg shadow-lg p-4 text-center">
                <p className="text-neutral-500 font-minecraftia text-xs">
                  No players found matching &ldquo;{friendName}&rdquo;
                </p>
              </div>
            )}
          </div>
          
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 mt-3 py-3 px-4 bg-neutral-700 hover:bg-neutral-600 text-neutral-300 font-inter text-sm rounded-lg transition-all duration-300 leading-none  hover:scale-105"
            >
             Cancel
            </button>
            <button
              type="submit"
              disabled={loading || !friendName.trim() || !!success}
              className={`flex-1 py-3 px-4 ${
                !!success 
                  ? 'bg-green-600 hover:bg-green-700 text-white' 
                  : 'bg-yellow-600 hover:bg-yellow-700 text-neutral-900'
              } disabled:bg-neutral-700 disabled:text-neutral-500 font-inter text-sm rounded-lg transition-all duration-300 leading-none disabled:cursor-not-allowed mt-3 hover:scale-105 disabled:hover:scale-100`}
            >
              {loading ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="h-4 w-4 border-2 border-neutral-900 border-t-transparent rounded-full animate-spin" />
                  Sending...
                </div>
              ) : !!success ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="h-4 w-4 text-white">✓</div>
                  Sent!
                </div>
              ) : (
                'Send Request'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddFriendModal;
