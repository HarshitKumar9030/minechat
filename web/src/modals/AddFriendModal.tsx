'use client';
import React, { useState } from 'react';
import { X } from 'lucide-react';

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!friendName.trim()) return;
    
    setError('');
    setSuccess('');
    
    const result = await onSubmit(friendName.trim());
    
    if (result.success) {
      setSuccess('Friend request sent successfully!');
      setFriendName('');
      // Auto close after success with delay
      setTimeout(() => {
        setSuccess('');
        onClose();
      }, 2000);
    } else {
      setError(result.error || 'Failed to send friend request');
    }
  };

  const handleClose = () => {
    setFriendName('');
    setError('');
    setSuccess('');
    onClose();
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      handleClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-6 w-full max-w-md animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-minecraftia text-neutral-200 leading-none">
            Add Friend
          </h2>
          <button
            onClick={handleClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-colors duration-300"
          >
            <X className="h-4 w-4 text-neutral-400" />
          </button>
        </div>
        
        <p className="text-neutral-400 tracking-wide  font-minecraftia text-sm  mt-3 mb-6">
          Enter a player name to send them a friend request
        </p>

        {error && (
          <div className="mb-4 p-3 bg-red-900/30 border border-red-800/50 rounded-lg animate-in slide-in-from-top-2 duration-300">
            <p className="text-red-400 font-minecraftia text-sm leading-none mt-3">
              {error}
            </p>
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 bg-green-900/30 border border-green-800/50 rounded-lg animate-in slide-in-from-top-2 duration-300">
            <p className="text-green-400 font-minecraftia text-sm leading-none mt-3">
              {success}
            </p>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block mt-2 text-neutral-300 font-minecraftia text-sm mb-2 leading-none">
              Player Name
            </label>
            {/* Inter has to used cuz of some weird bug with minecraftia */}
            <input
              type="text"
              placeholder="Enter player name..."
              value={friendName}
              onChange={(e) => setFriendName(e.target.value)}
              className="w-full px-4 py-3 font-inter bg-neutral-900 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-yellow-600 focus:border-transparent  text-sm leading-none mt-3 transition-all duration-300"
              required
              autoFocus
            />
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
