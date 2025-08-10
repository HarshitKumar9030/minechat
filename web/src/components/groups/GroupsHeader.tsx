import React from 'react';
import { Plus, Users, Search } from 'lucide-react';

interface GroupsHeaderProps {
  view: 'my-groups' | 'discover' | 'invites' | 'messages';
  onCreateGroup?: () => void;
  onJoinGroup?: () => void;
  groupCount?: number;
  searchTerm?: string;
  onSearchChange?: (term: string) => void;
}

const GroupsHeader: React.FC<GroupsHeaderProps> = ({
  view,
  onCreateGroup,
  onJoinGroup,
  groupCount = 0,
  searchTerm = '',
  onSearchChange
}) => {
  const getHeaderContent = () => {
    switch (view) {
      case 'my-groups':
        return {
          title: 'My Groups',
          subtitle: `You're a member of ${groupCount} groups`,
          showActions: true
        };
      case 'discover':
        return {
          title: 'Discover Groups',
          subtitle: 'Find and join groups that interest you',
          showActions: false
        };
      case 'invites':
        return {
          title: 'Group Invites',
          subtitle: 'Manage your pending group invitations',
          showActions: false
        };
      case 'messages':
        return {
          title: 'Messages',
          subtitle: 'Chat with your groups in real-time',
          showActions: false
        };
      default:
        return {
          title: 'Groups',
          subtitle: '',
          showActions: false
        };
    }
  };

  const { title, subtitle, showActions } = getHeaderContent();

  return (
    <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 sm:p-6 mb-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3 sm:gap-4">
          <div className="w-10 h-10 sm:w-12 sm:h-12 bg-gradient-to-br from-yellow-600 to-yellow-700 rounded-lg flex items-center justify-center">
            <Users className="w-5 h-5 sm:w-6 sm:h-6 text-neutral-900" />
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-neutral-100 font-minecraftia">
              {title}
            </h1>
            {subtitle && (
              <p className="text-neutral-400 mt-1 text-sm sm:text-base">
                {subtitle}
              </p>
            )}
          </div>
        </div>

        {showActions && (
          <div className="flex flex-col sm:flex-row gap-2 sm:gap-3 w-full sm:w-auto">
            <button
              onClick={onJoinGroup}
              className="px-3 sm:px-4 py-2 bg-neutral-700 hover:bg-neutral-600 text-neutral-300 rounded-lg transition-colors flex items-center justify-center gap-2 text-sm sm:text-base"
            >
              <Search className="w-4 h-4" />
              <span className="whitespace-nowrap">Join Group</span>
            </button>
            <button
              onClick={onCreateGroup}
              className="px-3 sm:px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 rounded-lg transition-colors flex items-center justify-center gap-2 text-sm sm:text-base font-medium"
            >
              <Plus className="w-4 h-4" />
              <span className="whitespace-nowrap">Create Group</span>
            </button>
          </div>
        )}
      </div>

      {/* Search Bar for applicable views */}
      {view === 'my-groups' && onSearchChange && (
        <div className="mt-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 sm:w-5 sm:h-5 text-neutral-400" />
            <input
              type="text"
              placeholder="Search your groups..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full pl-9 sm:pl-10 pr-4 py-2.5 sm:py-3 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400 focus:outline-none focus:border-yellow-600 text-sm sm:text-base"
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default GroupsHeader;
