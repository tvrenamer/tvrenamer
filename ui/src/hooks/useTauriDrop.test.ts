import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useTauriDrop } from './useTauriDrop';
import { listen } from '@tauri-apps/api/event';

const mockListen = vi.mocked(listen);

describe('useTauriDrop', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('registers listener for tauri://drag-drop on mount', async () => {
    const onDrop = vi.fn();
    mockListen.mockResolvedValue(() => {});

    renderHook(() => useTauriDrop(onDrop));

    // Wait for the effect to run
    await vi.waitFor(() => {
      expect(mockListen).toHaveBeenCalledWith('tauri://drag-drop', expect.any(Function));
    });
  });

  it('calls onDrop with file paths when tauri://drag-drop fires', async () => {
    const onDrop = vi.fn();
    let capturedHandler: ((e: { payload: { paths: string[] } }) => void) | null = null;

    mockListen.mockImplementation(async (_event, handler) => {
      capturedHandler = handler as typeof capturedHandler;
      return () => {};
    });

    renderHook(() => useTauriDrop(onDrop));

    await vi.waitFor(() => capturedHandler !== null);

    act(() => {
      capturedHandler!({ payload: { paths: ['/tv/Fargo.S01E01.mkv', '/tv/Fargo.S01E02.mkv'] } });
    });

    expect(onDrop).toHaveBeenCalledWith(['/tv/Fargo.S01E01.mkv', '/tv/Fargo.S01E02.mkv']);
  });

  it('calls unlisten on unmount', async () => {
    const unlisten = vi.fn();
    mockListen.mockResolvedValue(unlisten);
    const onDrop = vi.fn();

    const { unmount } = renderHook(() => useTauriDrop(onDrop));
    await vi.waitFor(() => expect(mockListen).toHaveBeenCalled());

    unmount();

    expect(unlisten).toHaveBeenCalled();
  });
});
