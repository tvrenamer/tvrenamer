// TypeScript mirror of Rust IPC types.
// Keep in sync with src-tauri/src/metadata/models.rs, config/prefs.rs, ipc.rs, parser/mod.rs.

export interface Series {
  id: number;
  name: string;
  first_air_date: string | null;
}

export interface Episode {
  name: string;
  season_number: number;
  episode_number: number;
  air_date: string | null;
  overview: string | null;
}

export interface UserPreferences {
  version: number;
  preload_folder: string | null;
  dest_dir: string;
  season_prefix: string;
  season_prefix_leading_zero: boolean;
  move_selected: boolean;
  rename_selected: boolean;
  remove_emptied_directories: boolean;
  delete_row_after_move: boolean;
  rename_replacement_mask: string;
  check_for_updates: boolean;
  recursively_add_folders: boolean;
  ignore_keywords: string[];
}

export interface ParseResult {
  show_name: string;
  season: number;
  episode: number;
  resolution: string | null;
}

export interface RenameRequest {
  source: string;
  dest: string;
}

export type RenameStatus = 'success' | 'already_in_place' | 'fail_to_move';

export interface RenameOutcome {
  source: string;
  dest: string;
  status: RenameStatus;
  error: string | null;
}

export type RowStatus =
  | 'idle'
  | 'searching'
  | 'ready'
  | 'no_match'
  | 'parse_failed'
  | 'renaming'
  | 'success'
  | 'error';

export interface FileRow {
  id: string;
  sourcePath: string;
  parseResult: ParseResult | null;
  status: RowStatus;
  seriesOptions: Series[];
  selectedSeriesId: number | null;
  episode: Episode | null;
  computedNewName: string | null; // result of applyTemplate + optional dest path
  renameOutcome: RenameOutcome | null;
  errorMessage: string | null;
}

/**
 * Replicate apply_template from src-tauri/src/renamer/template.rs.
 * Tokens: %S = show name, %s = season (unpadded), %0e = episode (zero-padded 2 digits), %t = title.
 * Runs entirely in the frontend — avoids a Rust round-trip for a pure string substitution.
 */
export function applyTemplate(
  mask: string,
  show: string,
  season: number,
  episode: number,
  title: string,
): string {
  return mask
    .replaceAll('%S', show)
    .replaceAll('%s', String(season))
    .replaceAll('%0e', String(episode).padStart(2, '0'))
    .replaceAll('%t', title);
}
