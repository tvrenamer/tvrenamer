import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { usePreferences } from './usePreferences';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

const mockInvoke = vi.mocked(invoke);

const defaultPrefs: UserPreferences = {
  version: 1,
  preload_folder: null,
  dest_dir: '~/TV',
  season_prefix: 'Season ',
  season_prefix_leading_zero: false,
  move_selected: false,
  rename_selected: true,
  remove_emptied_directories: true,
  delete_row_after_move: false,
  rename_replacement_mask: '%S [%sx%0e] %t',
  check_for_updates: true,
  recursively_add_folders: true,
  ignore_keywords: ['sample'],
};

describe('usePreferences', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls get_preferences on mount and returns prefs', async () => {
    mockInvoke.mockResolvedValue(defaultPrefs);

    const { result } = renderHook(() => usePreferences());

    await waitFor(() => expect(result.current.prefs).not.toBeNull());

    expect(mockInvoke).toHaveBeenCalledWith('get_preferences');
    expect(result.current.prefs?.dest_dir).toBe('~/TV');
  });

  it('save calls save_preferences with updated prefs', async () => {
    mockInvoke.mockResolvedValue(defaultPrefs);
    const { result } = renderHook(() => usePreferences());
    await waitFor(() => expect(result.current.prefs).not.toBeNull());

    mockInvoke.mockResolvedValue(undefined);

    const updated = { ...defaultPrefs, dest_dir: '/mnt/tv' };
    await act(async () => { await result.current.save(updated); });

    expect(mockInvoke).toHaveBeenCalledWith('save_preferences', { newPrefs: updated });
  });

  it('exposes loading state initially', async () => {
    let resolve: (v: UserPreferences) => void;
    mockInvoke.mockReturnValue(new Promise<UserPreferences>((r) => { resolve = r; }));

    const { result } = renderHook(() => usePreferences());

    expect(result.current.loading).toBe(true);

    act(() => { resolve!(defaultPrefs); });
    await waitFor(() => expect(result.current.loading).toBe(false));
  });
});
