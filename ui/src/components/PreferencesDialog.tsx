import { useState } from 'react';
import { open } from '@tauri-apps/plugin-dialog';
import { usePreferences } from '../hooks/usePreferences';
import { TokenBuilder } from './TokenBuilder';
import type { UserPreferences } from '../types';

interface PreferencesDialogProps {
  onClose: () => void;
}

/**
 * Settings modal for all 12 UserPreferences fields.
 * Loads current prefs via usePreferences on mount.
 * Save button persists via save_preferences IPC.
 */
export function PreferencesDialog({ onClose }: PreferencesDialogProps) {
  const { prefs, loading, save } = usePreferences();
  const [draft, setDraft] = useState<UserPreferences | null>(null);

  // Initialise draft from loaded prefs (only once)
  if (prefs && !draft) setDraft({ ...prefs });

  if (loading) return <div>Loading preferences…</div>;
  if (!draft) return null;

  const set = <K extends keyof UserPreferences>(key: K, value: UserPreferences[K]) =>
    setDraft((prev) => prev ? { ...prev, [key]: value } : prev);

  async function handleSave() {
    if (!draft) return;
    await save(draft);
    onClose();
  }

  async function handlePickDir() {
    const selected = await open({ directory: true, multiple: false });
    if (typeof selected === 'string') set('dest_dir', selected);
  }

  return (
    <div role="dialog" aria-modal="true" style={{ padding: '24px', maxWidth: '600px' }}>
      <h2>Preferences</h2>

      {/* dest_dir */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="dest-dir">Destination Directory</label>
        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
          <input id="dest-dir" type="text" value={draft.dest_dir}
            onChange={(e) => set('dest_dir', e.target.value)} style={{ flex: 1 }} />
          <button type="button" onClick={handlePickDir}>Browse…</button>
        </div>
      </div>

      {/* season_prefix */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="season-prefix">Season Prefix</label>
        <input id="season-prefix" type="text" value={draft.season_prefix}
          onChange={(e) => set('season_prefix', e.target.value)} style={{ display: 'block', marginTop: '4px' }} />
      </div>

      {/* Booleans */}
      {(
        [
          ['rename_selected', 'Rename selected files'] as const,
          ['move_selected', 'Move selected files'] as const,
          ['season_prefix_leading_zero', 'Leading zero for season number'] as const,
          ['remove_emptied_directories', 'Remove emptied directories'] as const,
          ['delete_row_after_move', 'Delete row after move'] as const,
          ['check_for_updates', 'Check for updates'] as const,
          ['recursively_add_folders', 'Recursively add folders'] as const,
        ] as const
      ).map(([field, labelText]) => (
        <div key={field} style={{ marginBottom: '8px' }}>
          <label>
            <input
              type="checkbox"
              aria-label={labelText}
              checked={draft[field] as boolean}
              onChange={(e) => set(field, e.target.checked as UserPreferences[typeof field])}
            />
            {' '}{labelText}
          </label>
        </div>
      ))}

      {/* preload_folder */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="preload-folder">Preload Folder (optional)</label>
        <input id="preload-folder" type="text" value={draft.preload_folder ?? ''}
          onChange={(e) => set('preload_folder', e.target.value || null)}
          style={{ display: 'block', marginTop: '4px', width: '100%' }} />
      </div>

      {/* ignore_keywords */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="ignore-keywords">Ignore Keywords (comma-separated)</label>
        <input id="ignore-keywords" type="text"
          value={draft.ignore_keywords.join(', ')}
          onChange={(e) => set('ignore_keywords', e.target.value.split(',').map((s) => s.trim()).filter(Boolean))}
          style={{ display: 'block', marginTop: '4px', width: '100%' }} />
      </div>

      {/* rename_replacement_mask via TokenBuilder */}
      <div style={{ marginBottom: '16px' }}>
        <label>Rename Template</label>
        <TokenBuilder value={draft.rename_replacement_mask}
          onChange={(v) => set('rename_replacement_mask', v)} />
      </div>

      <div style={{ display: 'flex', gap: '8px' }}>
        <button onClick={handleSave}>Save</button>
        <button onClick={onClose}>Cancel</button>
      </div>
    </div>
  );
}
