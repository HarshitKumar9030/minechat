'use client';
import React, { useState, useEffect } from 'react';
import { User, Settings, LayoutDashboard, LogIn, Menu, X, Users } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import LoginModal from '../modals/LoginModal';
const NavBar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [user, setUser] = useState<{ username: string } | null>(null);
    const router = useRouter();

    const toggleNav = () => setIsOpen((v) => !v);
    
    const handleLoginClick = () => {
        setShowLoginModal(true);
    };

    const handleLogout = () => {
        setIsLoggedIn(false);
        setUser(null);
        localStorage.removeItem('minechat_user');
        localStorage.removeItem('minechat_auth'); 
    };

    const handleProfileClick = () => {
        router.push('/');
    };

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
                localStorage.setItem('minechat_auth', JSON.stringify({
                    username: credentials.username,
                    password: credentials.password
                }));
            } else {
                throw new Error(data.error || 'Authentication failed');
            }
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    };

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

    return (
        <>
            <nav className="fixed z-50 top-2 sm:top-4 left-2 sm:left-4 flex flex-col items-start">
                <button
                    onClick={toggleNav}
                    className="w-10 h-10 sm:w-12 sm:h-12 rounded-full bg-neutral-800 border border-neutral-700 flex items-center justify-center hover:bg-neutral-700 transition-colors"
                    aria-label={isOpen ? 'Close navigation' : 'Open navigation'}
                >
                    {isOpen ? (
                        <X size={18} className="text-neutral-400 sm:w-5 sm:h-5" />
                    ) : (
                        <Menu size={18} className="text-neutral-400 sm:w-5 sm:h-5" />
                    )}
                </button>
                {isOpen && (
                    <div className="mt-2 flex flex-col gap-1.5 sm:gap-2 bg-neutral-900/95 backdrop-blur-sm border border-neutral-800 rounded-xl sm:rounded-2xl p-2 shadow-xl min-w-[160px] sm:min-w-[180px] max-w-[calc(100vw-1rem)] sm:max-w-[90vw]">
                        {isLoggedIn ? (
                            <>
                                <button
                                    onClick={handleProfileClick}
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-xs sm:text-sm"
                                    title="Profile"
                                >
                                    <User size={14} className="text-yellow-500 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5 truncate'>{user?.username || 'Profile'}</span>
                                </button>
                                <Link
                                    href="/dashboard"
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-xs sm:text-sm"
                                    title="Dashboard"
                                >
                                    <LayoutDashboard size={14} className="text-yellow-500 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5'>Dashboard</span>
                                </Link>
                                <Link
                                    href="/friends"
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-xs sm:text-sm"
                                    title="Friends"
                                >
                                    <User size={14} className="text-yellow-500 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5'>Friends</span>
                                </Link>
                                <Link
                                    href="/groups"
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-xs sm:text-sm"
                                    title="Groups"
                                >
                                    <Users size={14} className="text-yellow-500 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5'>Groups</span>
                                </Link>
                                <Link
                                    href="/settings"
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-xs sm:text-sm"
                                    title="Settings"
                                >
                                    <Settings size={14} className="text-yellow-500 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5'>Settings</span>
                                </Link>
                                <button
                                    onClick={handleLogout}
                                    className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-red-600 hover:bg-red-500 transition-colors text-white font-minecraftia text-xs sm:text-sm"
                                    title="Logout"
                                >
                                    <LogIn size={14} className="rotate-180 sm:w-4 sm:h-4" />
                                    <span className='!mt-2 sm:!mt-2.5'>Logout</span>
                                </button>
                            </>
                        ) : (
                            <button
                                onClick={handleLoginClick}
                                className="flex items-center gap-2 px-3 sm:px-4 py-2 rounded-lg bg-yellow-600 hover:bg-yellow-500 transition-colors text-black font-minecraftia text-xs sm:text-sm font-semibold"
                                title="Login"
                            >
                                <LogIn size={14} className="sm:w-4 sm:h-4" />
                                <span className='!mt-2 sm:!mt-2.5'>Login</span>
                            </button>
                        )}
                    </div>
                )}
            </nav>
            
            <LoginModal
                isOpen={showLoginModal}
                onClose={() => setShowLoginModal(false)}
                onLogin={handleLogin}
            />
        </>
    );
};

export default NavBar;