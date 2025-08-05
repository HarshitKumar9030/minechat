import React from 'react';
import { AlertTriangle, Users, X } from 'lucide-react';
import { GroupInfo } from '@/lib/api';

interface AlreadyMemberModalProps {
  isOpen: boolean;
  onClose: () => void;
  group: GroupInfo | null;
  onSwitchToGroup?: (group: GroupInfo) => void;
}

const AlreadyMemberModal: React.FC<AlreadyMemberModalProps> = ({
  isOpen,
  onClose,
  group,
  onSwitchToGroup
}) => {
  if (!isOpen || !group) return null;

  const handleSwitchToGroup = () => {
    if (onSwitchToGroup) {
      onSwitchToGroup(group);
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6 w-full max-w-md mx-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-transparent rounded-lg flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-yellow-600" />
            </div>
            <div>
              <h3 className=" text-lg text-neutral-100 font-inter font-bold">
                Already a Member
              </h3>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 hover:bg-neutral-700 rounded transition-colors"
          >
            <X className="w-5 h-5 text-neutral-400" />
          </button>
        </div>

        <div className="space-y-4">
          <div className="flex items-center gap-3 p-4 bg-orange-900/20 border border-orange-600/30 rounded-lg">
            <Users className="w-5 h-5 text-orange-400" />
            <div>
              <p className="text-neutral-200 font-medium">
                You&apos;re already a member of &quot;{group.groupName}&quot;
              </p>
              <p className="text-sm text-neutral-400 mt-1">
                You cannot join a group you&apos;re already part of.
              </p>
            </div>
          </div>

          <div className="bg-neutral-700 rounded-lg p-4">
            <h4 className="font-minecraftia text-sm text-neutral-200 mb-2">
              Group Information
            </h4>
            <div className="space-y-2 text-sm text-neutral-300">
              <div className="flex justify-between">
                <span>Group Name:</span>
                <span>{group.groupName}</span>
              </div>
              <div className="flex justify-between">
                <span>Members:</span>
                <span>{group.memberCount}/{group.maxMembers}</span>
              </div>
              {group.description && (
                <div>
                  <span className="block text-neutral-400 mb-1">Description:</span>
                  <span className="text-neutral-300">{group.description}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 mt-6">
          {onSwitchToGroup && (
            <button
              onClick={handleSwitchToGroup}
              className="px-4 py-2 bg-neutral-700 hover:bg-neutral-600 text-neutral-200 rounded-lg transition-colors font-medium"
            >
              Go to Group
            </button>
          )}
          <button
            onClick={onClose}
            className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 rounded-lg transition-colors font-medium"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  );
};

export default AlreadyMemberModal;
