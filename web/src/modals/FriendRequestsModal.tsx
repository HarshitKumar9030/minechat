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
  actionLoading: string | null;
}

const FriendRequestsModal: React.FC<FriendRequestsModalProps> = ({ 
  isOpen, 
  onClose, 
  incomingRequests, 
  sentRequests, 
  onAccept, 
  onReject, 
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
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-6 w-full max-w-2xl max-h-[80vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between mb-6">
          <div className="flex-1">
            <h2 className="text-xl mt-2 font-minecraftia text-neutral-200 leading-none mb-4">
              Friend Requests
            </h2>
            
            <div className="flex gap-1 bg-neutral-900 p-1 rounded-lg">
              <button
                onClick={() => setActiveTab('incoming')}
                className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-md font-inter text-sm transition-all duration-200 ${
                  activeTab === 'incoming'
                    ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                    : 'text-neutral-400 hover:text-neutral-300'
                }`}
              >
                <Users className="h-4 w-4" />
                Incoming ({incomingRequests.length})
              </button>
              <button
                onClick={() => setActiveTab('sent')}
                className={`flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-md font-inter text-sm transition-all duration-200 ${
                  activeTab === 'sent'
                    ? 'bg-neutral-700 text-neutral-200 shadow-sm'
                    : 'text-neutral-400 hover:text-neutral-300'
                }`}
              >
                <Send className="h-4 w-4" />
                Sent ({sentRequests.length})
              </button>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110"
          >
            <X className="h-5 w-5 text-neutral-400" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          {currentRequests.length === 0 ? (
            <div className="text-center py-12 animate-in fade-in-50 duration-500">
              <Image 
                src="/many_players.png" 
                alt="No requests" 
                width={96} 
                height={96} 
                className="mx-auto mb-4 opacity-50 animate-in zoom-in-75 duration-700"
              />
              <p className="text-neutral-400 font-minecraftia text-sm leading-none mt-3">
                {activeTab === 'incoming' ? 'No incoming friend requests' : 'No sent friend requests'}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {currentRequests.map((request: FriendRequest, index: number) => {
                const isIncoming = activeTab === 'incoming';
                const displayName = isIncoming ? request.senderName : (request.targetName || 'Unknown Player');
                const displayUUID = isIncoming ? request.senderUUID : request.targetUUID;
                
                return (
                  <div
                    key={request.senderUUID}
                    className="flex items-center gap-4 p-4 bg-neutral-900 border border-neutral-600 rounded-lg hover:border-neutral-500 transition-all duration-300  animate-in slide-in-from-bottom-3"
                    style={{ animationDelay: `${index * 50}ms`, animationDuration: '300ms' }}
                  >
                    <div className="relative">
                      <Image
                        src={getPlayerHeadUrl(displayName, displayUUID)}
                        alt={displayName}
                        width={48}
                        height={48}
                        className="rounded-lg transition-transform duration-300 hover:scale-110"
                        onError={handleImageError}
                      />
                      <div className={`absolute -bottom-1 -right-1 w-3 h-3 rounded-full border-2 border-neutral-900 ${
                        isIncoming ? 'bg-yellow-500 animate-pulse' : 'bg-blue-500'
                      }`} />
                    </div>

                    <div className="flex-1">
                      <h3 className="text-neutral-200 font-minecraftia text-sm leading-none">
                        {displayName}
                      </h3>
                      <p className="text-neutral-500 font-minecraftia text-xs leading-none mt-3">
                        {isIncoming ? 'Sent request' : 'Sent to you'} {new Date(request.timestamp).toLocaleDateString()}
                      </p>
                    </div>

                    {isIncoming ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => onAccept(request.senderUUID)}
                          disabled={actionLoading === request.senderUUID}
                          className="p-2 rounded-lg bg-green-900/30 border border-green-800/50 hover:bg-green-900/50 transition-all duration-300 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100"
                          aria-label="Accept Request"
                        >
                          {actionLoading === request.senderUUID ? (
                            <div className="h-4 w-4 border border-green-400 border-t-transparent rounded-full animate-spin" />
                          ) : (
                            <Check className="h-4 w-4 text-green-400" />
                          )}
                        </button>
                        <button
                          onClick={() => onReject(request.senderUUID)}
                          disabled={actionLoading === request.senderUUID}
                          className="p-2 rounded-lg bg-red-900/30 border border-red-800/50 hover:bg-red-900/50 transition-all duration-300 disabled:opacity-50 hover:scale-110 disabled:hover:scale-100"
                          aria-label="Reject Request"
                        >
                          <X className="h-4 w-4 text-red-400" />
                        </button>
                      </div>
                    ) : (
                      <div className="text-neutral-500 font-minecraftia text-xs">
                        Pending...
                      </div>
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
