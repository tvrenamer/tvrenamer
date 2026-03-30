import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ApiKeySetup } from './ApiKeySetup';
import { invoke } from '@tauri-apps/api/core';

const mockInvoke = vi.mocked(invoke);

describe('ApiKeySetup', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders the modal with explanation and input', () => {
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);
    expect(screen.getByText(/TMDB API key/i)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /test/i })).toBeInTheDocument();
  });

  it('enables Save button only after a successful test', async () => {
    mockInvoke.mockResolvedValue(undefined); // validate_tmdb_key returns Ok(())
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'abc123' } });

    const saveButton = screen.queryByRole('button', { name: /save/i });
    expect(saveButton).toBeNull(); // not shown before test passes

    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('validate_tmdb_key', { key: 'abc123' });
    });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    });
  });

  it('shows error message on failed validation', async () => {
    mockInvoke.mockRejectedValue('API key invalid or missing');
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={vi.fn()} />);

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'badkey' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => {
      expect(screen.getByText(/API key invalid or missing/i)).toBeInTheDocument();
    });
  });

  it('calls save_tmdb_key and then onSaved after saving', async () => {
    mockInvoke
      .mockResolvedValueOnce(undefined) // validate
      .mockResolvedValueOnce(undefined); // save

    const onSaved = vi.fn();
    render(<ApiKeySetup onDismiss={vi.fn()} onSaved={onSaved} />);

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'validkey' } });
    fireEvent.click(screen.getByRole('button', { name: /test/i }));

    await waitFor(() => screen.getByRole('button', { name: /save/i }));
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockInvoke).toHaveBeenCalledWith('save_tmdb_key', { key: 'validkey' });
      expect(onSaved).toHaveBeenCalled();
    });
  });

  it('calls onDismiss when dismissed without saving', () => {
    const onDismiss = vi.fn();
    render(<ApiKeySetup onDismiss={onDismiss} onSaved={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /skip/i }));
    expect(onDismiss).toHaveBeenCalled();
  });
});
