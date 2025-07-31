'use client';
import React, { useState } from 'react';
import { User, Settings, LayoutDashboard, LogIn, Menu, X } from 'lucide-react';

const NavBar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    const toggleNav = () => setIsOpen((v) => !v);
    const handleLogin = () => setIsLoggedIn((v) => !v);

    return (
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
                                onClick={handleLogin}
                                className="flex items-center gap-2  px-4 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 transition-colors text-neutral-200 font-minecraftia text-sm"
                                title="Profile"
                            >
                                <User size={16} className="text-yellow-500" />
                                <span className='!mt-2.5'>Profile</span>
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
                        </>
                    ) : (
                        <button
                            onClick={handleLogin}
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
    );
};

export default NavBar;