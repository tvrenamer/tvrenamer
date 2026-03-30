import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRenameProgress } from './useRenameProgress';
import { listen } from '@tauri-apps/api/event';
import type { RenameOutcome } from '../types';

const mockListen = vi.mocked(listen);

describe('useRenameProgress', () => {
  beforeEach(() => vi.clearAllMocks());

  it('registers listener for rename-progress on mount', async () => {
    const onProgress = vi.fn();
    mockListen.mockResolvedValue(() => {});

    renderHook(() => useRenameProgress(onProgress));

    await vi.waitFor(() => {
      expect(mockListen).toHaveBeenCalledWith('rename-progress', expect.any(Function));
    });
  });

  it('calls onProgress with RenameOutcome when event fires', async () => {
    const onProgress = vi.fn();
    let capturedHandler: ((e: { payload: RenameOutcome }) => void) | null = null;

    mockListen.mockImplementation(async (_event, handler) => {
      capturedHandler = handler as typeof capturedHandler;
      return () => {};
    });

    renderHook(() => useRenameProgress(onProgress));
    await vi.waitFor(() => capturedHandler !== null);

    const outcome: RenameOutcome = {
      source: '/tv/Fargo.S01E01.mkv',
      dest: '/tv/Fargo/Season 1/Fargo [1x01] The Crocodile.mkv',
      status: 'success',
      error: null,
    };

    act(() => { capturedHandler!({ payload: outcome }); });

    expect(onProgress).toHaveBeenCalledWith(outcome);
  });

  it('cleans up listener on unmount', async () => {
    const unlisten = vi.fn();
    mockListen.mockResolvedValue(unlisten);
    const { unmount } = renderHook(() => useRenameProgress(vi.fn()));
    await vi.waitFor(() => expect(mockListen).toHaveBeenCalled());
    unmount();
    expect(unlisten).toHaveBeenCalled();
  });
});
