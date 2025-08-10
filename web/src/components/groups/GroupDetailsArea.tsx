import React from 'react';
import Image from 'next/image';
import { Hash, Settings, LogOut, Crown, Shield, MessageSquare, Copy } from 'lucide-react';
import { GroupInfo, GroupMember } from '@/lib/api';
import { getPlayerHead } from '@/lib/api';
import RankBadge from './RankBadge';
import GroupAnnouncements from './GroupAnnouncements';

interface GroupDetailsAreaProps {
  selectedGroup: GroupInfo;
  groupMembers: GroupMember[];
  userRole: string;
  actionLoading: string | null;
  groupStats: {
    memberCount: number;
    messageCount: number;
    createdAt: number;
    mostActiveMembers: { playerName: string; messageCount: number }[];
    recentActivity: { date: string; messages: number; joins: number }[];
  } | null;
  onSettingsClick: () => void;
  onLeaveGroup: (groupId: string) => void;
  currentUser?: { playerUUID: string; playerName: string } | null;
}

const GroupDetailsArea: React.FC<GroupDetailsAreaProps> = ({
  selectedGroup,
  groupMembers,
  userRole,
  actionLoading,
  groupStats,
  onSettingsClick,
  onLeaveGroup,
  currentUser
}) => {
  const [motdExpanded, setMotdExpanded] = React.useState(false);
  const motdText = (selectedGroup.motd || '').toString();

  const linkify = (text: string) => {
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const parts = text.split(urlRegex);
    return parts.map((part, i) => {
      if (urlRegex.test(part)) {
        return (
          <a
            key={`url-${i}`}
            href={part}
            target="_blank"
            rel="noopener noreferrer"
            className="text-yellow-400 hover:underline break-all"
          >
            {part}
          </a>
        );
      }
      return <span key={`txt-${i}`}>{part}</span>;
    });
  };

  const getMotdPreview = () => {
    const lines = motdText.split(/\r?\n/);
    const tooManyLines = lines.length > 5;
    const tooLong = motdText.length > 240;
    if (!tooManyLines && !tooLong) return motdText;
    if (tooManyLines) return lines.slice(0, 5).join('\n');
    return motdText.slice(0, 240) + '…';
  };

  const copyMotd = async () => {
    try {
      await navigator.clipboard.writeText(motdText);
    } catch (e) {
      console.error('Failed to copy MOTD:', e);
    }
  };
  const getDaysOld = (timestamp: number) => {
    if (!timestamp || isNaN(timestamp)) return 0;
    return Math.floor((Date.now() - timestamp) / (1000 * 60 * 60 * 24));
  };

  return (
    <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-yellow-600 rounded-lg flex items-center justify-center">
            <Hash className="w-6 h-6 text-black" />
          </div>
          <div>
            <h2 className="text-xl font-minecraftia text-white">{selectedGroup.groupName}</h2>
            <p className="text-sm text-neutral-400">{selectedGroup.description}</p>
          </div>
        </div>
        <div className="flex gap-2">
          {(userRole === 'OWNER' || userRole === 'ADMIN') && (
            <button
              onClick={onSettingsClick}
              className="text-neutral-400 hover:text-white transition-colors"
            >
              <Settings className="w-5 h-5" />
            </button>
          )}
          <button
            onClick={() => onLeaveGroup(selectedGroup.groupId)}
            disabled={actionLoading === `leave-${selectedGroup.groupId}`}
            className="text-red-400 hover:text-red-300 transition-colors"
          >
            {actionLoading === `leave-${selectedGroup.groupId}` ? (
              <div className="w-5 h-5 border-2 border-red-400 border-t-transparent rounded-full animate-spin"></div>
            ) : (
              <LogOut className="w-5 h-5" />
            )}
          </button>
        </div>
      </div>

  <div className="space-y-6 max-h-[calc(100vh-260px)] overflow-y-auto pr-1">
        {groupStats && (
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-neutral-700 rounded-lg p-4 text-center">
              <div className="text-2xl font-minecraftia text-yellow-500">
                {getDaysOld(groupStats.createdAt)}
              </div>
              <div className="text-xs text-neutral-400">Days Old</div>
            </div>
            <div className="bg-neutral-700 rounded-lg p-4 text-center">
              <div className="text-2xl font-minecraftia text-yellow-500">{selectedGroup.maxMembers}</div>
              <div className="text-xs text-neutral-400">Max Members</div>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {motdText && (
            <div className="bg-gradient-to-r from-yellow-600/20 to-orange-600/20 border border-yellow-600/30 rounded-lg p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-yellow-500" />
                  <h3 className="font-minecraftia text-yellow-500 text-sm">Message of the Day</h3>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={copyMotd}
                    title="Copy MOTD"
                    className="p-1.5 rounded hover:bg-yellow-600/20 text-yellow-400"
                  >
                    <Copy className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div className="text-neutral-200 text-sm leading-relaxed whitespace-pre-wrap break-words">
                {motdExpanded ? linkify(motdText) : linkify(getMotdPreview())}
              </div>
              {(motdText.length > 240 || motdText.split(/\r?\n/).length > 5) && (
                <div className="mt-2 text-right">
                  <button
                    onClick={() => setMotdExpanded(e => !e)}
                    className="text-xs text-yellow-400 hover:text-yellow-300"
                  >
                    {motdExpanded ? 'Show less' : 'Show more'}
                  </button>
                </div>
              )}
            </div>
          )}

          <GroupAnnouncements
            group={selectedGroup}
            userRole={userRole}
            currentUser={currentUser}
          />

        </div>

        <div>
          <h3 className="font-minecraftia text-neutral-200 text-sm mb-4">
            Members ({groupMembers.length})
          </h3>
          <div className="space-y-3">
            {groupMembers.length === 0 ? (
              <div className="flex items-center justify-center py-8">
                <div className="w-6 h-6 border-2 border-neutral-400 border-t-transparent rounded-full animate-spin"></div>
                <span className="ml-2 text-neutral-400 text-sm">Loading members...</span>
              </div>
            ) : (
              <>
                {groupMembers.filter(member => member.online).length > 0 && (
                  <div>
                    <h4 className="text-xs text-green-400 font-inter mb-2">
                      Online — {groupMembers.filter(member => member.online).length}
                    </h4>
                    {groupMembers.filter(member => member.online).map((member, idx) => (
                      <div key={`member-${member.playerUUID || member.playerName || 'unknown'}-${member.joinedAt || idx}`} className="flex items-center justify-between p-3 bg-neutral-700 rounded-lg mb-2">
                        <div className="flex items-center gap-3">
                          <div className="relative">
                            <Image
                              src={getPlayerHead(member.playerUUID, member.playerName)}
                              alt={member.playerName}
                              width={32}
                              height={32}
                              className="rounded"
                            />
                            <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 border-2 border-neutral-700 rounded-full"></div>
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-white font-inter font-semibold text-sm">{member.playerName}</span>
                              {member.role === 'OWNER' && <Crown className="w-4 h-4 text-yellow-500" />}
                              {member.role === 'ADMIN' && <Shield className="w-4 h-4 text-blue-500" />}
                              {member.rank && <RankBadge rank={member.rank} />}
                            </div>
                            <div className="text-xs text-neutral-400">
                              {member.role}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {groupMembers.filter(member => !member.online).length > 0 && (
                  <div>
                    <h4 className="text-xs text-neutral-500 font-inter mb-2">
                      Offline — {groupMembers.filter(member => !member.online).length}
                    </h4>
                    {groupMembers.filter(member => !member.online).map((member, idx) => (
                      <div key={`member-${member.playerUUID || member.playerName || 'unknown'}-${member.joinedAt || idx}`} className="flex items-center justify-between p-3 bg-neutral-700 rounded-lg mb-2">
                        <div className="flex items-center gap-3">
                          <div className="relative">
                            <Image
                              src={getPlayerHead(member.playerUUID, member.playerName)}
                              alt={member.playerName}
                              width={32}
                              height={32}
                              className="rounded opacity-60"
                            />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-neutral-400 font-inter font-semibold text-sm">{member.playerName}</span>
                              {member.role === 'OWNER' && <Crown className="w-4 h-4 text-yellow-500 opacity-60" />}
                              {member.role === 'ADMIN' && <Shield className="w-4 h-4 text-blue-500 opacity-60" />}
                              {member.rank && <RankBadge rank={member.rank} />}
                            </div>
                            <div className="text-xs text-neutral-500">
                              {member.role}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default GroupDetailsArea;