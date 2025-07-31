'use client';
import React, { useState, useEffect } from 'react';
import { User, Settings, LayoutDashboard, LogIn, Menu, X } from 'lucide-react';
import LoginModal from '../modals/LoginModal';
import { redirect } from 'next/navigation';
const NavBar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [user, setUser] = useState<{ username: string } | null>(null);

    const toggleNav = () => setIsOpen((v) => !v);
    
    const handleLoginClick = () => {
        setShowLoginModal(true);
    };

    const handleLogout = () => {
        setIsLoggedIn(false);
        setUser(null);
        localStorage.removeItem('minechat_user');
    };

    const handleProfileClick = () => {
        console.log("tapped")

        window.location.href = '/';
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
            } else {
                throw new Error(data.error || 'Authentication failed');
            }
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    };

    // check for existing login on component mount
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
            <nav className="fixed z-50 top-4 left-4 flex flex-col items-start">
                <button
                    onClick={toggleNav}
                    className="w-12 h-12 rounded-full bg-neutral-800 border border-neutral-700 flex items-center justify-center hover:bg-neutral-700 transition-colors"
                    aria-label={isOpen ? 'Close navigation' : 'Open navigation'}
                >
                    {isOpen ? (
                        <X size={20} className="text-neutral-400" />
                    ) : (
                        <Menu size={20} className="text-neutral-400" />
                    )}
                </button>
                {isOpen && (
                    <div className="mt-2 flex flex-col gap-2 bg-neutral-900/95 border border-neutral-800 rounded-2xl p-2 shadow-xl min-w-[180px] max-w-[90vw]">
                        {isLoggedIn ? (
                            <>
                                <button
                                    onClick={handleProfileClick}
                                    className="flex items-center gap-2  px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-sm"
                                    title="Profile"
                                >
                                    <User size={16} className="text-yellow-500" />
                                    <span className='!mt-2.5'>{user?.username || 'Profile'}</span>
                                </button>
                                <a
                                    href="/dashboard"
                                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-sm"
                                    title="Dashboard"
                                >
                                    <LayoutDashboard size={16} className="text-yellow-500" />
                                    <span className='!mt-2.5'>Dashboard</span>

                                </a>
                                <a
                                    href="/settings"
                                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-sm"
                                    title="Settings"
                                >
                                    <Settings size={16} className="text-yellow-500" />
                                    <span className='!mt-2.5'>Settings</span>

                                </a>
                                <button
                                    onClick={handleLogout}
                                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-500 transition-colors text-white font-minecraftia text-sm"
                                    title="Logout"
                                >
                                    <LogIn size={16} className="rotate-180" />
                                    <span className='!mt-2.5'>Logout</span>
                                </button>
                            </>
                        ) : (
                            <button
                                onClick={handleLoginClick}
                                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-yellow-600 hover:bg-yellow-500 transition-colors text-black font-minecraftia text-sm font-semibold"
                                title="Login"
                            >
                                <LogIn size={16} />
                                <span className='!mt-2.5'>Login</span>

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