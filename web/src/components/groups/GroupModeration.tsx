import React, { useState } from 'react';
import { Shield, Ban, UserX, VolumeX, Crown, Settings, AlertTriangle, Users } from 'lucide-react';
import { GroupInfo, GroupMember, moderationApi } from '@/lib/api';
import RankBadge from './RankBadge';
import { getPlayerHead } from '@/lib/api';
import Image from 'next/image';

interface GroupModerationProps {
  group: GroupInfo;
  members: GroupMember[];
  userRole: string;
  currentUser?: { playerUUID: string; playerName: string } | null;
  onMemberAction?: () => void;
}

interface ModerationAction {
  type: 'kick' | 'ban' | 'mute' | 'unmute' | 'promote' | 'demote';
  targetUUID: string;
  targetName: string;
  reason?: string;
  duration?: number;
}

interface ActionItem {
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  description: string;
}

const GroupModeration: React.FC<GroupModerationProps> = ({
  group,
  members,
  userRole,
  currentUser,
  onMemberAction
}) => {
  const [selectedMember, setSelectedMember] = useState<GroupMember | null>(null);
  const [showActionModal, setShowActionModal] = useState(false);
  const [pendingAction, setPendingAction] = useState<ModerationAction | null>(null);
  const [actionReason, setActionReason] = useState('');
  const [muteDuration, setMuteDuration] = useState<number>(60);
  const [processing, setProcessing] = useState(false);

  const canModerate = userRole === 'OWNER' || userRole === 'ADMIN';
  const isOwner = userRole === 'OWNER';

  const getMemberActions = (member: GroupMember): ActionItem[] => {
    const actions: ActionItem[] = [];
    
    if (member.playerUUID === currentUser?.playerUUID) return actions;
    if (member.role === 'OWNER') return actions;
    
    if (userRole === 'ADMIN' && member.role === 'ADMIN') return actions;

    actions.push(
      { 
        id: 'kick', 
        label: 'Kick Member', 
        icon: UserX, 
        color: 'text-orange-400 hover:text-orange-300',
        description: 'Remove member from group temporarily' 
      },
      { 
        id: 'ban', 
        label: 'Ban Member', 
        icon: Ban, 
        color: 'text-red-400 hover:text-red-300',
        description: 'Permanently ban member from group' 
      },
      { 
        id: 'mute', 
        label: 'Mute Member', 
        icon: VolumeX, 
        color: 'text-yellow-400 hover:text-yellow-300',
        description: 'Prevent member from sending messages' 
      }
    );

    if (isOwner) {
      if (member.role === 'MEMBER') {
        actions.push({
          id: 'promote',
          label: 'Promote to Admin',
          icon: Crown,
          color: 'text-emerald-400 hover:text-emerald-300',
          description: 'Give member administrative privileges'
        });
      } else if (member.role === 'ADMIN') {
        actions.push({
          id: 'demote',
          label: 'Demote to Member',
          icon: Shield,
          color: 'text-stone-400 hover:text-stone-300',
          description: 'Remove administrative privileges'
        });
      }
    }

    return actions;
  };

  const handleActionClick = (action: string, member: GroupMember) => {
    setPendingAction({
      type: action as ModerationAction['type'],
      targetUUID: member.playerUUID,
      targetName: member.playerName
    });
    setSelectedMember(member);
    setActionReason('');
    setMuteDuration(60);
    setShowActionModal(true);
  };

  const handleConfirmAction = async () => {
    if (!pendingAction) return;

    setProcessing(true);
    try {
      switch (pendingAction.type) {
        case 'kick':
          await moderationApi.kickMember(group.groupId, pendingAction.targetUUID, actionReason);
          break;
        case 'ban':
          await moderationApi.banMember(group.groupId, pendingAction.targetUUID, actionReason);
          break;
        case 'mute':
          await moderationApi.muteMember(group.groupId, pendingAction.targetUUID, muteDuration);
          break;
        case 'unmute':
          await moderationApi.unmuteMember(group.groupId, pendingAction.targetUUID);
          break;
        case 'promote':
          await moderationApi.promoteMember(group.groupId, pendingAction.targetUUID);
          break;
        case 'demote':
          await moderationApi.demoteMember(group.groupId, pendingAction.targetUUID);
          break;
      }
      handleCloseModal();
      onMemberAction?.();
    } catch (error) {
      console.error('Failed to execute moderation action:', error);
    } finally {
      setProcessing(false);
    }
  };

  const handleCloseModal = () => {
    setShowActionModal(false);
    setPendingAction(null);
    setSelectedMember(null);
    setActionReason('');
    setMuteDuration(60);
  };

  const getActionColor = (actionType: string) => {
    switch (actionType) {
      case 'kick': return 'border-orange-500/40 bg-orange-950/30';
      case 'ban': return 'border-red-500/40 bg-red-950/30';
      case 'mute': return 'border-yellow-500/40 bg-yellow-950/30';
      case 'promote': return 'border-emerald-500/40 bg-emerald-950/30';
      case 'demote': return 'border-slate-500/40 bg-slate-950/30';
      default: return 'border-stone-500/40 bg-stone-950/30';
    }
  };

  if (!canModerate) {
    return (
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
        <div className="text-center py-8">
          <Shield className="w-12 h-12 text-neutral-600 mx-auto mb-3" />
          <p className="text-neutral-400">You don&apos;t have moderation permissions</p>
          <p className="text-sm text-neutral-500 mt-1">
            Only group admins and owners can access moderation tools
          </p>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-yellow-600 rounded-lg flex items-center justify-center">
            <Shield className="w-5 h-5 text-black" />
          </div>
          <div>
            <h3 className="font-minecraftia text-lg text-stone-100">
              Moderation Tools
            </h3>
            <p className="text-sm text-stone-400">
              Manage group members and maintain order
            </p>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between p-3 bg-neutral-700 rounded-lg">
            <div className="flex items-center gap-3">
              <Users className="w-5 h-5 text-neutral-400" />
              <div>
                <p className="text-neutral-200 font-medium">Members to Moderate</p>
                <p className="text-sm text-neutral-400">
                  {members.filter(m => m.playerUUID !== currentUser?.playerUUID && m.role !== 'OWNER').length} members available
                </p>
              </div>
            </div>
            <div className="text-sm text-neutral-500">
              {isOwner ? 'Owner' : 'Admin'} permissions
            </div>
          </div>

          <div className="grid gap-3">
            {members
              .filter(member => member.playerUUID !== currentUser?.playerUUID && member.role !== 'OWNER')
              .map((member) => {
                const actions = getMemberActions(member);
                return (
                  <div
                    key={member.playerUUID}
                    className="flex items-center justify-between p-3 bg-neutral-700 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <div className="relative">
                        <Image
                          src={getPlayerHead(member.playerName, member.playerUUID)}
                          alt={member.playerName}
                          width={32}
                          height={32}
                          className="rounded"
                        />
                        {member.online && (
                          <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 border-2 border-stone-800 rounded-full"></div>
                        )}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-white font-minecraftia text-sm">
                            {member.playerName}
                          </span>
                          {member.role === 'ADMIN' && <Shield className="w-4 h-4 text-yellow-500" />}
                          {member.rank && <RankBadge rank={member.rank} />}
                        </div>
                        <div className="text-xs text-stone-400">
                          {member.role} • {member.online ? 'Online' : 'Offline'}
                        </div>
                      </div>
                    </div>

                    <div className="flex gap-1">
                      {actions.map((action) => (
                        <button
                          key={action.id}
                          onClick={() => handleActionClick(action.id, member)}
                          className={`p-2 ${action.color} transition-colors rounded`}
                          title={action.description}
                        >
                          <action.icon className="w-4 h-4" />
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}

            {members.filter(m => m.playerUUID !== currentUser?.playerUUID && m.role !== 'OWNER').length === 0 && (
              <div className="text-center py-8">
                <Settings className="w-12 h-12 text-neutral-600 mx-auto mb-3" />
                <p className="text-neutral-400">No members to moderate</p>
                <p className="text-sm text-neutral-500 mt-1">
                  All group members have equal or higher permissions
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {showActionModal && pendingAction && selectedMember && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 w-full max-w-md mx-4">
            <div className="flex items-center gap-3 mb-4">
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${getActionColor(pendingAction.type)}`}>
                <AlertTriangle className="w-5 h-5 text-yellow-400" />
              </div>
              <div>
                <h3 className="font-bold text-lg text-neutral-100 font-minecraftia capitalize">
                  {pendingAction.type} Member
                </h3>
                <p className="text-sm text-neutral-400">
                  {selectedMember.playerName}
                </p>
              </div>
            </div>

            <div className="space-y-4">
              {(pendingAction.type === 'kick' || pendingAction.type === 'ban') && (
                <div>
                  <label className="block text-sm font-medium text-neutral-300 mb-2">
                    Reason (optional)
                  </label>
                  <textarea
                    value={actionReason}
                    onChange={(e) => setActionReason(e.target.value)}
                    className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 focus:outline-none focus:border-yellow-500 resize-none"
                    rows={3}
                    maxLength={200}
                    placeholder={`Why are you ${pendingAction.type}ing this member?`}
                  />
                </div>
              )}

              {pendingAction.type === 'mute' && (
                <div>
                  <label className="block text-sm font-medium text-neutral-300 mb-2">
                    Mute Duration
                  </label>
                  <select
                    value={muteDuration}
                    onChange={(e) => setMuteDuration(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 focus:outline-none focus:border-yellow-500"
                  >
                    <option value={5}>5 minutes</option>
                    <option value={15}>15 minutes</option>
                    <option value={30}>30 minutes</option>
                    <option value={60}>1 hour</option>
                    <option value={180}>3 hours</option>
                    <option value={360}>6 hours</option>
                    <option value={720}>12 hours</option>
                    <option value={1440}>24 hours</option>
                  </select>
                </div>
              )}

              <div className={`p-3 rounded-lg border ${getActionColor(pendingAction.type)}`}>
                <p className="text-sm text-neutral-300">
                  Are you sure you want to <strong>{pendingAction.type}</strong> {selectedMember.playerName}?
                  {pendingAction.type === 'ban' && ' This action will permanently remove them from the group.'}
                  {pendingAction.type === 'kick' && ' They will be removed from the group but can rejoin.'}
                  {pendingAction.type === 'mute' && ' They will not be able to send messages for the specified duration.'}
                  {pendingAction.type === 'promote' && ' They will gain administrative privileges.'}
                  {pendingAction.type === 'demote' && ' They will lose administrative privileges.'}
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={handleCloseModal}
                disabled={processing}
                className="px-4 py-2 text-neutral-400 hover:text-neutral-200 transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmAction}
                disabled={processing}
                className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-neutral-600 text-black rounded-lg transition-colors font-medium"
              >
                {processing ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                ) : (
                  `Confirm ${pendingAction.type}`
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default GroupModeration;
