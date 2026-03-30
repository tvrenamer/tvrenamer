import { useState, useEffect, useCallback } from 'react';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

interface UsePreferencesResult {
  prefs: UserPreferences | null;
  loading: boolean;
  error: string | null;
  save: (updated: UserPreferences) => Promise<void>;
}

/**
 * Loads preferences on mount via get_preferences IPC.
 * Provides a save() function that calls save_preferences IPC.
 */
export function usePreferences(): UsePreferencesResult {
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    invoke<UserPreferences>('get_preferences')
      .then(setPrefs)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false));
  }, []);

  const save = useCallback(async (updated: UserPreferences) => {
    await invoke('save_preferences', { newPrefs: updated });
    setPrefs(updated);
  }, []);

  return { prefs, loading, error, save };
}
