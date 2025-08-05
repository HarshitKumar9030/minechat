import React, { useState } from 'react';
import { Megaphone, Plus, Edit, Trash2, Save, X } from 'lucide-react';
import { GroupInfo, moderationApi } from '@/lib/api';

interface GroupAnnouncementsProps {
  group: GroupInfo;
  userRole: string;
  onUpdate?: () => void;
}

const GroupAnnouncements: React.FC<GroupAnnouncementsProps> = ({
  group,
  userRole,
  onUpdate
}) => {
  const [announcements, setAnnouncements] = useState<string[]>(group.announcements || []);
  const [newAnnouncement, setNewAnnouncement] = useState('');
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editingText, setEditingText] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [processing, setProcessing] = useState<number | null>(null);

  const canManage = userRole === 'OWNER' || userRole === 'ADMIN';
  const maxAnnouncements = 5;
  const maxLength = 200;

  const handleAddAnnouncement = async () => {
    if (!newAnnouncement.trim() || announcements.length >= maxAnnouncements) return;

    setProcessing(-1);
    try {
      await moderationApi.addAnnouncement(group.groupId, newAnnouncement.trim());
      setAnnouncements([...announcements, newAnnouncement.trim()]);
      setNewAnnouncement('');
      setShowAddForm(false);
      onUpdate?.();
    } catch (error) {
      console.error('Failed to add announcement:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleEditAnnouncement = async (index: number) => {
    if (!editingText.trim()) return;

    setProcessing(index);
    try {
      await moderationApi.updateAnnouncement(group.groupId, index, editingText.trim());
      const updated = [...announcements];
      updated[index] = editingText.trim();
      setAnnouncements(updated);
      setEditingIndex(null);
      setEditingText('');
      onUpdate?.();
    } catch (error) {
      console.error('Failed to update announcement:', error);
    } finally {
      setProcessing(null);
    }
  };

  const handleDeleteAnnouncement = async (index: number) => {
    setProcessing(index);
    try {
      await moderationApi.removeAnnouncement(group.groupId, index);
      const updated = announcements.filter((_, i) => i !== index);
      setAnnouncements(updated);
      onUpdate?.();
    } catch (error) {
      console.error('Failed to remove announcement:', error);
    } finally {
      setProcessing(null);
    }
  };

  const startEdit = (index: number) => {
    setEditingIndex(index);
    setEditingText(announcements[index]);
  };

  const cancelEdit = () => {
    setEditingIndex(null);
    setEditingText('');
  };

  if (!canManage && announcements.length === 0) {
    return null;
  }

  return (
    <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-yellow-600 rounded-lg flex items-center justify-center">
            <Megaphone className="w-5 h-5 text-black" />
          </div>
          <div>
            <h3 className="font-minecraftia text-lg text-stone-100">
              Group Announcements
            </h3>
            <p className="text-sm text-stone-400">
              Important updates and information
            </p>
          </div>
        </div>
        {canManage && (
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            disabled={announcements.length >= maxAnnouncements}
            className="px-3 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-stone-600 disabled:cursor-not-allowed text-black rounded-lg transition-colors text-sm"
          >
            <Plus className="w-4 h-4" />
          </button>
        )}
      </div>

      <div className="space-y-4">
        {showAddForm && canManage && (
          <div className="border border-yellow-600/30 bg-yellow-900/20 rounded-lg p-4">
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-neutral-300 mb-2">
                  New Announcement ({newAnnouncement.length}/{maxLength})
                </label>
                <textarea
                  value={newAnnouncement}
                  onChange={(e) => setNewAnnouncement(e.target.value.slice(0, maxLength))}
                  className="w-full px-3 py-2 bg-stone-700 border border-stone-600 rounded-lg text-stone-200 placeholder-stone-400 focus:outline-none focus:border-yellow-600 resize-none"
                  rows={3}
                  placeholder="Enter your announcement..."
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  onClick={() => {
                    setShowAddForm(false);
                    setNewAnnouncement('');
                  }}
                  className="px-3 py-2 text-stone-400 hover:text-stone-200 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleAddAnnouncement}
                  disabled={!newAnnouncement.trim() || processing === -1}
                  className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-stone-600 text-black rounded-lg transition-colors font-medium"
                >
                  {processing === -1 ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  ) : (
                    'Add'
                  )}
                </button>
              </div>
            </div>
          </div>
        )}

        {announcements.length === 0 ? (
          <div className="text-center py-8">
            <Megaphone className="w-12 h-12 text-stone-600 mx-auto mb-3" />
            <p className="text-stone-400">No announcements yet</p>
            {canManage && (
              <p className="text-sm text-stone-500 mt-1">
                Add important updates for group members
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {announcements.map((announcement, index) => (
              <div
                key={`announcement-${index}`}
                className="bg-stone-700 border border-stone-600 rounded-lg p-4"
              >
                {editingIndex === index ? (
                  <div className="space-y-3">
                    <textarea
                      value={editingText}
                      onChange={(e) => setEditingText(e.target.value.slice(0, maxLength))}
                      className="w-full px-3 py-2 bg-stone-800 border border-stone-600 rounded-lg text-stone-200 placeholder-stone-400 focus:outline-none focus:border-yellow-600 resize-none"
                      rows={3}
                    />
                    <div className="flex justify-between items-center">
                      <span className="text-xs text-stone-500">
                        {editingText.length}/{maxLength} characters
                      </span>
                      <div className="flex gap-2">
                        <button
                          onClick={cancelEdit}
                          className="p-2 text-stone-400 hover:text-stone-200 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleEditAnnouncement(index)}
                          disabled={!editingText.trim() || processing === index}
                          className="p-2 text-green-400 hover:text-green-300 disabled:text-stone-600 transition-colors"
                        >
                          {processing === index ? (
                            <div className="w-4 h-4 border-2 border-green-400 border-t-transparent rounded-full animate-spin"></div>
                          ) : (
                            <Save className="w-4 h-4" />
                          )}
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="flex justify-between items-start">
                    <p className="text-stone-200 leading-relaxed flex-1 mr-4">
                      {announcement}
                    </p>
                    {canManage && (
                      <div className="flex gap-1">
                        <button
                          onClick={() => startEdit(index)}
                          className="p-2 text-stone-400 hover:text-yellow-400 transition-colors"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteAnnouncement(index)}
                          disabled={processing === index}
                          className="p-2 text-stone-400 hover:text-red-400 disabled:text-stone-600 transition-colors"
                        >
                          {processing === index ? (
                            <div className="w-4 h-4 border-2 border-red-400 border-t-transparent rounded-full animate-spin"></div>
                          ) : (
                            <Trash2 className="w-4 h-4" />
                          )}
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {canManage && announcements.length >= maxAnnouncements && (
          <div className="text-center py-2">
            <p className="text-xs text-stone-500">
              Maximum of {maxAnnouncements} announcements reached
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default GroupAnnouncements;
