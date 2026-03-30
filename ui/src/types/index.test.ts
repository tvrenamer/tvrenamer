import { describe, it, expect } from 'vitest';
import type { Series, Episode, UserPreferences, ParseResult, RenameOutcome, FileRow } from './index';
import { applyTemplate } from './index';

describe('TypeScript types compile and satisfy contracts', () => {
  it('Series matches IPC contract', () => {
    const s: Series = { id: 1, name: 'Fargo', first_air_date: '2014-04-15' };
    expect(s.id).toBe(1);
  });

  it('Episode matches IPC contract', () => {
    const e: Episode = {
      name: 'The Crocodile',
      season_number: 1,
      episode_number: 2,
      air_date: '2014-04-22',
      overview: null,
    };
    expect(e.season_number).toBe(1);
  });

  it('UserPreferences matches IPC contract (12 fields + version)', () => {
    const p: UserPreferences = {
      version: 1,
      preload_folder: null,
      dest_dir: '~/TV',
      season_prefix: 'Season ',
      season_prefix_leading_zero: false,
      move_selected: false,
      rename_selected: true,
      remove_emptied_directories: true,
      delete_row_after_move: false,
      rename_replacement_mask: '%S [%sx%0e] %t',
      check_for_updates: true,
      recursively_add_folders: true,
      ignore_keywords: ['sample'],
    };
    expect(p.rename_replacement_mask).toBe('%S [%sx%0e] %t');
  });

  it('ParseResult matches IPC contract', () => {
    const r: ParseResult = { show_name: 'Fargo', season: 1, episode: 1, resolution: '720p' };
    expect(r.show_name).toBe('Fargo');
  });

  it('RenameOutcome status union is exhaustive', () => {
    const statuses: RenameOutcome['status'][] = ['success', 'already_in_place', 'fail_to_move'];
    expect(statuses).toHaveLength(3);
  });

  it('FileRow has required shape', () => {
    const row: FileRow = {
      id: 'abc',
      sourcePath: '/tv/Fargo.S01E01.mkv',
      parseResult: null,
      status: 'idle',
      seriesOptions: [],
      selectedSeriesId: null,
      episode: null,
      computedNewName: null,
      renameOutcome: null,
      errorMessage: null,
    };
    expect(row.status).toBe('idle');
  });

  it('applyTemplate produces correct filename', () => {
    expect(applyTemplate('%S [%sx%0e] %t', 'Fargo', 1, 1, 'The Crocodile'))
      .toBe('Fargo [1x01] The Crocodile');
  });
});
