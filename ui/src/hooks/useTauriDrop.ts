import { useEffect, useRef } from 'react';
import { listen } from '@tauri-apps/api/event';

interface DragDropPayload {
  paths: string[];
}

/**
 * Listens for Tauri native drag-drop events (dragDropEnabled: true in tauri.conf.json).
 * Calls onDrop with the array of OS-level file paths when files are dropped onto the window.
 * Automatically cleans up the listener on unmount.
 */
export function useTauriDrop(onDrop: (paths: string[]) => void): void {
  // Keep onDrop in a ref so the effect doesn't re-run when the callback identity changes.
  const onDropRef = useRef(onDrop);
  onDropRef.current = onDrop;

  useEffect(() => {
    // `cancelled` flag handles React StrictMode's double-invocation: if cleanup runs
    // before the listen() promise resolves, the unlisten fn is called immediately on resolve.
    let cancelled = false;
    let unlisten: (() => void) | null = null;

    listen<DragDropPayload>('tauri://drag-drop', (event) => {
      onDropRef.current(event.payload.paths);
    }).then((fn) => {
      if (cancelled) fn();
      else unlisten = fn;
    });

    return () => {
      cancelled = true;
      unlisten?.();
    };
  }, []); // effect runs once — listener is stable
}
