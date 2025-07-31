'use client';
import React, { useState } from 'react';
import { X, User, Lock, LogIn, Eye, EyeOff } from 'lucide-react';
import { SERVER_NAME } from '@/lib/constants';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLogin: (credentials: { username: string; password: string }) => void;
}

const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose, onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!username.trim() || !password.trim()) {
      setError('Username and password are required');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await onLogin({ username: username.trim(), password });
      // reset on successful login
      setUsername('');
      setPassword('');
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={handleOverlayClick}
    >
      <div className="relative w-full max-w-md mx-4 bg-neutral-900 border border-neutral-700 rounded-2xl shadow-2xl">
        <div className="flex items-center justify-between p-6 border-b border-neutral-700">
          <h2 className="text-xl font-minecraftia text-neutral-100 leading-none pb-1">
            Login to <span className="text-yellow-600">{SERVER_NAME}</span>
          </h2>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-neutral-800 border border-neutral-600 flex items-center justify-center hover:bg-neutral-700 transition-colors"
            aria-label="Close modal"
          >
            <X size={16} className="text-neutral-400" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-lg bg-red-900/30 border border-red-700/50 text-red-300 font-minecraftia text-sm">
              <span className="leading-none">{error}</span>
            </div>
          )}

          <div className="space-y-2">
            <label htmlFor="username" className="block text-sm font-minecraftia text-neutral-300 leading-none pb-1">
              Username
            </label>
            <div className="relative">
              <User size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-neutral-500" />
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-neutral-800 border border-neutral-600 rounded-lg text-neutral-100 font-minecraftia text-sm placeholder-neutral-500 focus:border-yellow-500 focus:ring-1 focus:ring-yellow-500/20 focus:outline-none transition-colors leading-none"
                placeholder="Enter your username"
                disabled={isLoading}
                autoComplete="username"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="password" className="block text-sm font-minecraftia text-neutral-300 leading-none pb-1">
              Password
            </label>
            <div className="relative">
              <Lock size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-neutral-500" />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-12 py-3 bg-neutral-800 border border-neutral-600 rounded-lg text-neutral-100 font-minecraftia text-sm placeholder-neutral-500 focus:border-yellow-500 focus:ring-1 focus:ring-yellow-500/20 focus:outline-none transition-colors leading-none"
                placeholder="Enter your password"
                disabled={isLoading}
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-neutral-500 hover:text-neutral-400 transition-colors"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading || !username.trim() || !password.trim()}
            className="w-full flex items-center justify-center gap-2 py-3 px-4 bg-yellow-600 hover:bg-yellow-500 disabled:bg-neutral-700 disabled:text-neutral-500 text-black font-minecraftia text-sm font-semibold rounded-lg transition-colors disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <>
                <div className="w-4 h-4 border-2 border-black/30 border-t-black rounded-full animate-spin" />
                <span className="leading-none">Logging in...</span>
              </>
            ) : (
              <>
                <LogIn size={16} />
                <span className="leading-none mt-3">Login</span>
              </>
            )}
          </button>

          <p className="text-xs font-minecraftia text-neutral-500 text-center leading-none">
            Use your Minecraft server credentials to access the web chat
          </p>
        </form>
      </div>
    </div>
  );
};

export default LoginModal;