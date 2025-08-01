'use client';
import React, { useState } from 'react';
import { Check, X, Send, Users } from 'lucide-react';
import Image from 'next/image';
import { FriendRequest, getPlayerHead } from '@/lib/api';

interface FriendRequestsModalProps {
  isOpen: boolean;
  onClose: () => void;
  incomingRequests: FriendRequest[];
  sentRequests: FriendRequest[];
  onAccept: (uuid: string) => Promise<void>;
  onReject: (uuid: string) => Promise<void>;
  onCancel: (uuid: string) => Promise<void>;
  actionLoading: string | null;
}

const FriendRequestsModal: React.FC<FriendRequestsModalProps> = ({ 
  isOpen, 
  onClose, 
  incomingRequests, 
  sentRequests, 
  onAccept, 
  onReject, 
  onCancel,
  actionLoading
}) => {
  const [activeTab, setActiveTab] = useState<'incoming' | 'sent'>('incoming');

  const getPlayerHeadUrl = (playerName: string, playerUUID?: string) => {
    return getPlayerHead(playerName, playerUUID);
  };

  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    e.currentTarget.src = 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=64&overlay';
  };

  const currentRequests = activeTab === 'incoming' ? incomingRequests : sentRequests;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-2 sm:p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-3 sm:p-6 w-full max-w-2xl max-h-[90vh] sm:max-h-[80vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        <div className="flex items-start sm:items-center justify-between mb-4 sm:mb-6 flex-col sm:flex-row gap-4 sm:gap-0">
          <div className="flex-1 w-full">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg sm:text-xl mt-2 font-minecraftia text-neutral-200 leading-none">
                Friend Requests
              </h2>
              <button
                onClick={onClose}
                className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110 sm:hidden"
              >
                <X className="h-5 w-5 text-neutral-400" />
              </button>
            </div>
            
            <div className="flex gap-1 bg-neutral-900 p-1 rounded-lg">
              <button
                onClick={() => setActiveTab('incoming')}
                className={`flex-1 flex items-center justify-center gap-1 sm:gap-2 px-2 sm:px-3 py-2 rounded-md font-inter text-xs sm:text-sm transition-all duration-200 ${
                  activeTab === 'incoming'
                    ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                    : 'text-neutral-400 hover:text-neutral-300'
                }`}
              >
                <Users className="h-3 w-3 sm:h-4 sm:w-4" />
                <span className="hidden sm:inline">Incoming</span>
                <span className="sm:hidden">In</span> ({incomingRequests.length})
              </button>
              <button
                onClick={() => setActiveTab('sent')}
                className={`flex-1 flex items-center justify-center gap-1 sm:gap-2 px-2 sm:px-3 py-2 rounded-md font-inter text-xs sm:text-sm transition-all duration-200 ${
                  activeTab === 'sent'
                    ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                    : 'text-neutral-400 hover:text-neutral-300'
                }`}
              >
                <Send className="h-3 w-3 sm:h-4 sm:w-4" />
                <span className="hidden sm:inline">Sent</span>
                <span className="sm:hidden">Out</span> ({sentRequests.length})
              </button>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110 hidden sm:block"
          >
            <X className="h-5 w-5 text-neutral-400" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          {currentRequests.length === 0 ? (
            <div className="text-center py-8 sm:py-12 animate-in fade-in-50 duration-500">
              <Image 
                src="/many_players.png" 
                alt="No requests" 
                width={80} 
                height={80} 
                className="w-16 h-16 sm:w-24 sm:h-24 mx-auto mb-3 sm:mb-4 opacity-50 animate-in zoom-in-75 duration-700"
              />
              <p className="text-neutral-400 font-minecraftia text-xs sm:text-sm leading-none mt-2 sm:mt-3 px-4">
                {activeTab === 'incoming' ? 'No incoming friend requests' : 'No sent friend requests'}
              </p>
            </div>
          ) : (
            <div className="space-y-2 sm:space-y-3">
              {currentRequests.map((request: FriendRequest, index: number) => {
                const isIncoming = activeTab === 'incoming';
                const displayName = isIncoming ? request.senderName : (request.targetName || 'Unknown Player');
                const displayUUID = isIncoming ? request.senderUUID : request.targetUUID;
                
                return (
                  <div
                    key={request.senderUUID}
                    className="flex items-center gap-3 sm:gap-4 p-3 sm:p-4 bg-neutral-900 border border-neutral-600 rounded-lg hover:border-neutral-500 transition-all duration-300 animate-in slide-in-from-bottom-3"
                    style={{ animationDelay: `${index * 50}ms`, animationDuration: '300ms' }}
                  >
                    <div className="relative flex-shrink-0">
                      <Image
                        src={getPlayerHeadUrl(displayName, displayUUID)}
                        alt={displayName}
                        width={40}
                        height={40}
                        className="w-10 h-10 sm:w-12 sm:h-12 rounded-lg transition-transform duration-300 hover:scale-110"
                        onError={handleImageError}
                      />
                      <div className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full border-2 border-neutral-900 ${
                        isIncoming ? 'bg-yellow-500 animate-pulse' : 'bg-blue-500'
                      }`} />
                    </div>

                    <div className="flex-1 min-w-0">
                      <h3 className="text-neutral-200 pt-2 font-minecraftia text-xs sm:text-sm leading-none truncate">
                        {displayName}
                      </h3>
                      <p className="text-neutral-500 pt-2 font-minecraftia text-xs leading-none mt-2 sm:mt-3 truncate">
                        <span className="hidden  sm:inline">
                          {isIncoming ? 'Sent request' : 'Sent by you'} 
                        </span>
                        <span className="sm:hidden">
                          {isIncoming ? 'From' : 'To'} 
                        </span>
                        {' '}{new Date(request.timestamp).toLocaleDateString()}
                      </p>
                    </div>

                    {isIncoming ? (
                      <div className="flex gap-1.5 sm:gap-2 flex-shrink-0">
                        <button
                          onClick={() => onAccept(request.senderUUID)}
                          disabled={actionLoading === request.senderUUID}
                          className="p-1.5 sm:p-2 rounded-lg bg-green-900/30 border border-green-800/50 hover:bg-green-900/50 transition-all duration-300 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100"
                          aria-label="Accept Request"
                        >
                          {actionLoading === request.senderUUID ? (
                            <div className="h-3 w-3 sm:h-4 sm:w-4 border border-green-400 border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <Check className="h-3 w-3 sm:h-4 sm:w-4 text-green-400" />
                          )}
                        </button>
                        <button
                          onClick={() => onReject(request.senderUUID)}
                          disabled={actionLoading === request.senderUUID}
                          className="p-1.5 sm:p-2 rounded-lg bg-red-900/30 border border-red-800/50 hover:bg-red-900/50 transition-all duration-300 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100"
                          aria-label="Reject Request"
                        >
                          <X className="h-3 w-3 sm:h-4 sm:w-4 text-red-400" />
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => request.targetUUID && onCancel(request.targetUUID)}
                        disabled={actionLoading === request.targetUUID}
                        className="p-1.5 sm:p-2 rounded-lg bg-red-900/30 border border-red-800/50 hover:bg-red-900/50 transition-all duration-300 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100 flex-shrink-0"
                        aria-label="Cancel Request"
                      >
                        {actionLoading === request.targetUUID ? (
                          <div className="h-3 w-3 sm:h-4 sm:w-4 border border-red-400 border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <X className="h-3 w-3 sm:h-4 sm:w-4 text-red-400" />
                        )}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default FriendRequestsModal;
