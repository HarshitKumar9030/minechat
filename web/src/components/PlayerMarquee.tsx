'use client';
import { useEffect, useState } from 'react';
import { api, getPlayerHead, formatMinecraftRank, getRankColor } from '@/lib/api';
import Image from 'next/image';

interface Player {
  playerName: string;
  playerUUID: string;
  rank: string;
  formattedRank: string;
  online: boolean;
}

export default function PlayerMarquee() {
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOnlinePlayers = async () => {
      try {
        const response = await api.getOnlinePlayers();
        setPlayers(response.players);
      } catch (error) {
        console.error('Failed to fetch online players:', error);
        setPlayers([
          { playerName: 'Steve', playerUUID: '', rank: '[Player]', formattedRank: '§7[Player]', online: true },
          { playerName: 'Alex', playerUUID: '', rank: '[VIP]', formattedRank: '§6[VIP]', online: true },
          { playerName: 'Admin', playerUUID: '', rank: '[ADMIN]', formattedRank: '§c[ADMIN]', online: true },
        ]);
      } finally {
        setLoading(false);
      }
    };

    fetchOnlinePlayers();
    const interval = setInterval(fetchOnlinePlayers, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="fixed top-8 right-4 py-4">
        <div className="h-16 flex items-center transform -skew-x-12">
          <p className="text-yellow-400 font-minecraftia text-sm">Loading players...</p>
        </div>
      </div>
    );
  }

  if (players.length === 0) {
    return (
      <div className="fixed top-8 right-4 py-4">
        <div className="h-16 flex items-center transform -skew-x-12">
          <p className="text-neutral-400 font-minecraftia text-sm">No players online</p>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed top-8 right-4 py-4">
      <div className="relative overflow-hidden h-16 transform -skew-x-12">
        <div className="flex animate-marquee gap-4 items-center h-full">
          {[...players, ...players].map((player, index) => {
            const displayRank = player.formattedRank || player.rank;
            const cleanRank = formatMinecraftRank(displayRank);
            const rankColor = getRankColor(displayRank);
            
            return (
              <div 
                key={`${player.playerName}-${index}`}
                className="flex items-center gap-2 whitespace-nowrap"
              >
                <div className="relative">
                  <Image
                    src={getPlayerHead(player.playerName, player.playerUUID, 32)}
                    alt={player.playerName}
                    width={28}
                    height={28}
                    className="rounded pixelated"
                    unoptimized
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.src = getPlayerHead('alex', '', 32);
                    }}
                  />
                  <div className="absolute -top-1 -right-1 w-2 h-2 bg-yellow-400 rounded-full border border-yellow-300"></div>
                </div>
                <div className="flex flex-col">
                  <span className="text-white font-minecraftia text-xs">{player.playerName}</span>
                  {cleanRank && cleanRank !== '[Player]' && (
                    <span className={`font-minecraftia text-[10px] leading-none ${rankColor}`}>
                      {cleanRank}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
