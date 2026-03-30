import { useEffect, useRef } from 'react';
import { listen } from '@tauri-apps/api/event';
import type { RenameOutcome } from '../types';

/**
 * Listens for `rename-progress` events emitted by the Rust `perform_renames` command.
 * Calls onProgress after each file is processed, with the outcome for that file.
 * Automatically cleans up on unmount.
 */
export function useRenameProgress(onProgress: (outcome: RenameOutcome) => void): void {
  const onProgressRef = useRef(onProgress);
  onProgressRef.current = onProgress;

  useEffect(() => {
    let unlisten: (() => void) | null = null;

    listen<RenameOutcome>('rename-progress', (event) => {
      onProgressRef.current(event.payload);
    }).then((fn) => {
      unlisten = fn;
    });

    return () => {
      if (unlisten) unlisten();
    };
  }, []);
}
