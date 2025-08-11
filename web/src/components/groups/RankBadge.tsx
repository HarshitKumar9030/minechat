import React from 'react';

interface RankBadgeProps {
  rank: string;
  className?: string;
}

const MC_COLORS: Record<string, string> = {
  '0': '#000000', // Black
  '1': '#0000AA', // Dark Blue
  '2': '#00AA00', // Dark Green
  '3': '#00AAAA', // Dark Aqua
  '4': '#AA0000', // Dark Red
  '5': '#AA00AA', // Dark Purple
  '6': '#FFAA00', // Gold
  '7': '#AAAAAA', // Gray
  '8': '#555555', // Dark Gray
  '9': '#5555FF', // Blue
  a: '#55FF55', // Green
  b: '#55FFFF', // Aqua
  c: '#FF5555', // Red
  d: '#FF55FF', // Light Purple
  e: '#FFFF55', // Yellow
  f: '#FFFFFF', // White
};

const renderFormattedRank = (text: string) => {
  if (!text) return null;

  const parts: React.ReactNode[] = [];
  let currentColor = '#FFFFFF';
  let bold = false;
  let italic = false;
  let underline = false;
  let strikethrough = false;

  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch === '§' && i + 1 < text.length) {
      const code = text[i + 1].toLowerCase();
      i++; 

      if (code in MC_COLORS) {
        currentColor = MC_COLORS[code];
      } else {
        switch (code) {
          case 'l':
            bold = true; break;
          case 'o':
            italic = true; break;
          case 'n':
            underline = true; break;
          case 'm':
            strikethrough = true; break;
          case 'r':
            currentColor = '#FFFFFF';
            bold = italic = underline = strikethrough = false;
            break;
         
        }
      }
      continue;
    }

   
    const style = {
      color: currentColor,
      fontWeight: bold ? 700 : 600,
      fontStyle: italic ? 'italic' as const : 'normal' as const,
      textDecoration: [underline ? 'underline' : '', strikethrough ? 'line-through' : '']
        .filter(Boolean)
        .join(' ') || 'none',
    };
    parts.push(
      <span key={parts.length} style={style} className="font-inter">
        {ch}
      </span>
    );
  }

  return parts;
};

const RankBadge: React.FC<RankBadgeProps> = ({ rank, className = '' }) => {
  if (!rank || rank.trim() === '') return null;

  return (
    <span
      className={`inline-flex items-center px-1.5 py-0.5 rounded text-xs border border-neutral-700/50 bg-neutral-800/40 ${className}`}
    >
      {renderFormattedRank(rank)}
    </span>
  );
};

export default RankBadge;
