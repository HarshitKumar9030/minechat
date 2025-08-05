'use client';
import React, { useState, useEffect } from 'react';
import { X, Save, Trash2, Users, Lock, Unlock, Settings } from 'lucide-react';
import { GroupInfo, api } from '@/lib/api';

interface GroupSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  group: GroupInfo | null;
  onUpdate: () => void;
}

const GroupSettingsModal: React.FC<GroupSettingsModalProps> = ({
  isOpen,
  onClose,
  group,
  onUpdate
}) => {
  const [formData, setFormData] = useState({
    groupName: '',
    description: '',
    maxMembers: 10,
    isPrivate: false
  });

  const [loading, setLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const currentUser = {
    playerUUID: localStorage.getItem('playerUUID') || '',
    playerName: localStorage.getItem('playerName') || '',
  };

  useEffect(() => {
    if (group) {
      setFormData({
        groupName: group.groupName,
        description: group.description,
        maxMembers: group.maxMembers,
        isPrivate: group.isPrivate || false
      });
    }
  }, [group]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!group) return;
    
    // Validation
    const newErrors: Record<string, string> = {};
    
    if (!formData.groupName.trim()) {
      newErrors.groupName = 'Group name is required';
    } else if (formData.groupName.length < 3) {
      newErrors.groupName = 'Group name must be at least 3 characters';
    } else if (formData.groupName.length > 32) {
      newErrors.groupName = 'Group name must be less than 32 characters';
    }
    
    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    } else if (formData.description.length > 256) {
      newErrors.description = 'Description must be less than 256 characters';
    }
    
    if (formData.maxMembers < group.memberCount) {
      newErrors.maxMembers = `Cannot be less than current member count (${group.memberCount})`;
    } else if (formData.maxMembers > 50) {
      newErrors.maxMembers = 'Maximum 50 members allowed';
    }
    
    setErrors(newErrors);
    
    if (Object.keys(newErrors).length === 0) {
      try {
        setLoading(true);
        await api.updateGroup(group.groupId, {
          groupName: formData.groupName,
          description: formData.description,
          maxMembers: formData.maxMembers,
          isPrivate: formData.isPrivate
        });
        onUpdate();
      } catch (error) {
        console.error('Failed to update group:', error);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleDeleteGroup = async () => {
    if (!group) return;
    
    try {
      setDeleteLoading(true);
      await api.deleteGroup(group.groupId, currentUser.playerUUID);
      onUpdate();
      onClose();
    } catch (error) {
      console.error('Failed to delete group:', error);
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen || !group) return null;

  const isOwner = group.role === 'OWNER';

  return (
    <div 
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 animate-in fade-in duration-200"
      onClick={handleBackdropClick}
    >
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-4 sm:p-6 w-full max-w-sm sm:max-w-md lg:max-w-lg animate-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4 sm:mb-6">
          <div className="flex items-center gap-3">
            <Settings className="h-5 w-5 text-neutral-400" />
            <h2 className="text-lg sm:text-xl font-minecraftia text-neutral-200 leading-none mt-2">
              Group Settings
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110"
          >
            <X className="h-5 w-5 text-neutral-400" />
          </button>
        </div>

        {!isOwner ? (
          <div className="text-center py-8">
            <Lock className="h-12 w-12 text-neutral-500 mx-auto mb-4" />
            <h3 className="font-minecraftia text-neutral-300 text-sm mb-2 leading-none">
              Access Denied
            </h3>
            <p className="text-neutral-400 font-inter text-sm">
              Only group owners can modify settings
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3 sm:space-y-4">
            {/* Group Name */}
            <div>
              <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none">
                Group Name *
              </label>
              <input
                type="text"
                value={formData.groupName}
                onChange={(e) => setFormData(prev => ({ ...prev, groupName: e.target.value }))}
                placeholder="Enter group name..."
                className={`w-full px-3 sm:px-4 py-2 sm:py-3 bg-neutral-700 border rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:outline-none transition-all duration-300 ${
                  errors.groupName ? 'border-red-500 focus:border-red-400' : 'border-neutral-600 focus:border-yellow-600'
                }`}
                maxLength={32}
              />
              {errors.groupName && (
                <p className="text-red-400 font-inter text-xs mt-1">{errors.groupName}</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none">
                Description *
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Describe your group..."
                rows={3}
                className={`w-full px-3 sm:px-4 py-2 sm:py-3 bg-neutral-700 border rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:outline-none transition-all duration-300 resize-none ${
                  errors.description ? 'border-red-500 focus:border-red-400' : 'border-neutral-600 focus:border-yellow-600'
                }`}
                maxLength={256}
              />
              <div className="flex justify-between items-center mt-1">
                {errors.description ? (
                  <p className="text-red-400 font-inter text-xs">{errors.description}</p>
                ) : (
                  <div />
                )}
                <span className="text-neutral-500 font-inter text-xs">
                  {formData.description.length}/256
                </span>
              </div>
            </div>

            <div>
              <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none">
                Maximum Members
              </label>
              <div className="relative">
                <Users className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-400" />
                <input
                  type="number"
                  value={formData.maxMembers}
                  onChange={(e) => setFormData(prev => ({ ...prev, maxMembers: parseInt(e.target.value) || group.memberCount }))}
                  min={group.memberCount}
                  max={50}
                  className={`w-full pl-10 pr-4 py-3 bg-neutral-700 border rounded-lg text-neutral-200 font-inter focus:outline-none transition-all duration-300 ${
                    errors.maxMembers ? 'border-red-500 focus:border-red-400' : 'border-neutral-600 focus:border-yellow-600'
                  }`}
                />
              </div>
              {errors.maxMembers && (
                <p className="text-red-400 font-inter text-xs mt-1">{errors.maxMembers}</p>
              )}
              <p className="text-neutral-500 font-inter text-xs mt-1">
                Current members: {group.memberCount}
              </p>
            </div>

            <div>
              <label className="flex items-center gap-3 p-3 bg-neutral-700 border border-neutral-600 rounded-lg hover:border-neutral-500 transition-all duration-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.isPrivate}
                  onChange={(e) => setFormData(prev => ({ ...prev, isPrivate: e.target.checked }))}
                  className="sr-only"
                />
                <div className={`flex items-center justify-center w-5 h-5 rounded border-2 transition-all duration-300 ${
                  formData.isPrivate 
                    ? 'bg-yellow-600 border-yellow-600' 
                    : 'border-neutral-400 hover:border-neutral-300'
                }`}>
                  {formData.isPrivate && (
                    <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </div>
                
                <div className="flex items-center gap-2">
                  {formData.isPrivate ? (
                    <Lock className="h-4 w-4 text-neutral-400" />
                  ) : (
                    <Unlock className="h-4 w-4 text-neutral-400" />
                  )}
                  <div>
                    <span className="text-neutral-200 font-minecraftia text-sm leading-none">
                      Private Group
                    </span>
                    <p className="text-neutral-400 font-inter text-xs mt-1">
                      {formData.isPrivate 
                        ? 'Only invited members can join' 
                        : 'Anyone can join this group'
                      }
                    </p>
                  </div>
                </div>
              </label>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-3 sm:px-4 py-2 sm:py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-300 hover:bg-neutral-600 transition-all duration-300 font-inter text-sm sm:text-base"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 px-3 sm:px-4 py-2 sm:py-3 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-neutral-900 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed font-inter flex items-center justify-center gap-2 text-sm sm:text-base"
              >
                {loading ? (
                  <>
                    <div className="h-4 w-4 border border-neutral-900 border-t-transparent rounded-full animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="h-4 w-4" />
                    Save Changes
                  </>
                )}
              </button>
            </div>

            <div className="pt-4 border-t border-neutral-700">
              <div className="bg-red-900/10 border border-red-800/30 rounded-lg p-4">
                <h3 className="font-minecraftia text-red-400 text-sm mb-2 leading-none">
                  Danger Zone
                </h3>
                <p className="text-neutral-400 font-inter text-xs mb-3">
                  Permanently delete this group. This action cannot be undone.
                </p>
                
                {!showDeleteConfirm ? (
                  <button
                    type="button"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="flex items-center gap-2 px-3 py-2 bg-red-900/30 border border-red-800/50 rounded-lg text-red-400 hover:bg-red-900/50 transition-all duration-300 font-inter text-sm"
                  >
                    <Trash2 className="h-4 w-4" />
                    Delete Group
                  </button>
                ) : (
                  <div className="space-y-3">
                    <p className="text-red-300 font-inter text-xs">
                      Are you sure? Type the group name to confirm:
                    </p>
                    <input
                      type="text"
                      placeholder={group.groupName}
                      className="w-full px-3 py-2 bg-neutral-700 border border-red-500 rounded text-neutral-200 placeholder-neutral-400 font-inter text-sm focus:outline-none"
                    />
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => setShowDeleteConfirm(false)}
                        className="px-3 py-2 bg-neutral-700 border border-neutral-600 rounded text-neutral-300 hover:bg-neutral-600 transition-all duration-300 font-inter text-sm"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={handleDeleteGroup}
                        disabled={deleteLoading}
                        className="px-3 py-2 bg-red-900/30 border border-red-800/50 rounded text-red-400 hover:bg-red-900/50 transition-all duration-300 disabled:opacity-50 font-inter text-sm flex items-center gap-2"
                      >
                        {deleteLoading ? (
                          <>
                            <div className="h-3 w-3 border border-red-400 border-t-transparent rounded-full animate-spin" />
                            Deleting...
                          </>
                        ) : (
                          <>
                            <Trash2 className="h-3 w-3" />
                            Delete Forever
                          </>
                        )}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default GroupSettingsModal;
