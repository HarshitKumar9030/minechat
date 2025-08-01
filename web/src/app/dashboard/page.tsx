'use client';
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { MoveRight, MoveLeft } from 'lucide-react';
import { SERVER_NAME } from '@/lib/constants';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { MinechatAPI, Player, FriendInfo } from '@/lib/api';

export interface NavigationCard {
    id: string;
    title: string;
    description: string;
    image: string;
    href: string;
}

const navigationCards = [
  {
    id: 'dashboard',
    title: 'Dashboard',
    description: 'View server statistics and your activity',
    image: '/dashboard/sword.png',
    href: '/dashboard'
  },
  {
    id: 'friends',
    title: 'Friends',
    description: 'Manage your friend list and requests',
    image: '/dashboard/misc.png',
    href: '/friends'
  },
  {
    id: 'messages',
    title: 'Messages',
    description: 'Chat with friends and groups',
    image: '/dashboard/goat.png',
    href: '/messages'
  },
  {
    id: 'settings',
    title: 'Settings',
    description: 'Configure your preferences',
    image: '/dashboard/creeper.png',
    href: '/settings'
  }
];

const DashboardPage = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [user, setUser] = useState<{ playerUUID: string; playerName: string; rank?: string; } | null>(null);
  const [userDetails, setUserDetails] = useState<Player | null>(null);
  const [recentFriends, setRecentFriends] = useState<FriendInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const router = useRouter();
  const api = useMemo(() => new MinechatAPI(), []);

  useEffect(() => {
    const savedUser = localStorage.getItem('minechat_user');
    
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        

        setUser({ 
          playerUUID: userData.playerUUID, 
          playerName: userData.playerName,
          rank: userData.rank // Keep for fallback only
        });
      } catch {
        router.push('/');
      }
    } else {
      router.push('/');
    }
  }, [router]);

  const loadUserData = useCallback(async () => {
    if (!user) return;
    
    setLoading(true);
    try {
      try {
        const detailsResponse = await api.getUserDetails(user.playerUUID);
        const freshUserData = detailsResponse.user;
        setUserDetails(freshUserData);
        
      } catch {
        setUserDetails({
          playerName: user.playerName,
          playerUUID: user.playerUUID,
          rank: user.rank || '[Player]',
          formattedRank: '§7[Player]',
          online: false,
          lastSeen: undefined,
          firstJoin: undefined,
          webAccessEnabled: false
        });
      }

      try {
        const friendsResponse = await api.getFriends(user.playerUUID);
        setRecentFriends(friendsResponse.friends?.slice(0, 5) || []);
      } catch {
        setRecentFriends([]);
      }
      
    } catch {
    } finally {
      setLoading(false);
    }
  }, [user, api]);

  useEffect(() => {
    if (user) {
      loadUserData();
    }
  }, [user, loadUserData]);

  const nextSlide = () => {
    setCurrentIndex((prev) => (prev + 1) % navigationCards.length);
  };

  const prevSlide = () => {
    setCurrentIndex((prev) => (prev - 1 + navigationCards.length) % navigationCards.length);
  };

  const handleCardClick = (href: string) => {
    router.push(href);
  };

  return (
    <main className='min-h-screen w-full flex flex-col gap-8 justify-center items-center p-4'>
      <div className="text-center mb-6">
        <h1 className="text-2xl md:text-3xl font-minecraftia text-neutral-300 mb-4 leading-none">
          Welcome back, <span className='text-yellow-600'>{user?.playerName || 'Player'}</span>
        </h1>
        <div className="flex items-center justify-center gap-2 text-neutral-500 font-minecraftia text-sm">
          {userDetails?.rank && (
            <>
              <span className=" text-xs">
                <span className=''>{userDetails.rank}</span>
              </span>
              <span>•</span>
            </>
          )}
          <div className="flex items-center gap-1.5">
            <div className={`w-2 h-2 mb-2.5 rounded-full ${userDetails?.online ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <span>{userDetails?.online ? ' Online' : 'Offline'}</span>
          </div>
        </div>
      </div>

      <div className="text-center">
        <h2 className="text-xl md:text-2xl font-minecraftia text-neutral-300 mb-2 leading-none">
          Browse <span className='text-yellow-600'>{SERVER_NAME}</span>
        </h2>
        <p className="text-neutral-500 font-minecraftia text-sm leading-none">
          Choose a section to explore
        </p>
      </div>

      <div className="hidden md:flex items-center gap-6 w-full max-w-6xl">
        <button
          onClick={prevSlide}
          className="p-4 rounded-full bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-colors duration-300"
          aria-label="Previous"
        >
          <MoveLeft className='h-5 w-5 text-neutral-400' />
        </button>

        <div className="flex-1 relative overflow-hidden py-4">
          <div 
            className="flex transition-transform duration-500 ease-out"
            style={{ transform: `translateX(-${currentIndex * 33.333}%)` }}
          >
            {navigationCards.map((card, index) => {
              const image = card.image;
              const isActive = index === currentIndex;
              
              return (
                <div
                  key={card.id}
                  className={`w-1/3 flex-shrink-0 px-2 transition-all duration-500 ${
                    isActive ? 'scale-100 opacity-100' : 'scale-95 opacity-60'
                  }`}
                >
                  <div
                    className="h-80 rounded-xl bg-neutral-800 border border-neutral-700 hover:border-neutral-600 cursor-pointer transition-all duration-300 hover:scale-105 flex flex-col items-center justify-center p-8 text-center group overflow-hidden"
                    onClick={() => handleCardClick(card.href)}
                  >
                    <Image src={image} alt={card.title} width={600} height={600} className="h-16 w-16 text-yellow-500 mb-6 group-hover:scale-110 transition-transform duration-300" />

                    <h3 className="text-xl font-minecraftia text-neutral-200 mb-3 leading-none">
                      {card.title}
                    </h3>
                    <p className="text-neutral-400 font-minecraftia text-sm leading-relaxed">
                      {card.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <button
          onClick={nextSlide}
          className="p-4 rounded-full bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-colors duration-300"
          aria-label="Next"
        >
          <MoveRight className='h-5 w-5 text-neutral-400' />
        </button>
      </div>

      <div className="md:hidden w-full max-w-sm">
        <div className="relative overflow-hidden h-48">
          <div 
            className="flex transition-transform duration-500 ease-out"
            style={{ transform: `translateX(-${currentIndex * 100}%)` }}
          >
            {navigationCards.map((card) => {
              const image = card.image;

              return (
                <div
                  key={card.id}
                  className="w-full flex-shrink-0"
                >
                  <div
                    className="h-48 rounded-xl bg-neutral-800 border border-neutral-700 hover:border-neutral-600 cursor-pointer transition-all duration-300 hover:scale-105 flex flex-col items-center justify-center p-4 text-center group"
                    onClick={() => handleCardClick(card.href)}
                  >
                    <Image src={image} alt={card.title} width={600} height={600} className="h-12 w-12 text-yellow-500 mb-4 group-hover:scale-110 transition-transform duration-300" />
                    <h3 className="text-base font-minecraftia text-neutral-200 mb-1 leading-none">
                      {card.title}
                    </h3>
                    <p className="text-neutral-400 font-minecraftia text-xs leading-relaxed">
                      {card.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
        
        <div className="flex justify-center mt-4">
          <button
            onClick={nextSlide}
            className="p-3 rounded-full bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-colors duration-300"
            aria-label="Next Section"
          >
            <MoveRight className="h-5 w-5 text-neutral-400" />
          </button>
        </div>
      </div>

      {!loading && recentFriends.length > 0 && (
        <div className="w-full max-w-4xl">
          <h3 className="text-lg font-minecraftia text-neutral-300 mb-4 text-center">
            Recent Friends
          </h3>
          <div className="flex justify-center gap-4 overflow-x-auto pb-2">
            {recentFriends.map((friend) => (
              <div
                key={friend.friendUUID}
                className="flex-shrink-0 p-3 bg-neutral-800 border border-neutral-700 rounded-lg text-center min-w-[100px]"
              >
                <div className="relative inline-block mb-2">
                  <Image
                    src={`https://crafatar.com/avatars/${friend.friendUUID}?size=32&overlay`}
                    alt={friend.friendName}
                    width={32}
                    height={32}
                    className="rounded-lg"
                    onError={(e) => {
                      e.currentTarget.src = 'https://crafatar.com/avatars/5e9b103b-dfc2-4b59-be29-eb7523248b5d?size=32&overlay';
                    }}
                  />
                  <div className={`absolute -bottom-0.5 -right-0.5 w-2 h-2 rounded-full border border-neutral-800 ${
                    friend.online ? 'bg-green-500' : 'bg-neutral-600'
                  }`} />
                </div>
                <p className="text-neutral-200 font-minecraftia text-xs truncate">
                  {friend.friendName}
                </p>
                <p className="text-neutral-500 font-minecraftia text-xs mt-1">
                  {friend.online ? 'Online' : 'Offline'}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </main>
  );
};

export default DashboardPage;