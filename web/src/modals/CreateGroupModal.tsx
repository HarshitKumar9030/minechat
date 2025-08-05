'use client';
import React, { useState } from 'react';
import { X, Users, Hash, Lock, Unlock } from 'lucide-react';

interface CreateGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (groupData: {
    groupName: string;
    description: string;
    maxMembers: number;
    isPrivate: boolean;
  }) => Promise<void>;
  loading: boolean;
  currentGroupCount?: number;
  maxGroupsAllowed?: number;
}

const CreateGroupModal: React.FC<CreateGroupModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  loading,
  currentGroupCount = 0,
  maxGroupsAllowed = 15
}) => {
  const [formData, setFormData] = useState({
    groupName: '',
    description: '',
    maxMembers: 10,
    isPrivate: false
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: Record<string, string> = {};
    
    // Check group limit first
    if (currentGroupCount >= maxGroupsAllowed) {
      newErrors.general = `You have reached the maximum limit of ${maxGroupsAllowed} groups. Please delete some groups before creating new ones.`;
      setErrors(newErrors);
      return;
    }
    
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
    
    if (formData.maxMembers < 2) {
      newErrors.maxMembers = 'Group must allow at least 2 members';
    } else if (formData.maxMembers > 50) {
      newErrors.maxMembers = 'Maximum 50 members allowed';
    }
    
    setErrors(newErrors);
    
    if (Object.keys(newErrors).length === 0) {
      try {
        await onSubmit(formData);
        setFormData({
          groupName: '',
          description: '',
          maxMembers: 10,
          isPrivate: false
        });
        setErrors({});
      } catch (error) {
        console.error('Failed to create group:', error);
      }
    }
  };

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
      <div className="bg-neutral-800 border border-neutral-700 rounded-xl p-6 w-full max-w-md animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-minecraftia text-neutral-200 leading-none mt-2">
              Create Group
            </h2>
            <p className="text-xs text-neutral-400 mt-1 font-inter">
              Groups: {currentGroupCount}/{maxGroupsAllowed}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-700 hover:bg-neutral-600 transition-all duration-300 hover:scale-110"
          >
            <X className="h-5 w-5 text-neutral-400" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {errors.general && (
            <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
              <p className="text-red-400 text-sm font-inter">{errors.general}</p>
            </div>
          )}
          
          {currentGroupCount >= maxGroupsAllowed - 2 && currentGroupCount < maxGroupsAllowed && (
            <div className="p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-lg">
              <p className="text-yellow-400 text-sm font-inter">
                Warning: You&apos;re approaching the group limit ({currentGroupCount}/{maxGroupsAllowed})
              </p>
            </div>
          )}
          
          <div>
            <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none mt-2">
              Group Name *
            </label>
            <div className="relative">
              <Hash className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-400" />
              <input
                type="text"
                value={formData.groupName}
                onChange={(e) => setFormData(prev => ({ ...prev, groupName: e.target.value }))}
                placeholder="Enter group name..."
                className={`w-full pl-10 pr-4 py-3 bg-neutral-700 border rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:outline-none transition-all duration-300 ${
                  errors.groupName ? 'border-red-500 focus:border-red-400' : 'border-neutral-600 focus:border-yellow-600'
                }`}
                maxLength={32}
              />
            </div>
            {errors.groupName && (
              <p className="text-red-400 font-inter text-xs mt-1">{errors.groupName}</p>
            )}
          </div>

          <div>
            <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none mt-2">
              Description *
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              placeholder="Describe your group..."
              rows={3}
              className={`w-full px-4 py-3 bg-neutral-700 border rounded-lg text-neutral-200 placeholder-neutral-400 font-inter focus:outline-none transition-all duration-300 resize-none ${
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
            <label className="block text-neutral-300 font-minecraftia text-sm mb-2 leading-none mt-2">
              Maximum Members
            </label>
            <div className="relative">
              <Users className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-400" />
              <input
                type="number"
                value={formData.maxMembers}
                onChange={(e) => setFormData(prev => ({ ...prev, maxMembers: parseInt(e.target.value) || 2 }))}
                min={2}
                max={50}
                className={`w-full pl-10 pr-4 py-3 bg-neutral-700 border rounded-lg text-neutral-200 font-inter focus:outline-none transition-all duration-300 ${
                  errors.maxMembers ? 'border-red-500 focus:border-red-400' : 'border-neutral-600 focus:border-yellow-600'
                }`}
              />
            </div>
            {errors.maxMembers && (
              <p className="text-red-400 font-inter text-xs mt-1">{errors.maxMembers}</p>
            )}
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
                  <svg className="w-3 h-3 text-neutral-900" fill="currentColor" viewBox="0 0 20 20">
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
                  <span className="text-neutral-200 font-minecraftia text-sm leading-none mt-1">
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

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-300 hover:bg-neutral-600 transition-all duration-300 font-inter"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-3 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-neutral-900 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed font-inter flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="h-4 w-4 border border-neutral-900 border-t-transparent rounded-full animate-spin" />
                  Creating...
                </>
              ) : (
                'Create Group'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateGroupModal;
