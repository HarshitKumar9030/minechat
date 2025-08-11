

'use client';
import { SERVER_DESCRIPTION, SERVER_NAME, SERVER_IP, SOCIAL_LINKS, LEGAL_LINKS } from "@/lib/constants";
import Image from "next/image";
import { Copy, ExternalLink, Check, LogIn, User } from "lucide-react";
import PlayerMarquee from "@/components/PlayerMarquee";
import LoginModal from "@/modals/LoginModal";
import { useState, useEffect } from "react";
import { useRouter } from 'next/navigation';

export default function Home() {
  const [copiedStates, setCopiedStates] = useState<{ [key: string]: boolean }>({});
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [user, setUser] = useState<{ username: string } | null>(null);
  const router = useRouter();

  useEffect(() => {
    const savedUser = localStorage.getItem('minechat_user');
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser({ username: userData.playerName });
        setIsLoggedIn(true);
      } catch (error) {
        console.error('Error parsing saved user data:', error);
        localStorage.removeItem('minechat_user');
      }
    }
  }, []);

  const handleLogin = async (credentials: { username: string; password: string }) => {
    try {
      const response = await fetch('http://localhost:8080/api/auth', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
      });

      const data = await response.json();

      if (data.success) {
        setIsLoggedIn(true);
        setUser({ username: data.user.playerName });
        setShowLoginModal(false);
        
        localStorage.setItem('minechat_user', JSON.stringify(data.user));
      } else {
        throw new Error(data.error || 'Authentication failed');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const handleLoginButtonClick = () => {
    if (isLoggedIn) {
      router.push('/dashboard');
    } else {
      setShowLoginModal(true);
    }
  };

  const copyToClipboard = async (text: string, key: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedStates(prev => ({ ...prev, [key]: true }));
      
      // reset after one and half secs
      setTimeout(() => {
        setCopiedStates(prev => ({ ...prev, [key]: false }));
      }, 1500);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground relative">
      <PlayerMarquee />
      
      <main className="min-h-screen flex flex-col items-center justify-center px-4 py-8">
        <div className="logo mb-6">
          <Image 
            src={'/server/logo.png'} 
            alt={SERVER_NAME} 
            width={400} 
            height={400}
            className="drop-shadow-[0_0_25px_rgba(255,215,0,0.8)] hover:drop-shadow-[0_0_35px_rgba(255,215,0,1)] transition-all duration-300"
          />
        </div>
        
        <p className="text-neutral-400 font-minecraftia mb-10 max-w-xl text-center text-sm">
          {SERVER_DESCRIPTION}
        </p>


        <div className="server-ip mb-8 text-center">
          <p className="text-neutral-300 font-minecraftia mb-3">Server IP</p>
          <div className="flex items-center gap-2 bg-neutral-800 rounded-lg px-6 py-3 border border-neutral-700">
            <span className="font-mono text-yellow-400 text-lg">{SERVER_IP}</span>
            <button
              onClick={() => copyToClipboard(SERVER_IP, 'serverip')}
              className={`text-neutral-400 hover:text-yellow-400 transition-all duration-300 transform ${copiedStates.serverip ? 'scale-110 text-green-400' : 'hover:scale-105'}`}
              title={copiedStates.serverip ? "Copied!" : "Copy Server IP"}
            >
              {copiedStates.serverip ? (
                <Check size={18} className="animate-pulse" />
              ) : (
                <Copy size={18} />
              )}
            </button>
          </div>
        </div>

        <div className="relative mb-10 group">
          <button 
            onClick={handleLoginButtonClick}
            className={`relative font-minecraftia px-8 py-3 rounded-lg transition-all duration-500 transform hover:scale-105 overflow-hidden ${
              isLoggedIn 
                ? 'bg-gradient-to-r from-yellow-600 to-yellow-500 hover:from-yellow-500 hover:to-yellow-400 text-neutral-900' 
                : 'bg-gradient-to-r from-yellow-600 to-yellow-500 hover:from-yellow-500 hover:to-yellow-400 text-neutral-900'
            }`}
          >
            <span className="relative z-10 flex items-center gap-2 transition-all duration-600">
              {isLoggedIn ? (
                <>
                  <User size={18} className="animate-pulse" />
                  <span className="leading-none mt-3 font-semibold">You&apos;re logged in, {user?.username}!</span>
                </>
              ) : (
                <>
                  <LogIn size={18} className="text-black" />
                  <span className="leading-none mt-3 font-semibold">Login to Web Chat</span>
                </>
              )}
            </span>
            <div className={`absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700 ease-in-out`}></div>
            {isLoggedIn && (
              <>
                <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/30 to-yellow-600/30 animate-pulse duration-1000"></div>
                <div className="absolute inset-0 border-2 border-yellow-400/50 rounded-lg animate-pulse duration-1500"></div>
              </>
            )}
          </button>
        </div>

        {!isLoggedIn && (
          <div className="setup-info max-w-xl text-center">
            <h3 className="text-neutral-200 font-minecraftia mb-4">First time? Set up web access:</h3>
            <div className="bg-neutral-800 rounded-lg p-4 border border-neutral-700">
              <div className="flex items-center justify-center gap-2 mb-2">
                <code className="text-yellow-400 font-mono">/minechat setpassword &lt;password&gt;</code>
                <button
                  onClick={() => copyToClipboard('/minechat setpassword <password>', 'command')}
                  className={`text-neutral-400 hover:text-yellow-400 transition-all duration-300 transform ${copiedStates.command ? 'scale-110 text-green-400' : 'hover:scale-105'}`}
                  title={copiedStates.command ? "Copied!" : "Copy command"}
                >
                  {copiedStates.command ? (
                    <Check size={16} className="animate-pulse" />
                  ) : (
                    <Copy size={16} />
                  )}
                </button>
              </div>
              <p className="text-neutral-400 font-minecraftia text-sm">Run this command in-game to enable web access</p>
            </div>
          </div>
        )}
      </main>

      <div className="fixed bottom-4 right-4 flex flex-col gap-2">
        {SOCIAL_LINKS.discord && (
          <a 
            href={SOCIAL_LINKS.discord} 
            target="_blank" 
            rel="noopener noreferrer"
            className="bg-indigo-600 hover:bg-indigo-500 text-white p-3 rounded-lg transition-colors shadow-lg"
            title="Discord"
          >
            <ExternalLink size={16} />
          </a>
        )}
        {SOCIAL_LINKS.website && (
          <a 
            href={SOCIAL_LINKS.website} 
            target="_blank" 
            rel="noopener noreferrer"
            className="bg-neutral-700 hover:bg-neutral-600 text-white p-3 rounded-lg transition-colors shadow-lg"
            title="Website"
          >
            <ExternalLink size={16} />
          </a>
        )}
        {SOCIAL_LINKS.github && (
          <a 
            href={SOCIAL_LINKS.github} 
            target="_blank" 
            rel="noopener noreferrer"
            className="bg-gray-800 hover:bg-gray-700 text-white p-3 rounded-lg transition-colors shadow-lg"
            title="GitHub"
          >
            <ExternalLink size={16} />
          </a>
        )}
      </div>

      <div className="fixed bottom-4 left-4 flex flex-col gap-1">
        <a 
          href={LEGAL_LINKS.privacyPolicy} 
          className="text-neutral-400 hover:text-neutral-300 font-minecraftia text-xs transition-colors"
        >
          Privacy Policy
        </a>
        <a 
          href={LEGAL_LINKS.termsOfService} 
          className="text-neutral-400 hover:text-neutral-300 font-minecraftia text-xs transition-colors"
        >
          Terms of Service
        </a>
        <a 
          href={LEGAL_LINKS.rules} 
          className="text-neutral-400 hover:text-neutral-300 font-minecraftia text-xs transition-colors"
        >
          Server Rules
        </a>
        <a 
          href={LEGAL_LINKS.support} 
          className="text-neutral-400 hover:text-neutral-300 font-minecraftia text-xs transition-colors"
        >
          Support
        </a>
      </div>

      
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onLogin={handleLogin}
      />
    </div>
  );
}
