import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';
import { invoke } from '@tauri-apps/api/core';

vi.mocked(invoke).mockResolvedValue([]);

describe('App', () => {
  it('renders the main heading', () => {
    render(<App />);
    expect(screen.getByText('TVRenamer')).toBeInTheDocument();
  });

  it('renders the Preferences button', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /preferences/i })).toBeInTheDocument();
  });

  it('renders the drop zone instruction text when no files loaded', () => {
    render(<App />);
    expect(screen.getByText(/drop files/i)).toBeInTheDocument();
  });

  it('renders the Rename Selected button', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /rename selected/i })).toBeInTheDocument();
  });
});
