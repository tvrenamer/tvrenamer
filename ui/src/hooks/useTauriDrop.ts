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
    let unlisten: (() => void) | null = null;

    listen<DragDropPayload>('tauri://drag-drop', (event) => {
      onDropRef.current(event.payload.paths);
    }).then((fn) => {
      unlisten = fn;
    });

    return () => {
      if (unlisten) unlisten();
    };
  }, []); // effect runs once — listener is stable
}
