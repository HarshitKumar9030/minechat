import React, { useState, useEffect } from 'react';
import { Mail, Check, X, Clock, Users } from 'lucide-react';
import { api, GroupInvite } from '@/lib/api';

interface GroupInvitesProps {
  user?: { playerUUID: string; playerName: string } | null;
  onInviteAccepted?: (groupId: string) => void;
}

const GroupInvites: React.FC<GroupInvitesProps> = ({
  user,
  onInviteAccepted
}) => {
  const [invites, setInvites] = useState<GroupInvite[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingInvite, setProcessingInvite] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      loadInvites();
    }
  }, [user]);

  const loadInvites = async () => {
    if (!user) return;

    try {
      setLoading(true);
      const response = await api.getGroupInvites(user.playerUUID);
      if (response.invites) {
        setInvites(response.invites);
      }
    } catch (error) {
      console.error('Failed to load invites:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptInvite = async (invite: GroupInvite) => {
    if (!user || processingInvite) return;

    try {
      setProcessingInvite(invite.inviteId);
      const response = await api.acceptGroupInvite(invite.inviteId, user.playerUUID);
      
      if (response && typeof response === 'object' && 'success' in response && response.success) {
        setInvites(prev => prev.filter(inv => inv.inviteId !== invite.inviteId));
        
        if (onInviteAccepted) {
          onInviteAccepted(invite.groupId);
        }
      }
    } catch (error) {
      console.error('Failed to accept invite:', error);
    } finally {
      setProcessingInvite(null);
    }
  };

  const handleDeclineInvite = async (invite: GroupInvite) => {
    if (!user || processingInvite) return;

    try {
      setProcessingInvite(invite.inviteId);
      const response = await api.rejectGroupInvite(invite.inviteId, user.playerUUID);
      
      if (response && typeof response === 'object' && 'success' in response && response.success) {
        setInvites(prev => prev.filter(inv => inv.inviteId !== invite.inviteId));
      }
    } catch (error) {
      console.error('Failed to decline invite:', error);
    } finally {
      setProcessingInvite(null);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) return 'just now';
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Mail className="w-6 h-6 text-yellow-600" />
        <div>
          <h2 className="text-2xl font-bold text-neutral-100 font-minecraftia">
            Group Invites
          </h2>
          <p className="text-neutral-400 mt-1">
            Manage your pending group invitations
          </p>
        </div>
      </div>

      <div className="space-y-4">
        {loading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 animate-pulse">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-neutral-700 rounded-lg"></div>
                  <div>
                    <div className="h-5 bg-neutral-700 rounded w-32 mb-2"></div>
                    <div className="h-4 bg-neutral-700 rounded w-24"></div>
                  </div>
                </div>
                <div className="flex gap-2">
                  <div className="w-20 h-8 bg-neutral-700 rounded"></div>
                  <div className="w-20 h-8 bg-neutral-700 rounded"></div>
                </div>
              </div>
            </div>
          ))
        ) : invites.length === 0 ? (
          <div className="text-center py-12">
            <Mail className="w-16 h-16 text-neutral-600 mx-auto mb-4" />
            <h3 className="text-xl font-minecraftia text-neutral-300 mb-2">
              No pending invites
            </h3>
            <p className="text-neutral-500">
              You&apos;ll see group invitations here when you receive them
            </p>
          </div>
        ) : (
          invites.map((invite) => (
            <div
              key={invite.inviteId}
              className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 hover:border-neutral-600 transition-all duration-300"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-gradient-to-br from-yellow-600 to-yellow-700 rounded-lg flex items-center justify-center">
                    <Users className="w-6 h-6 text-neutral-900" />
                  </div>
                  <div>
                    <h3 className="font-minecraftia text-lg text-neutral-100">
                      {invite.groupName}
                    </h3>
                    <div className="flex items-center gap-2 text-sm text-neutral-400">
                      <span>Invited by {invite.inviterName}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {formatTimestamp(invite.timestamp)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => handleDeclineInvite(invite)}
                    disabled={processingInvite === invite.inviteId}
                    className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-neutral-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors flex items-center gap-2"
                  >
                    {processingInvite === invite.inviteId ? (
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    ) : (
                      <X className="w-4 h-4" />
                    )}
                    Decline
                  </button>
                  <button
                    onClick={() => handleAcceptInvite(invite)}
                    disabled={processingInvite === invite.inviteId}
                    className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-neutral-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors flex items-center gap-2"
                  >
                    {processingInvite === invite.inviteId ? (
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    ) : (
                      <Check className="w-4 h-4" />
                    )}
                    Accept
                  </button>
                </div>
              </div>

              {invite.message && (
                <div className="mt-4 p-3 bg-neutral-700 rounded-lg">
                  <p className="text-sm text-neutral-300">
                    &ldquo;{invite.message}&ldquo;
                  </p>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {!loading && invites.length > 0 && (
        <div className="text-center">
          <button
            onClick={loadInvites}
            className="px-6 py-2 bg-neutral-700 hover:bg-neutral-600 text-neutral-300 rounded-lg transition-colors text-sm"
          >
            Refresh Invites
          </button>
        </div>
      )}
    </div>
  );
};

export default GroupInvites;
