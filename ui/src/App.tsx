import { useState, useCallback, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { open as openFilePicker } from '@tauri-apps/plugin-dialog';
import { useTauriDrop } from './hooks/useTauriDrop';
import { useRenameProgress } from './hooks/useRenameProgress';
import { FileTable } from './components/FileTable';
import { StatusBar, type Phase } from './components/StatusBar';
import { ApiKeySetup } from './components/ApiKeySetup';
import { PreferencesDialog } from './components/PreferencesDialog';
import { applyTemplate } from './types';
import type { FileRow, ParseResult, Series, Episode, RenameOutcome, UserPreferences } from './types';

let rowCounter = 0;
function nextId(): string { return `row-${++rowCounter}`; }

function buildInitialRow(path: string, parseResult: ParseResult | null): FileRow {
  return {
    id: nextId(),
    sourcePath: path,
    parseResult,
    status: parseResult ? 'searching' : 'parse_failed',
    seriesOptions: [],
    selectedSeriesId: null,
    episode: null,
    computedNewName: null,
    renameOutcome: null,
    errorMessage: null,
  };
}

export default function App() {
  const [rows, setRows] = useState<FileRow[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [showPrefs, setShowPrefs] = useState(false);
  const [showApiSetup, setShowApiSetup] = useState(false);
  const [phase, setPhase] = useState<Phase>('idle');
  const [prefs, setPrefs] = useState<UserPreferences | null>(null);

  // Load prefs once on mount
  // NOTE: Must use useEffect, NOT useState — useState initializer sets initial state,
  // it does not execute side effects. Using useState(() => {...}) would store a function
  // object as the state value, never invoking it.
  useEffect(() => {
    invoke<UserPreferences>('get_preferences').then(setPrefs).catch(() => {});
  }, []);

  // Update a single row by id
  const updateRow = useCallback((id: string, changes: Partial<FileRow>) => {
    setRows((prev) => prev.map((r) => r.id === id ? { ...r, ...changes } : r));
  }, []);

  // Called when files are dropped
  const handleDrop = useCallback(async (paths: string[]) => {
    // Parse all dropped files in one IPC call
    const parseResults: Array<ParseResult | null> = await invoke('parse_files', { paths });

    const newRows = paths.map((path, i) => buildInitialRow(path, parseResults[i] ?? null));
    setRows((prev) => [...prev, ...newRows]);
    setPhase('searching');

    // Kick off TMDB search for each successfully parsed file
    for (const row of newRows) {
      if (!row.parseResult) continue;
      const { show_name: showName, season, episode } = row.parseResult;

      invoke<Series[]>('search_shows', { query: showName })
        .then(async (seriesOptions) => {
          if (seriesOptions.length === 0) {
            updateRow(row.id, { status: 'no_match', seriesOptions: [] });
            return;
          }

          const selectedSeries = seriesOptions[0];
          let ep: Episode | null = null;
          try {
            ep = await invoke<Episode>('lookup_episode', {
              seriesId: selectedSeries.id,
              season,
              episode,
            });
          } catch {
            // Episode not found — still mark ready with no computed name
          }

          const ext = row.sourcePath.split('.').pop();
          const baseName = ep && prefs
            ? applyTemplate(prefs.rename_replacement_mask, selectedSeries.name, season, episode, ep.name)
            : null;
          const computedNewName = baseName && ext ? `${baseName}.${ext}` : baseName;

          updateRow(row.id, {
            status: 'ready',
            seriesOptions,
            selectedSeriesId: selectedSeries.id,
            episode: ep,
            computedNewName,
          });
        })
        .catch((e: string) => {
          // Check if error indicates missing API key
          if (String(e).includes('ApiKeyMissing') || String(e).includes('API key invalid or missing')) {
            setShowApiSetup(true);
            updateRow(row.id, { status: 'error', errorMessage: 'TMDB API key not set' });
          } else {
            updateRow(row.id, { status: 'error', errorMessage: String(e) });
          }
        });
    }
  }, [prefs, updateRow]);

  useTauriDrop(handleDrop);

  const handleAddFiles = useCallback(async () => {
    const selected = await openFilePicker({ multiple: true, directory: false });
    if (!selected) return;
    const paths = Array.isArray(selected) ? selected : [selected];
    if (paths.length > 0) handleDrop(paths);
  }, [handleDrop]);

  // Called when rename-progress event fires for a file
  const handleRenameProgress = useCallback((outcome: RenameOutcome) => {
    setRows((prev) =>
      prev.map((r) =>
        r.sourcePath === outcome.source
          ? { ...r, status: outcome.status === 'success' ? 'success' : 'error', renameOutcome: outcome, errorMessage: outcome.error }
          : r
      )
    );
  }, []);

  useRenameProgress(handleRenameProgress);

  // Series dropdown changed for a row
  const handleSeriesChange = useCallback(async (rowId: string, seriesId: number) => {
    const row = rows.find((r) => r.id === rowId);
    if (!row?.parseResult) return;

    const { season, episode } = row.parseResult;
    const selectedSeries = row.seriesOptions.find((s) => s.id === seriesId);
    if (!selectedSeries) return;

    updateRow(rowId, { selectedSeriesId: seriesId, episode: null, computedNewName: null });

    try {
      const ep = await invoke<Episode>('lookup_episode', { seriesId, season, episode });
      const ext = row.sourcePath.split('.').pop();
      const baseName = prefs
        ? applyTemplate(prefs.rename_replacement_mask, selectedSeries.name, season, episode, ep.name)
        : null;
      const computedNewName = baseName && ext ? `${baseName}.${ext}` : baseName;
      updateRow(rowId, { episode: ep, computedNewName });
    } catch {
      updateRow(rowId, { episode: null, computedNewName: null });
    }
  }, [rows, prefs, updateRow]);

  // Checkbox selection
  const handleSelectionChange = useCallback((id: string, checked: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  }, []);

  // Rename selected rows
  async function handleRename() {
    const toRename = rows.filter(
      (r) => selectedIds.has(r.id) && r.computedNewName && r.status === 'ready'
    );
    if (toRename.length === 0) return;

    setPhase('renaming');

    const renames = toRename.map((r) => ({
      source: r.sourcePath,
      // Rename in place (same directory, new filename from template)
      dest: [r.sourcePath.split('/').slice(0, -1).join('/'), r.computedNewName].filter(Boolean).join('/'),
    }));

    try {
      await invoke('perform_renames', { renames });
      setPhase('complete');
    } catch (e) {
      console.error('perform_renames failed:', e);
    }
  }

  const successCount = rows.filter((r) => r.status === 'success').length;
  const errorCount = rows.filter((r) => r.status === 'error').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: 'system-ui' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '8px 12px', borderBottom: '1px solid #ccc' }}>
        <h1 style={{ margin: 0, fontSize: '1.1em' }}>TVRenamer</h1>
        <button onClick={() => setShowPrefs(true)}>Preferences</button>
        <button onClick={handleAddFiles}>Add Files…</button>
        <div style={{ flex: 1 }} />
        <button onClick={handleRename} disabled={selectedIds.size === 0}>
          Rename Selected
        </button>
      </div>

      {/* Main content area */}
      <div style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        {rows.length === 0 ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#999' }}>
            Drop files here to begin
          </div>
        ) : (
          <FileTable
            rows={rows}
            selectedIds={selectedIds}
            onSelectionChange={handleSelectionChange}
            onSeriesChange={handleSeriesChange}
          />
        )}
      </div>

      {/* Status bar — only shown once files are loaded */}
      {rows.length > 0 && (
        <StatusBar total={rows.length} success={successCount} failed={errorCount} phase={phase} />
      )}

      {/* Modals */}
      {showApiSetup && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: '8px' }}>
            <ApiKeySetup onDismiss={() => setShowApiSetup(false)} onSaved={() => setShowApiSetup(false)} />
          </div>
        </div>
      )}

      {showPrefs && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'white', borderRadius: '8px' }}>
            <PreferencesDialog onClose={() => setShowPrefs(false)} />
          </div>
        </div>
      )}
    </div>
  );
}
