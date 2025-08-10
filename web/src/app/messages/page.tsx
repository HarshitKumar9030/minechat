'use client';
import React, { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { ArrowLeft, Send, Users, MessageSquare, Loader2, Search } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { MinechatAPI, FriendInfo } from '@/lib/api';
import { connectWebSocket, MinechatWebSocket, FriendMessage } from '@/lib/websocket';
import Image from 'next/image';

export default function MessagesPage() {
  const router = useRouter();
  const api = useMemo(() => new MinechatAPI(), []);
  const [user, setUser] = useState<{ playerUUID: string; playerName: string; auth?: { username: string; password: string } } | null>(null);
  const [friends, setFriends] = useState<FriendInfo[]>([]);
  const [selectedFriend, setSelectedFriend] = useState<FriendInfo | null>(null);
  const [ws, setWs] = useState<MinechatWebSocket | null>(null);
  const [loading, setLoading] = useState(true);
  const [connecting, setConnecting] = useState(false);
  const [messages, setMessages] = useState<{ [key: string]: FriendMessage[] }>({});
  const [input, setInput] = useState('');
  const [activeTab, setActiveTab] = useState<'friends' | 'search'>('friends');
  const [online, setOnline] = useState<Array<{ name: string; uuid: string }>>([]);
  const [search, setSearch] = useState('');
  const [searchResults, setSearchResults] = useState<Array<{ 
    name: string; 
    uuid: string; 
    online: boolean; 
    rank?: string; 
    formattedRank?: string; 
    lastSeen?: number;
  }>>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const savedUser = localStorage.getItem('minechat_user');
    const savedAuth = localStorage.getItem('minechat_auth');
    if (!savedUser || !savedAuth) {
      router.push('/');
      return;
    }
    try {
      const userData = JSON.parse(savedUser);
      const auth = JSON.parse(savedAuth);
      setUser({ playerUUID: userData.playerUUID, playerName: userData.playerName, auth });
    } catch {
      router.push('/');
    }
  }, [router]);

  useEffect(() => {
    if (!user) return;
    let mounted = true;
    (async () => {
      try {
        setLoading(true);
        const { friends } = await api.getFriends(user.playerUUID);
        if (!mounted) return;
        setFriends(friends);
        if (friends.length > 0) setSelectedFriend(friends[0]);
      } finally {
        setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [user, api]);

  useEffect(() => {
    const loadHistory = async () => {
      if (!user || !selectedFriend) return;
      try {
        const { messages: history } = await api.getPrivateMessages(user.playerName, selectedFriend.friendName, 100);
  type DbPrivateMessage = { senderUUID?: string; senderName: string; targetUUID?: string; targetName: string; message: string; date?: number; timestamp?: string; source?: 'web' | 'minecraft' };
  const mapped = (history as DbPrivateMessage[]).map((m) => ({
          senderUUID: m.senderUUID || '',
          senderName: m.senderName,
          targetUUID: m.targetUUID || '',
          targetName: m.targetName,
          content: m.message,
          timestamp: typeof m.date === 'number' ? m.date : (m.timestamp ? Date.parse(m.timestamp) : Date.now()),
          source: (m.source === 'web' || m.source === 'minecraft') ? m.source : 'minecraft'
        })) as FriendMessage[];
        setMessages(prev => ({ ...prev, [selectedFriend.friendName]: mapped }));
        requestAnimationFrame(() => listRef.current?.scrollTo({ top: listRef.current.scrollHeight }));
      } catch (e) {
        console.error('Failed to load DM history:', e);
      }
    };
    loadHistory();
  }, [selectedFriend, user, api]);

  useEffect(() => {
    if (!user || !user.auth) return;
    let alive = true;
    let socket: MinechatWebSocket | null = null;
    setConnecting(true);
    connectWebSocket(user.auth.username, user.auth.password)
      .then((sock) => {
        if (!alive) { sock.disconnect(); return; }
        socket = sock;
        setWs(sock);
        sock.onFriendMessage((msg) => {
          setMessages((prev) => {
            const key = msg.senderName === user.playerName ? msg.targetName : msg.senderName;
            const list = prev[key] ? [...prev[key]] : [];
            list.push(msg);
            return { ...prev, [key]: list };
          });
          requestAnimationFrame(() => listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' }));
        });
        sock.onOnlinePlayers((payload) => {
          setOnline(payload.players.map(p => ({ name: p.name, uuid: p.uuid })));
        });
        sock.requestOnlinePlayers();
      })
      .finally(() => setConnecting(false));
    return () => { alive = false; socket?.disconnect(); };
  }, [user]);

  const performSearch = useCallback(async () => {
    if (!search.trim() || !user?.auth) return;
    
    setSearchLoading(true);
    try {
      const { players } = await api.searchPlayers(search, 20);
      
      const searchResults = players
        .filter(p => p.playerName !== user.playerName)
        .map(p => ({
          name: p.playerName,
          uuid: p.playerUUID,
          online: p.online,
          rank: p.rank,
          formattedRank: p.formattedRank,
          lastSeen: p.lastSeen
        }));
      
      setSearchResults(searchResults);
    } catch (error) {
      console.error('Search failed:', error);
      const onlineResults = online
        .filter(p => p.name.toLowerCase().includes(search.toLowerCase()) && p.name !== user.playerName)
        .map(p => ({ ...p, online: true }));
      setSearchResults(onlineResults);
    } finally {
      setSearchLoading(false);
    }
  }, [search, online, user?.playerName, user?.auth, api]);

  useEffect(() => {
    if (activeTab === 'search' && search.trim()) {
      const debounceTimer = setTimeout(performSearch, 300);
      return () => clearTimeout(debounceTimer);
    } else {
      setSearchResults([]);
    }
  }, [search, activeTab, performSearch]);

  const handleSend = async () => {
    if (!user || !selectedFriend || !input.trim()) return;
    const content = input.trim();

    if (ws && ws.isAuth()) {
      try {
        ws.sendFriendMessage(selectedFriend.friendName, content);
        setMessages((prev) => {
          const key = selectedFriend.friendName;
          const list = prev[key] ? [...prev[key]] : [];
          list.push({
            senderUUID: user.playerUUID,
            senderName: user.playerName,
            targetUUID: selectedFriend.friendUUID,
            targetName: selectedFriend.friendName,
            content,
            timestamp: Date.now(),
            source: 'web'
          });
          return { ...prev, [key]: list };
        });
        setInput('');
        requestAnimationFrame(() => listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' }));
      } catch (error) {
        console.error('Failed to send message:', error);
        // Show user-friendly error
        alert('Failed to send message. Please check your connection and try again.');
      }
    } else {
      // Show connection error if WebSocket is not available
      alert('Not connected to chat server. Please refresh the page and try again.');
    }
  };

  const handleDirect = (name: string) => {
    if (!user || !ws || !input.trim()) return;
    
    if (!ws.isAuth()) {
      alert('Not connected to chat server. Please refresh the page and try again.');
      return;
    }

    try {
      const content = input.trim();
      ws.sendDirectMessage(name, content);
      setMessages((prev) => {
        const key = name;
        const list = prev[key] ? [...prev[key]] : [];
        list.push({
          senderUUID: user.playerUUID,
          senderName: user.playerName,
          targetUUID: '',
          targetName: name,
          content,
          timestamp: Date.now(),
          source: 'web'
        });
        return { ...prev, [key]: list };
      });
      if (!selectedFriend) setSelectedFriend({ friendName: name, friendUUID: '', timestamp: Date.now(), online: true });
      setInput('');
      requestAnimationFrame(() => listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' }));
    } catch (error) {
      console.error('Failed to send direct message:', error);
      alert('Failed to send message. Please try again.');
    }
  };

  if (!user) return null;

  return (
    <main className="min-h-screen bg-neutral-950">
      <div className="flex items-center gap-3 p-4 border-b border-neutral-800 md:hidden bg-neutral-900">
        <button
          onClick={() => router.push('/dashboard')}
          className="p-2 rounded-lg bg-neutral-800 border border-neutral-700 hover:bg-neutral-700 transition-all duration-200"
          aria-label="Back to Dashboard"
        >
          <ArrowLeft className="h-4 w-4 text-neutral-300" />
        </button>
        <h1 className="text-lg font-inter font-bold text-neutral-100">Messages</h1>
      </div>

      <div className="flex flex-col md:flex-row h-[calc(100vh-73px)] md:h-screen">
        <aside className="w-full md:w-80 lg:w-96 flex-shrink-0 bg-neutral-900 border-r border-neutral-800 flex flex-col">
          <div className="hidden md:flex items-center justify-between p-4 border-b border-neutral-800">
            <div className="flex items-center gap-3">
              <Users className="h-5 w-5 text-yellow-500" />
              <h2 className="text-neutral-100 font-inter font-bold text-lg">Messages</h2>
            </div>
            <button
              onClick={() => router.push('/friends')}
              className="px-3 py-1.5 text-xs font-minecraftia rounded-lg bg-yellow-600 hover:bg-yellow-700 text-neutral-900 transition-all duration-200"
            >
              Manage
            </button>
          </div>

          <div className="md:hidden flex justify-end p-4 pb-2">
            <button
              onClick={() => router.push('/friends')}
              className="px-3 py-1.5 text-xs font-minecraftia rounded-lg bg-yellow-600 hover:bg-yellow-700 text-neutral-900 transition-all duration-200"
            >
              Manage
            </button>
          </div>

          <div className="flex-1 flex flex-col p-4 pt-2 md:pt-4">

          <div className="flex gap-1 mb-4 p-1 bg-neutral-800 rounded-lg">
            <button 
              onClick={() => setActiveTab('friends')} 
              className={`flex-1 px-3 py-2 rounded-md text-sm font-inter font-semibold tracking-wide transition-all duration-200 ${
                activeTab === 'friends' 
                  ? 'bg-yellow-600 text-neutral-900' 
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-700'
              }`}
            >
              Friends
            </button>
            <button 
              onClick={() => setActiveTab('search')} 
              className={`flex-1 px-3 py-2 rounded-md text-sm font-inter font-semibold tracking-wide transition-all duration-200 ${
                activeTab === 'search' 
                  ? 'bg-yellow-600 text-neutral-900' 
                  : 'text-neutral-400 hover:text-neutral-200 hover:bg-neutral-700'
              }`}
            >
              Search Players
            </button>
          </div>

          {activeTab === 'search' && (
            <div className="mb-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-neutral-500" />
                <input 
                  value={search} 
                  onChange={(e) => setSearch(e.target.value)} 
                  placeholder="Search players by username..." 
                  className="w-full pl-10 pr-3 py-2.5 bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-200 placeholder-neutral-500 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 transition-all duration-200"
                  onKeyDown={(e) => { if (e.key === 'Enter') performSearch(); }}
                />
              </div>
            </div>
          )}

          <div className="flex-1 overflow-hidden">
            {loading && activeTab === 'friends' ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="animate-spin h-6 w-6 text-neutral-400" />
              </div>
            ) : activeTab === 'friends' ? (
              <div className="space-y-2 h-full overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-neutral-700 scrollbar-track-neutral-800">
                {friends.map((f) => (
                  <button
                    key={f.friendUUID}
                    onClick={() => setSelectedFriend(f)}
                    className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all duration-200 ${
                      selectedFriend?.friendUUID === f.friendUUID 
                        ? 'bg-yellow-600/20 border-yellow-600/60 shadow-lg' 
                        : 'bg-neutral-800 border-neutral-700 hover:bg-neutral-750 hover:border-neutral-600'
                    }`}
                  >
                    <div className="relative flex-shrink-0">
                      <Image 
                        src={`https://minotar.net/avatar/${f.friendName}/32`} 
                        alt={f.friendName} 
                        width={32} 
                        height={32} 
                        className="rounded-md" 
                      />
                      <div className={`absolute -bottom-1 -right-1 w-3 h-3 rounded-full border-2 border-neutral-800 ${
                        f.online ? 'bg-green-500' : 'bg-neutral-600'
                      }`} />
                    </div>
                    <div className="text-left min-w-0 flex-1">
                      <div className="text-neutral-100 font-minecraftia text-sm pt-1 truncate">{f.friendName}</div>
                      <div className={`text-xs ${f.online ? 'text-green-400' : 'text-neutral-500'}`}>
                        {f.online ? 'Online' : 'Offline'}
                      </div>
                    </div>
                  </button>
                ))}
                {friends.length === 0 && (
                  <div className="text-center py-12">
                    <div className="w-16 h-16 bg-neutral-800 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Users className="h-8 w-8 text-neutral-600" />
                    </div>
                    <p className="text-neutral-500 text-sm mb-3">No friends yet</p>
                    <button
                      onClick={() => router.push('/friends')}
                      className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 rounded-lg font-inter text-sm transition-all duration-200"
                    >
                      Add Friends
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-2 h-full overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-neutral-700 scrollbar-track-neutral-800">
                {searchLoading ? (
                  <div className="flex items-center justify-center py-12">
                    <Loader2 className="animate-spin h-6 w-6 text-neutral-400" />
                  </div>
                ) : searchResults.length > 0 ? (
                  searchResults.map((p) => (
                    <button
                      key={p.uuid}
                      onClick={() => setSelectedFriend({ friendName: p.name, friendUUID: p.uuid, timestamp: Date.now(), online: p.online })}
                      className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all duration-200 ${
                        selectedFriend?.friendName === p.name 
                          ? 'bg-yellow-600/20 border-yellow-600/60 shadow-lg' 
                          : 'bg-neutral-800 border-neutral-700 hover:bg-neutral-750 hover:border-neutral-600'
                      }`}
                    >
                      <div className="relative flex-shrink-0">
                        <Image 
                          src={`https://minotar.net/avatar/${p.name}/32`} 
                          alt={p.name} 
                          width={32} 
                          height={32} 
                          className="rounded-md" 
                        />
                        {p.online && <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-neutral-800" />}
                      </div>
                      <div className="text-left min-w-0 flex-1">
                        <div className="text-neutral-100 font-minecraftia text-sm truncate">
                          {p.name}
                          {p.rank && p.rank !== '[Player]' && (
                            <span className="ml-2 text-xs text-yellow-400">{p.rank}</span>
                          )}
                        </div>
                        <div className={`text-xs ${p.online ? 'text-green-400' : 'text-neutral-500'}`}>
                          {p.online ? 'Online' : (
                            p.lastSeen ? 
                              `Last seen ${new Date(p.lastSeen).toLocaleDateString()}` : 
                              'Offline'
                          )}
                        </div>
                      </div>
                      {input.trim().length > 0 && (
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); handleDirect(p.name); }}
                          className="ml-2 px-3 py-1 text-xs font-minecraftia rounded-md bg-yellow-600 text-neutral-900 hover:bg-yellow-700 transition-all duration-200"
                        >
                          Send
                        </button>
                      )}
                    </button>
                  ))
                ) : search.trim() ? (
                  <div className="text-center py-12">
                    <div className="w-16 h-16 bg-neutral-800 rounded-full flex items-center justify-center mx-auto mb-4">
                      <MessageSquare className="h-8 w-8 text-neutral-600" />
                    </div>
                    <p className="text-neutral-500 text-sm">No players found for &ldquo;{search}&rdquo;</p>
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <div className="w-16 h-16 bg-neutral-800 rounded-full flex items-center justify-center mx-auto mb-4">
                      <Search className="h-8 w-8 text-neutral-600" />
                    </div>
                    <p className="text-neutral-500 text-sm">Search for a player to start messaging</p>
                  </div>
                )}
              </div>
            )}
          </div>
          </div>
        </aside>

        <section className="flex-1 bg-neutral-950 flex flex-col">
          <div className="flex items-center justify-between p-4 border-b border-neutral-800 bg-neutral-900">
            <div className="flex items-center gap-3 min-w-0 flex-1">
              <MessageSquare className="h-5 w-5 text-yellow-500 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <div className="text-neutral-100 pt-1 font-minecraftia text-sm md:text-base truncate">
                  {selectedFriend?.friendName || 'Select a chat'}
                </div>
                <div className="text-xs text-neutral-500">
                  {connecting ? 'Connecting…' : ws?.isAuth() ? 'Connected' : 'Not connected'}
                </div>
              </div>
            </div>
            {selectedFriend && (
              <div className="flex items-center gap-2 flex-shrink-0">
                <div className={`w-2 h-2 rounded-full ${selectedFriend.online ? 'bg-green-500' : 'bg-neutral-600'}`} />
                <span className="text-xs text-neutral-500 hidden sm:inline">
                  {selectedFriend.online ? 'Online' : 'Offline'}
                </span>
              </div>
            )}
          </div>

          <div ref={listRef} className="flex-1 overflow-y-auto p-3 md:p-4 space-y-3 scrollbar-thin scrollbar-thumb-neutral-700 scrollbar-track-neutral-800">
            {(selectedFriend && messages[selectedFriend.friendName]) ? (
              messages[selectedFriend.friendName].map((m, idx) => {
                const mine = m.senderName === user?.playerName || m.senderUUID === user?.playerUUID;
                return (
                  <div key={idx} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[85%] sm:max-w-[70%] px-3 md:px-4 py-2.5 md:py-3 rounded-lg ${
                      mine 
                        ? 'bg-yellow-600 text-neutral-900' 
                        : 'bg-neutral-800 border border-neutral-700 text-neutral-100'
                    }`}>
                      <div className={`text-xs mb-1 ${mine ? 'text-neutral-800' : 'text-neutral-400'}`}>
                        {mine ? 'You' : m.senderName} • {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div className="whitespace-pre-wrap break-words text-sm">{m.content}</div>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-neutral-500 px-4">
                <div className="w-16 md:w-20 h-16 md:h-20 bg-neutral-800 rounded-full flex items-center justify-center mb-4">
                  <MessageSquare className="h-8 md:h-10 w-8 md:w-10 text-neutral-600" />
                </div>
                <p className="text-base md:text-lg font-minecraftia mb-2 text-center text-neutral-400">No conversation selected</p>
                <p className="text-sm text-center text-neutral-500 max-w-sm">
                  {activeTab === 'friends' 
                    ? 'Choose a friend from the sidebar to start chatting' 
                    : 'Search for any player to start a conversation'
                  }
                </p>
              </div>
            )}
          </div>

          <div className="p-3 md:p-4 border-t border-neutral-800 bg-neutral-900">
            <div className="flex items-center gap-2 md:gap-3">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
                placeholder={
                  selectedFriend 
                    ? `Message ${selectedFriend.friendName}…` 
                    : activeTab === 'search' 
                      ? 'Search for a player, then type a message' 
                      : 'Select a friend to start chatting'
                }
                disabled={!selectedFriend}
                className="flex-1 px-3 md:px-4 py-2.5 md:py-3 bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-200 placeholder-neutral-500 text-sm md:text-base focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
              />
              <button
                onClick={handleSend}
                disabled={!selectedFriend || !input.trim()}
                className="p-2.5 md:p-3 rounded-lg bg-yellow-600 text-neutral-900 hover:bg-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 flex-shrink-0"
                aria-label="Send message"
              >
                <Send className="h-4 md:h-5 w-4 md:w-5" />
              </button>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
