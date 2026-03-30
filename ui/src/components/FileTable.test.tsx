import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileTable } from './FileTable';
import type { FileRow } from '../types';

const makeRow = (overrides: Partial<FileRow> = {}): FileRow => ({
  id: 'row-1',
  sourcePath: '/tv/Fargo.S01E01.HDTV.mkv',
  parseResult: { show_name: 'Fargo', season: 1, episode: 1, resolution: '720p' },
  status: 'ready',
  seriesOptions: [
    { id: 101, name: 'Fargo', first_air_date: '2014-04-15' },
    { id: 202, name: 'Fargo (2024)', first_air_date: '2024-01-01' },
  ],
  selectedSeriesId: 101,
  episode: { name: 'The Crocodile', season_number: 1, episode_number: 1, air_date: null, overview: null },
  computedNewName: 'Fargo [1x01] The Crocodile',
  renameOutcome: null,
  errorMessage: null,
  ...overrides,
});

describe('FileTable', () => {
  it('renders source filename in current file column', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    // Basename is displayed, not full path
    expect(screen.getByText('Fargo.S01E01.HDTV.mkv')).toBeInTheDocument();
  });

  it('renders computed new filename in new filename column', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('Fargo [1x01] The Crocodile')).toBeInTheDocument();
  });

  it('shows series options in dropdown', () => {
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    const select = screen.getByRole('combobox');
    expect(select).toBeInTheDocument();
    expect(screen.getByText('Fargo (2014-04-15)')).toBeInTheDocument();
    expect(screen.getByText('Fargo (2024) (2024-01-01)')).toBeInTheDocument();
  });

  it('calls onSeriesChange when dropdown selection changes', () => {
    const onSeriesChange = vi.fn();
    render(
      <FileTable
        rows={[makeRow()]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={onSeriesChange}
      />
    );
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: '202' } });
    expect(onSeriesChange).toHaveBeenCalledWith('row-1', 202);
  });

  it('shows "Searching..." status for searching rows', () => {
    render(
      <FileTable
        rows={[makeRow({ status: 'searching' })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('Searching…')).toBeInTheDocument();
  });

  it('shows "No match" for no_match status', () => {
    render(
      <FileTable
        rows={[makeRow({ status: 'no_match', seriesOptions: [], selectedSeriesId: null, episode: null, computedNewName: null })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    expect(screen.getByText('No match')).toBeInTheDocument();
  });

  it('renders checkbox for each row and header checkbox', () => {
    render(
      <FileTable
        rows={[makeRow(), makeRow({ id: 'row-2', sourcePath: '/tv/Fargo.S01E02.mkv' })]}
        selectedIds={new Set()}
        onSelectionChange={vi.fn()}
        onSeriesChange={vi.fn()}
      />
    );
    // Header checkbox + 2 row checkboxes
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);
  });
});
