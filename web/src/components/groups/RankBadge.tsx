import React from 'react';

interface RankBadgeProps {
  rank: string;
  className?: string;
}

const RankBadge: React.FC<RankBadgeProps> = ({ rank, className = '' }) => {
  const parseMinecraftColors = (text: string) => {
    const colorMap: Record<string, string> = {
      '§0': '#000000', // Black
      '§1': '#0000AA', // Dark Blue
      '§2': '#00AA00', // Dark Green
      '§3': '#00AAAA', // Dark Aqua
      '§4': '#AA0000', // Dark Red
      '§5': '#AA00AA', // Dark Purple
      '§6': '#FFAA00', // Gold
      '§7': '#AAAAAA', // Gray
      '§8': '#555555', // Dark Gray
      '§9': '#5555FF', // Blue
      '§a': '#55FF55', // Green
      '§b': '#55FFFF', // Aqua
      '§c': '#FF5555', // Red
      '§d': '#FF55FF', // Light Purple
      '§e': '#FFFF55', // Yellow
      '§f': '#FFFFFF', // White
      '§k': '', // Obfuscated (skip)
      '§l': '', // Bold
      '§m': '', // Strikethrough
      '§n': '', // Underline
      '§o': '', // Italic
      '§r': '', // Reset
    };

    let cleanText = text.replace(/§[0-9a-fk-or]/g, '');
    
    cleanText = cleanText.replace(/^\[|\]$/g, '');
    
    return cleanText.trim();
  };

  const getRankColor = (rankText: string) => {
    const lower = rankText.toLowerCase();
    
    if (lower.includes('owner')) return 'bg-red-600 text-white';
    if (lower.includes('admin')) return 'bg-red-500 text-white';
    if (lower.includes('mod')) return 'bg-blue-500 text-white';
    if (lower.includes('vip') || lower.includes('mvp')) return 'bg-yellow-500 text-black';
    if (lower.includes('member')) return 'bg-gray-500 text-white';
    if (lower.includes('staff')) return 'bg-purple-500 text-white';
    if (lower.includes('helper')) return 'bg-green-500 text-white';
    if (lower.includes('builder')) return 'bg-orange-500 text-white';
    
    return 'bg-gray-600 text-white';
  };

  if (!rank || rank.trim() === '') {
    return null;
  }

  const cleanRank = parseMinecraftColors(rank);
  const colorClass = getRankColor(cleanRank);

  if (!cleanRank) {
    return null;
  }

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-bold ${colorClass} ${className}`}>
      {cleanRank}
    </span>
  );
};

export default RankBadge;
