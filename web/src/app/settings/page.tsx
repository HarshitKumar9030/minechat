"use client";
import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { settingsApi } from '@/lib/api';
import { Lock, Unlock, Save } from 'lucide-react';

export default function SettingsPage() {
  const [user, setUser] = useState<{ playerUUID: string; playerName: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [webAccessEnabled, setWebAccessEnabled] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enablePassword, setEnablePassword] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem('minechat_user');
    if (!saved) {
      setLoading(false);
      return;
    }
    try {
      const parsed = JSON.parse(saved);
      setUser({ playerUUID: parsed.playerUUID, playerName: parsed.playerName });
    } catch {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const fetchSettings = async () => {
      if (!user) return;
      try {
        setLoading(true);
        const res = await settingsApi.getUserSettings(user.playerUUID);
        setWebAccessEnabled(!!res?.user?.webAccessEnabled);
      } catch (e: unknown) {
        const msg = e instanceof Error ? e.message : 'Failed to load settings';
        setError(msg);
      } finally {
        setLoading(false);
      }
    };
    fetchSettings();
  }, [user]);

  const handleToggleWebAccess = async () => {
    if (!user) return;
    setError(null); setStatus(null);
    try {
      setSaving(true);
      if (!webAccessEnabled) {
        if (!enablePassword || enablePassword.length < 4) {
          setError('Please provide a password (min 4 chars)');
          return;
        }
        await settingsApi.enableWebAccess(user.playerUUID, user.playerName, enablePassword);
        setEnablePassword('');
        setWebAccessEnabled(true);
        setStatus('Web access enabled');
      } else {
        await settingsApi.disableWebAccess(user.playerUUID);
        setWebAccessEnabled(false);
        setStatus('Web access disabled');
      }
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Operation failed';
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdatePassword = async () => {
    if (!user) return;
    setError(null); setStatus(null);
    try {
      if (!newPassword || newPassword.length < 4) {
        setError('New password must be at least 4 characters');
        return;
      }
      setSaving(true);
      await settingsApi.updateWebPassword(user.playerUUID, user.playerName, currentPassword || undefined, newPassword);
      setCurrentPassword('');
      setNewPassword('');
  setStatus('Password updated');
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Failed to update password';
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-900">
      <div className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-minecraftia text-neutral-100 mb-6">Settings</h1>

        {loading ? (
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
            <div className="w-8 h-8 border-2 border-neutral-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : !user ? (
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
            <h2 className="text-lg font-minecraftia text-neutral-100 mb-2">Web Access</h2>
            <p className="text-neutral-300 mb-3">
              You need to enable web access from in-game before using the web app.
            </p>
            <div className="bg-neutral-900/60 border border-neutral-700 rounded-lg p-3">
              <code className="text-yellow-400">/friend webaccess enable &lt;password&gt;</code>
            </div>
            <p className="text-neutral-400 text-sm mt-3">
              After enabling, sign in on the Home page with your Minecraft username and this web password.
            </p>
            <Link href="/" className="inline-block mt-4 px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 rounded-lg">Go to Home</Link>
            <p className="text-neutral-500 text-xs mt-3">
              Tip: If you disable web access here later, you will need to run the in-game command again to re-enable it.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {error && (
              <div className="bg-red-900/30 border border-red-700 text-red-300 rounded-lg p-3">{error}</div>
            )}
            {status && (
              <div className="bg-green-900/30 border border-green-700 text-green-300 rounded-lg p-3">{status}</div>
            )}

            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-lg font-minecraftia text-neutral-100">Web Access</h2>
                  <p className="text-sm text-neutral-400">Control your ability to sign in to the web app. If you disable this, you must re-enable it in-game with the command below.</p>
                </div>
                <button
                  onClick={handleToggleWebAccess}
                  disabled={saving}
                  className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${webAccessEnabled ? 'bg-red-600 hover:bg-red-700' : 'bg-yellow-600 hover:bg-yellow-700'} text-neutral-900`}
                >
                  {webAccessEnabled ? <Lock className="w-4 h-4"/> : <Unlock className="w-4 h-4"/>}
                  {webAccessEnabled ? 'Disable' : 'Enable'}
                </button>
              </div>

              {!webAccessEnabled && (
                <div className="mt-3">
                  <label className="block text-sm text-neutral-300 mb-1">Set Password</label>
                  <input
                    type="password"
                    value={enablePassword}
                    onChange={(e) => setEnablePassword(e.target.value)}
                    placeholder="New password for web access"
                    className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400"
                  />
                  <p className="text-xs text-neutral-500 mt-1">Required to enable web access</p>
                  <div className="bg-neutral-900/60 border border-neutral-700 rounded-lg p-3 mt-3">
                    <code className="text-yellow-400">/friend webaccess enable &lt;password&gt;</code>
                  </div>
                </div>
              )}
            </div>

            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-6">
              <div className="mb-4">
                <h2 className="text-lg font-minecraftia text-neutral-100">Change Web Password</h2>
                <p className="text-sm text-neutral-400">Update the password used to log into the web app</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-neutral-300 mb-1">Current Password</label>
                  <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Optional if not set before"
                    className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400"
                  />
                </div>
                <div>
                  <label className="block text-sm text-neutral-300 mb-1">New Password</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Minimum 4 characters"
                    className="w-full px-3 py-2 bg-neutral-700 border border-neutral-600 rounded-lg text-neutral-200 placeholder-neutral-400"
                  />
                </div>
              </div>

              <div className="mt-4">
                <button
                  onClick={handleUpdatePassword}
                  disabled={saving}
                  className="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-neutral-900 rounded-lg flex items-center gap-2"
                >
                  <Save className="w-4 h-4"/>
                  Update Password
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
