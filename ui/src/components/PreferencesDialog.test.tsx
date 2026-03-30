import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { PreferencesDialog } from './PreferencesDialog';
import { invoke } from '@tauri-apps/api/core';
import type { UserPreferences } from '../types';

// Also mock the dialog plugin
vi.mock('@tauri-apps/plugin-dialog', () => ({
  open: vi.fn().mockResolvedValue('/picked/dir'),
}));

const mockInvoke = vi.mocked(invoke);

const defaultPrefs: UserPreferences = {
  version: 1, preload_folder: null, dest_dir: '~/TV', season_prefix: 'Season ',
  season_prefix_leading_zero: false, move_selected: false, rename_selected: true,
  remove_emptied_directories: true, delete_row_after_move: false,
  rename_replacement_mask: '%S [%sx%0e] %t', check_for_updates: true,
  recursively_add_folders: true, ignore_keywords: ['sample'],
};

describe('PreferencesDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockInvoke.mockResolvedValue(defaultPrefs);
  });

  it('renders all 12 preference fields', async () => {
    render(<PreferencesDialog onClose={vi.fn()} />);
    await waitFor(() => expect(screen.getByLabelText(/destination directory/i)).toBeInTheDocument());

    expect(screen.getByLabelText(/rename selected/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/move selected/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/season prefix/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/leading zero/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/remove emptied/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/delete row/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/check for updates/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/recursively/i)).toBeInTheDocument();
    // rename_replacement_mask via TokenBuilder
    expect(screen.getByRole('textbox', { name: /rename mask/i })).toBeInTheDocument();
  });

  it('calls save_preferences on Save', async () => {
    mockInvoke
      .mockResolvedValueOnce(defaultPrefs) // get_preferences
      .mockResolvedValueOnce(undefined);    // save_preferences

    render(<PreferencesDialog onClose={vi.fn()} />);
    await waitFor(() => screen.getByRole('button', { name: /save/i }));

    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('save_preferences', { newPrefs: expect.objectContaining({ version: 1 }) });
    });
  });

  it('calls onClose when Cancel is clicked', async () => {
    const onClose = vi.fn();
    render(<PreferencesDialog onClose={onClose} />);
    await waitFor(() => screen.getByRole('button', { name: /cancel/i }));
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onClose).toHaveBeenCalled();
  });
});
