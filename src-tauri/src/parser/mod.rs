use std::sync::OnceLock;

use regex::Regex;

#[derive(Debug, PartialEq, serde::Serialize)]
pub struct ParseResult {
    pub show_name: String,
    pub season: u32,
    pub episode: u32,
    pub resolution: Option<String>,
}

// "versions" is the duplicates staging directory — skip when climbing the path tree.
// Confirmed from Constants.java:207: DUPLICATES_DIRECTORY = "versions"
const DUPLICATES_DIRECTORY: &str = "versions";

// The 8 base patterns, verbatim from FilenameParser.java REGEX[] (translated from Java
// string escapes to Rust raw strings).  No lookahead/lookbehind — regex crate is sufficient.
// NOTE: ^ is NOT included here; it is prepended when building COMPILED_PATTERNS below.
const BASE_PATTERNS: [&str; 8] = [
    // Pattern 1: SxxExx — e.g. "Show.S01E05", "Show.S22E105"
    r"(.+?[^a-zA-Z0-9]\D*?)[sS](\d\d*)[eE](\d\d*).*",

    // Pattern 2: Season-XX-Episode-XX
    r"(.+?[^a-zA-Z0-9]\D*?)Season[- ](\d\d*)[- ]?Episode[- ](\d\d*).*",

    // Pattern 3: sXX.eXX — flexible separators between s/e and digits
    r"(.+[^a-zA-Z0-9]\D*?)[sS](\d\d*)\D*?[eE](\d\d*).*",

    // Pattern 4: SSxEE — with optional leading "S" (e.g. "5x01", "S5x01")
    r"(.+[^a-zA-Z0-9]\D*?)[Ss](\d\d?)x(\d\d\d?).*",

    // Pattern 5: titles with 4-digit year in show name (e.g. "castle.2009", "human.target.2010")
    r"(.+?\d{4}[^a-zA-Z0-9]\D*?)[sS]?(\d\d?)\D*?(\d\d).*",

    // Pattern 6: SXXYY — exactly 4 digits after S (e.g. "ncis.1304", "law.and.order.svu.1705")
    r"(.+?[^a-zA-Z0-9]\D*?)[sS](\d\d)(\d\d)\D.*",

    // Pattern 7: Fallback — show name + 1-2 digit season + non-digit separator + 2-digit episode
    r"(.+[^a-zA-Z0-9]\D*?)(\d\d?)\D+(\d\d).*",

    // Pattern 8: Last resort — minimal structure
    r"(.+[^a-zA-Z0-9]+)(\d\d?)(\d\d).*",
];

// Appended to each base pattern for the "with resolution" variant.
// \D ensures the resolution is preceded by a separator (e.g. ".720p", " 1080p").
const RESOLUTION_SUFFIX: &str = r"\D(\d+[pk]).*";

// 16 compiled patterns: [0..7] with resolution suffix, [8..15] without.
// OnceLock gives us thread-safe lazy initialisation without a runtime cost on every call.
static COMPILED_PATTERNS: OnceLock<Vec<Regex>> = OnceLock::new();

// Helper regex patterns — also lazy-compiled via OnceLock.
static FILENAME_BEGINS_WITH_SEASON: OnceLock<Regex> = OnceLock::new();
static DIR_LOOKS_LIKE_SEASON: OnceLock<Regex> = OnceLock::new();
static EXCESS_SEASON: OnceLock<Regex> = OnceLock::new();

fn compiled_patterns() -> &'static [Regex] {
    COMPILED_PATTERNS.get_or_init(|| {
        let mut v = Vec::with_capacity(16);
        // First 8: each base pattern + resolution suffix (tried first)
        for base in BASE_PATTERNS {
            let with_res = format!("^{}{}", base, RESOLUTION_SUFFIX);
            v.push(Regex::new(&with_res).expect("invalid pattern with resolution"));
        }
        // Next 8: base patterns alone (tried if no resolution variant matched)
        for base in BASE_PATTERNS {
            let plain = format!("^{}", base);
            v.push(Regex::new(&plain).expect("invalid base pattern"));
        }
        v
    })
}

fn filename_begins_with_season() -> &'static Regex {
    // Java: FILENAME_BEGINS_WITH_SEASON = "(([sS]\d\d?[eE]\d\d?)|([sS]?\d\d?[x.]?\d\d\d?)).*"
    // Java's String.matches() checks the full string, so ^ is implicit. Since it ends with .*,
    // only a ^ prefix is needed in Rust.
    FILENAME_BEGINS_WITH_SEASON.get_or_init(|| {
        Regex::new(r"^(([sS]\d\d?[eE]\d\d?)|([sS]?\d\d?[x.]?\d\d\d?)).*")
            .expect("invalid FILENAME_BEGINS_WITH_SEASON")
    })
}

fn dir_looks_like_season() -> &'static Regex {
    // Java: DIR_LOOKS_LIKE_SEASON = "[sS][0-3]\d"
    // Java's String.matches() = full string match → anchor both ends.
    DIR_LOOKS_LIKE_SEASON.get_or_init(|| {
        Regex::new(r"^[sS][0-3]\d$").expect("invalid DIR_LOOKS_LIKE_SEASON")
    })
}

fn excess_season() -> &'static Regex {
    // Java: EXCESS_SEASON = "[^A-Za-z]Season[ _-]?\d\d?"
    // Strips ".Season01" from directory names like "Quintuplets.Season01".
    EXCESS_SEASON.get_or_init(|| {
        Regex::new(r"[^A-Za-z]Season[ _-]?\d\d?").expect("invalid EXCESS_SEASON")
    })
}

/// Strip leading and trailing separator characters (space, underscore, dot, hyphen)
/// from an extracted show-name capture group.
/// Port of StringUtils.trimFoundShow().
fn trim_found_show(extracted: &str) -> String {
    extracted
        .trim_start_matches(|c: char| " _.-".contains(c))
        .trim_end_matches(|c: char| " _.-".contains(c))
        .to_string()
}

/// Case-insensitive version of Java's StringUtils.removeLast.
/// Finds the last occurrence of `needle` (compared case-insensitively) in `haystack`
/// and removes it. Only removes if idx > 0 (i.e. not at position 0), matching Java behaviour.
fn remove_last_ci(haystack: &str, needle: &str) -> String {
    let lower = haystack.to_lowercase();
    let needle_lower = needle.to_lowercase();
    if let Some(lower_byte_idx) = lower.rfind(&*needle_lower) {
        if lower_byte_idx > 0 {
            // Map byte-offset in `lower` to byte-offset in `haystack` via char count.
            // to_lowercase() can change byte lengths for non-ASCII chars,
            // so we cannot use the byte index directly on the original string.
            let chars_before = lower[..lower_byte_idx].chars().count();
            let needle_chars = needle_lower.chars().count();
            let haystack_start = haystack
                .char_indices()
                .nth(chars_before)
                .map(|(i, _)| i)
                .unwrap_or(haystack.len());
            let haystack_end = haystack
                .char_indices()
                .nth(chars_before + needle_chars)
                .map(|(i, _)| i)
                .unwrap_or(haystack.len());
            return format!("{}{}", &haystack[..haystack_start], &haystack[haystack_end..]);
        }
    }
    haystack.to_string()
}

/// Port of FilenameParser.stripJunk — removes "hdtv" and "dvdrip" junk tokens
/// from the combined show-string before pattern matching.
fn strip_junk(input: &str) -> String {
    let s = remove_last_ci(input, "hdtv");
    remove_last_ci(&s, "dvdrip")
}

/// Gets the last path component (directory name) of `path`, stripping any
/// "Season NN" suffix via EXCESS_SEASON.
/// Returns None if path has no file_name (e.g. filesystem root "/").
/// Port of FilenameParser.extractParentName().
fn extract_parent_name(path: &std::path::Path) -> Option<String> {
    let name = path.file_name()?.to_str()?;
    // e.g. "Quintuplets.Season01" → "Quintuplets"
    let stripped = excess_season().replace(name, "");
    Some(stripped.into_owned())
}

/// If the filename (last path component) starts with a season/episode pattern, prepends
/// the nearest non-season ancestor directory name.  Otherwise returns just the filename.
///
/// Port of FilenameParser.insertShowNameIfNeeded().
///
/// Skip conditions for parent directories (checked in order):
///   1. Name starts with "season" (case-insensitive)
///   2. Name matches DIR_LOOKS_LIKE_SEASON (^[sS][0-3]\d$), e.g. "s01", "S23"
///   3. Name equals DUPLICATES_DIRECTORY ("versions")
fn insert_show_name_if_needed(input: &str) -> String {
    let file_path = std::path::Path::new(input);

    let just_name = match file_path.file_name().and_then(|n| n.to_str()) {
        Some(n) => n,
        None => return input.to_string(),
    };

    if !filename_begins_with_season().is_match(just_name) {
        // Filename does not start with a season pattern; return it as-is (no path).
        return just_name.to_string();
    }

    // Climb parent directories until we find one that isn't a season/duplicates dir.
    let mut parent = file_path.parent();

    loop {
        let parent_name = match parent.and_then(extract_parent_name) {
            Some(n) => n,
            // Ran out of path components without finding a non-season dir.
            None => return just_name.to_string(),
        };

        let lower = parent_name.to_lowercase();
        let is_season_dir = lower.starts_with("season")
            || dir_looks_like_season().is_match(&parent_name)
            || parent_name == DUPLICATES_DIRECTORY;

        if !is_season_dir {
            return format!("{} {}", parent_name, just_name);
        }

        // This dir is a season/duplicates dir — move one level up.
        parent = parent.and_then(|p| p.parent());
    }
}

/// Parse a TV episode filename (or full path) and extract show name, season number,
/// episode number, and optional resolution.
///
/// Returns `None` if no pattern matches.
///
/// Port of FilenameParser.parseFilename().
pub fn parse_filename(input: &str) -> Option<ParseResult> {
    // Step 1: if filename starts with a season pattern, prepend the parent dir name.
    let with_show = insert_show_name_if_needed(input);

    // Step 2: strip common noise tokens ("hdtv", "dvdrip") so they don't confuse
    // the numeric-only fallback patterns (7 and 8).
    let stripped = strip_junk(&with_show);

    // Step 3: try each compiled pattern in order; first match wins.
    for (i, pattern) in compiled_patterns().iter().enumerate() {
        if let Some(caps) = pattern.captures(&stripped) {
            // Group 1: raw show name (always present in all 8 patterns)
            let show_name = trim_found_show(caps.get(1)?.as_str());

            // Groups 2 & 3: season and episode as strings; parse to u32
            // (leading zeros are dropped by parse(), matching Java's Integer.parseInt).
            let season: u32 = caps.get(2)?.as_str().parse().ok()?;
            let episode: u32 = caps.get(3)?.as_str().parse().ok()?;

            // Group 4 only exists in patterns 0–7 (compiled with RESOLUTION_SUFFIX).
            // Pattern index i < 8 means this is a "with resolution" pattern.
            let resolution = if i < 8 {
                caps.get(4).map(|m| m.as_str().to_string())
            } else {
                None
            };

            return Some(ParseResult {
                show_name,
                season,
                episode,
                resolution,
            });
        }
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn patterns_compile() {
        // Panics with a clear message if any pattern is invalid Rust regex syntax.
        assert_eq!(compiled_patterns().len(), 16);
    }

    #[test]
    fn trim_found_show_strips_separators() {
        assert_eq!(trim_found_show("  .Futurama. "), "Futurama");
        assert_eq!(trim_found_show("---Show---"), "Show");
        assert_eq!(trim_found_show("Show.Name"), "Show.Name"); // dots in middle untouched
        assert_eq!(trim_found_show("Offspring "), "Offspring"); // trailing space
    }

    #[test]
    fn strip_junk_removes_hdtv() {
        assert_eq!(
            strip_junk("one.tree.hill.s07e14.hdtv.xvid-fqm.avi"),
            "one.tree.hill.s07e14..xvid-fqm.avi"
        );
    }

    #[test]
    fn strip_junk_case_insensitive() {
        assert_eq!(
            strip_junk("Show.S01E01.HDTV.x264.mkv"),
            "Show.S01E01..x264.mkv"
        );
    }

    #[test]
    fn strip_junk_removes_dvdrip() {
        assert_eq!(
            strip_junk("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi"),
            "JAG.S10E01..XviD-P0W4DVD.avi"
        );
    }

    #[test]
    fn strip_junk_no_match_unchanged() {
        assert_eq!(
            strip_junk("Fargo.S01E01.x264-2HD.mp4"),
            "Fargo.S01E01.x264-2HD.mp4"
        );
    }

    #[test]
    fn remove_last_ci_not_at_zero() {
        // idx == 0 case: Java does NOT remove if found at position 0
        assert_eq!(remove_last_ci("hdtv.Show.S01E01", "hdtv"), "hdtv.Show.S01E01");
    }

    #[test]
    fn insert_show_name_flat_file() {
        // No path separator — just the filename. Not a season pattern.
        assert_eq!(
            insert_show_name_if_needed("Futurama.S07E14.HDTV.x264"),
            "Futurama.S07E14.HDTV.x264"
        );
    }

    #[test]
    fn insert_show_name_with_show_in_filename() {
        // filename starts with show name, not season → returned as-is (filename only)
        assert_eq!(
            insert_show_name_if_needed("Neighbours/neighbours.s23e233.pdtv.xvid-ss.txt"),
            "neighbours.s23e233.pdtv.xvid-ss.txt"
        );
    }

    #[test]
    fn insert_show_name_season_filename_one_deep() {
        assert_eq!(
            insert_show_name_if_needed("Quintuplets/S01E02.Quintagious.avi"),
            "Quintuplets S01E02.Quintagious.avi"
        );
    }

    #[test]
    fn insert_show_name_skips_versions_dir() {
        assert_eq!(
            insert_show_name_if_needed("Quintuplets/versions/S01E02.Quintagious.avi"),
            "Quintuplets S01E02.Quintagious.avi"
        );
    }

    #[test]
    fn insert_show_name_skips_season_dir() {
        assert_eq!(
            insert_show_name_if_needed("Quintuplets/Season01/S01E02.Quintagious.avi"),
            "Quintuplets S01E02.Quintagious.avi"
        );
    }

    #[test]
    fn insert_show_name_excess_season_stripped() {
        // "Quintuplets.Season01" → extract_parent_name strips ".Season01" → "Quintuplets"
        assert_eq!(
            insert_show_name_if_needed("Quintuplets.Season01/S01E02.Quintagious.avi"),
            "Quintuplets S01E02.Quintagious.avi"
        );
    }

    #[test]
    fn insert_show_name_absolute_path() {
        assert_eq!(
            insert_show_name_if_needed("/TV/Dexter/S05E05 First Blood.mkv"),
            "Dexter S05E05 First Blood.mkv"
        );
    }

    #[test]
    fn insert_show_name_numeric_only_season() {
        // "407" matches the numeric fallback in FILENAME_BEGINS_WITH_SEASON
        assert_eq!(
            insert_show_name_if_needed("/TV/Dexter/407.Slack.Tide.hdtv.x264-sys.mkv"),
            "Dexter 407.Slack.Tide.hdtv.x264-sys.mkv"
        );
    }

    // --- Pattern 1: SxxExx ---

    #[test]
    fn test_three_digits() {
        // S22E105 — episode 105 not 10
        let r = parse_filename("The.Daily.Show.S22E105.D.L.Hughley.HDTV.x264")
            .expect("should parse: The.Daily.Show.S22E105...");
        assert_eq!(r.show_name, "The.Daily.Show");
        assert_eq!(r.season, 22);
        assert_eq!(r.episode, 105);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_bug_20() {
        // path with parent dir — filename starts with show name, not season
        let r = parse_filename("Neighbours/neighbours.s23e233.pdtv.xvid-ss.txt")
            .expect("should parse: neighbours.s23e233...");
        assert_eq!(r.show_name, "neighbours");
        assert_eq!(r.season, 23);
        assert_eq!(r.episode, 233);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_not_three_digits() {
        // S07E14 — episode 14 not 142
        let r = parse_filename("Futurama.S07E14.2-D Blacktop.HDTV.x264")
            .expect("should parse: Futurama.S07E14...");
        assert_eq!(r.show_name, "Futurama");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 14);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_three_digit_season() {
        let r = parse_filename("House Hunters International.S103E02.mkv")
            .expect("should parse: House Hunters International.S103E02...");
        assert_eq!(r.show_name, "House Hunters International");
        assert_eq!(r.season, 103);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_long_three_digit_season() {
        // Pattern 2: Season-XX-Episode-XX
        let r = parse_filename("House Hunters International Season 103 Episode 2.mkv")
            .expect("should parse: ...Season 103 Episode 2...");
        assert_eq!(r.show_name, "House Hunters International");
        assert_eq!(r.season, 103);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_dot_three_digit_season() {
        // Pattern 3: sXX.eXX
        let r = parse_filename("House.Hunters.International.s103.e02.mkv")
            .expect("should parse: ...s103.e02...");
        assert_eq!(r.show_name, "House.Hunters.International");
        assert_eq!(r.season, 103);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_01() {
        // Pattern 4: SSxEE
        let r = parse_filename("game.of.thrones.5x01.mp4")
            .expect("should parse: game.of.thrones.5x01...");
        assert_eq!(r.show_name, "game.of.thrones");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_02() {
        // Pattern 3: sXX.eXX with resolution
        let r = parse_filename("24.s08.e01.720p.hdtv.x264-immerse.mkv")
            .expect("should parse: 24.s08.e01...");
        assert_eq!(r.show_name, "24");
        assert_eq!(r.season, 8);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_02a() {
        // filename starts with season (8x21) — parent "24" is prepended
        let r = parse_filename("MyShows/drama/widescreen/24/8x21.720p.hdtv.x264-immerse.mkv")
            .expect("should parse: .../24/8x21...");
        assert_eq!(r.show_name, "24");
        assert_eq!(r.season, 8);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_03() {
        // Pattern 3: sXX.eXX with resolution
        let r = parse_filename("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv")
            .expect("should parse: 24.S07.E18...");
        assert_eq!(r.show_name, "24");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 18);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_04() {
        // Pattern 5: title with 4-digit year (2010)
        let r = parse_filename("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv")
            .expect("should parse: human.target.2010...");
        assert_eq!(r.show_name, "human.target.2010");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_05() {
        // Pattern 7/8 fallback: SXXYY numeric (dexter.407)
        let r = parse_filename("dexter.407.720p.hdtv.x264-sys.mkv")
            .expect("should parse: dexter.407...");
        assert_eq!(r.show_name, "dexter");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_06() {
        let r = parse_filename("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi")
            .expect("should parse: JAG.S10E01...");
        assert_eq!(r.show_name, "JAG");
        assert_eq!(r.season, 10);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_07() {
        let r = parse_filename("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv")
            .expect("should parse: Lost.S06E05...");
        assert_eq!(r.show_name, "Lost");
        assert_eq!(r.season, 6);
        assert_eq!(r.episode, 5);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_08() {
        // show name with number: warehouse.13
        let r = parse_filename("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv")
            .expect("should parse: warehouse.13.s1e01...");
        assert_eq!(r.show_name, "warehouse.13");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_09() {
        let r = parse_filename("one.tree.hill.s07e14.hdtv.xvid-fqm.avi")
            .expect("should parse: one.tree.hill.s07e14...");
        assert_eq!(r.show_name, "one.tree.hill");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 14);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_10() {
        let r = parse_filename("gossip.girl.s03e15.hdtv.xvid-fqm.avi")
            .expect("should parse: gossip.girl.s03e15...");
        assert_eq!(r.show_name, "gossip.girl");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 15);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_11() {
        let r = parse_filename("smallville.s09e14.hdtv.xvid-xii.avi")
            .expect("should parse: smallville.s09e14...");
        assert_eq!(r.show_name, "smallville");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 14);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_12() {
        let r = parse_filename("smallville.s09e15.hdtv.xvid-2hd.avi")
            .expect("should parse: smallville.s09e15...");
        assert_eq!(r.show_name, "smallville");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 15);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_13() {
        let r = parse_filename("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv")
            .expect("should parse: the.big.bang.theory.s03e18...");
        assert_eq!(r.show_name, "the.big.bang.theory");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 18);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_14() {
        // Pattern 5: castle.2009 — year is part of show name
        let r = parse_filename("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv")
            .expect("should parse: castle.2009.s01e09...");
        assert_eq!(r.show_name, "castle.2009");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 9);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_15() {
        // Filename starts with season — parent "Dexter" prepended
        let r = parse_filename("/TV/Dexter/S05E05 First Blood.mkv")
            .expect("should parse: /TV/Dexter/S05E05...");
        assert_eq!(r.show_name, "Dexter");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 5);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_16() {
        // Pattern 4: SSxEE via parent dir prepend
        let r = parse_filename("/TV/Lost/Lost [2x07].mkv")
            .expect("should parse: /TV/Lost/Lost [2x07]...");
        assert_eq!(r.show_name, "Lost [");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_161() {
        // filename IS season (2x07) — parent "Lost" prepended
        let r = parse_filename("/TV/Lost/2x07.mkv")
            .expect("should parse: /TV/Lost/2x07...");
        assert_eq!(r.show_name, "Lost");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_17() {
        let r = parse_filename("American.Dad.S09E17.HDTV.x264-2HD.mp4")
            .expect("should parse: American.Dad.S09E17...");
        assert_eq!(r.show_name, "American.Dad");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 17);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_18() {
        let r = parse_filename("Californication.S07E04.HDTV.x264-2HD.mp4")
            .expect("should parse: Californication.S07E04...");
        assert_eq!(r.show_name, "Californication");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_19() {
        let r = parse_filename("Continuum.S03E07.HDTV.x264-2HD.mp4")
            .expect("should parse: Continuum.S03E07...");
        assert_eq!(r.show_name, "Continuum");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_20() {
        let r = parse_filename("Elementary.S02E23.HDTV.x264-LOL.mp4")
            .expect("should parse: Elementary.S02E23...");
        assert_eq!(r.show_name, "Elementary");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 23);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_21() {
        let r = parse_filename("Family.Guy.S12E19.HDTV.x264-2HD.mp4")
            .expect("should parse: Family.Guy.S12E19...");
        assert_eq!(r.show_name, "Family.Guy");
        assert_eq!(r.season, 12);
        assert_eq!(r.episode, 19);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_22() {
        let r = parse_filename("Fargo.S01E01.HDTV.x264-2HD.mp4")
            .expect("should parse: Fargo.S01E01...");
        assert_eq!(r.show_name, "Fargo");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_23() {
        let r = parse_filename("Girls.S03E11.HDTV.x264-KILLERS.mp4")
            .expect("should parse: Girls.S03E11...");
        assert_eq!(r.show_name, "Girls");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 11);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_24() {
        let r = parse_filename("Grimm.S03E19.HDTV.x264-LOL.mp4")
            .expect("should parse: Grimm.S03E19...");
        assert_eq!(r.show_name, "Grimm");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 19);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_25() {
        // Pattern 5: year in title
        let r = parse_filename("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4")
            .expect("should parse: House.Of.Cards.2013.S01E06...");
        assert_eq!(r.show_name, "House.Of.Cards.2013");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 6);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_26() {
        let r = parse_filename("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4")
            .expect("should parse: Modern.Family.S05E12...");
        assert_eq!(r.show_name, "Modern.Family");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 12);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_27() {
        let r = parse_filename("New.Girl.S03E23.HDTV.x264-LOL.mp4")
            .expect("should parse: New.Girl.S03E23...");
        assert_eq!(r.show_name, "New.Girl");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 23);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_28() {
        let r = parse_filename("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4")
            .expect("should parse: Nurse.Jackie.S06E04...");
        assert_eq!(r.show_name, "Nurse.Jackie");
        assert_eq!(r.season, 6);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_29() {
        let r = parse_filename("Offspring - S05E01.mp4")
            .expect("should parse: Offspring - S05E01...");
        assert_eq!(r.show_name, "Offspring");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_30() {
        // Pattern 5: year in title
        let r = parse_filename("Reign.2013.S01E20.HDTV.x264-2HD.mp4")
            .expect("should parse: Reign.2013.S01E20...");
        assert_eq!(r.show_name, "Reign.2013");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 20);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_31() {
        let r = parse_filename("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4")
            .expect("should parse: Robot.Chicken.S07E04...");
        assert_eq!(r.show_name, "Robot.Chicken");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_32() {
        let r = parse_filename("Supernatural.S09E21.HDTV.x264-LOL.mp4")
            .expect("should parse: Supernatural.S09E21...");
        assert_eq!(r.show_name, "Supernatural");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_33() {
        // Pattern 5: year in title
        let r = parse_filename("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4")
            .expect("should parse: The.Americans.2013.S02E10...");
        assert_eq!(r.show_name, "The.Americans.2013");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 10);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_34() {
        let r = parse_filename("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4")
            .expect("should parse: The.Big.Bang.Theory.S07E23...");
        assert_eq!(r.show_name, "The.Big.Bang.Theory");
        assert_eq!(r.season, 7);
        assert_eq!(r.episode, 23);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_35() {
        let r = parse_filename("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4")
            .expect("should parse: The.Good.Wife.S05E20...");
        assert_eq!(r.show_name, "The.Good.Wife");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 20);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_36() {
        let r = parse_filename("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4")
            .expect("should parse: The.Walking.Dead.S04E16...");
        assert_eq!(r.show_name, "The.Walking.Dead");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 16);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_37() {
        let r = parse_filename("Veep.S03E05.HDTV.x264-KILLERS.mp4")
            .expect("should parse: Veep.S03E05...");
        assert_eq!(r.show_name, "Veep");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 5);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_38() {
        let r = parse_filename("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4")
            .expect("should parse: Witches.of.East.End.S01E01...");
        assert_eq!(r.show_name, "Witches.of.East.End");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_39() {
        // show name with number: Warehouse.13
        let r = parse_filename("Warehouse.13.S05E04.HDTV.x264-2HD.mp4")
            .expect("should parse: Warehouse.13.S05E04...");
        assert_eq!(r.show_name, "Warehouse.13");
        assert_eq!(r.season, 5);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_40() {
        // Pattern 6: SXXYY fallback — the.100.208
        let r = parse_filename("the.100.208.hdtv-lol.mp4")
            .expect("should parse: the.100.208...");
        assert_eq!(r.show_name, "the.100");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 8);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_41() {
        // Pattern 4: SSxEE
        let r = parse_filename("firefly.1x01.hdtv-lol.mp4")
            .expect("should parse: firefly.1x01...");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_42() {
        let r = parse_filename("firefly.1x02.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_43() {
        let r = parse_filename("firefly.1x03.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_44() {
        let r = parse_filename("firefly.1x04.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_45() {
        let r = parse_filename("firefly.1x05.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 5);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_46() {
        let r = parse_filename("firefly.1x06.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 6);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_47() {
        let r = parse_filename("firefly.1x07.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_48() {
        let r = parse_filename("firefly.1x08.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 8);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_49() {
        let r = parse_filename("firefly.1x09.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 9);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_50() {
        let r = parse_filename("firefly.1x10.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 10);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_51() {
        let r = parse_filename("firefly.1x11.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 11);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_52() {
        let r = parse_filename("firefly.1x12.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 12);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_53() {
        let r = parse_filename("firefly.1x13.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 13);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_54() {
        let r = parse_filename("firefly.1x14.hdtv-lol.mp4").expect("should parse");
        assert_eq!(r.show_name, "firefly");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 14);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_55() {
        let r = parse_filename("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv")
            .expect("should parse: Strike.Back.S01E01...");
        assert_eq!(r.show_name, "Strike.Back");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_56() {
        // Pattern 6: SXXYY — law.and.order.svu.1705
        let r = parse_filename("law.and.order.svu.1705.hdtv-lol")
            .expect("should parse: law.and.order.svu.1705...");
        assert_eq!(r.show_name, "law.and.order.svu");
        assert_eq!(r.season, 17);
        assert_eq!(r.episode, 5);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_57() {
        // Pattern 6: SXXYY — ncis.1304
        let r = parse_filename("ncis.1304.hdtv-lol")
            .expect("should parse: ncis.1304...");
        assert_eq!(r.show_name, "ncis");
        assert_eq!(r.season, 13);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_58() {
        // S.H.I.E.L.D. — acronym with dots in show name
        let r = parse_filename("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET")
            .expect("should parse: Marvels.Agents.of.S.H.I.E.L.D.S03E03...");
        assert_eq!(r.show_name, "Marvels.Agents.of.S.H.I.E.L.D");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_59() {
        let r = parse_filename("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS")
            .expect("should parse: Marvels.Agents.of.S.H.I.E.L.D.S03E10...");
        assert_eq!(r.show_name, "Marvels.Agents.of.S.H.I.E.L.D");
        assert_eq!(r.season, 3);
        assert_eq!(r.episode, 10);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_60() {
        let r = parse_filename("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv")
            .expect("should parse: Nip.Tuck.S06E01...");
        assert_eq!(r.show_name, "Nip.Tuck");
        assert_eq!(r.season, 6);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_61() {
        let r = parse_filename("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv")
            .expect("should parse: The.Big.Bang.Theory.S10E04...");
        assert_eq!(r.show_name, "The.Big.Bang.Theory");
        assert_eq!(r.season, 10);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_62() {
        let r = parse_filename("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv")
            .expect("should parse: Lucifer.S02E03...");
        assert_eq!(r.show_name, "Lucifer");
        assert_eq!(r.season, 2);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_63() {
        let r = parse_filename("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv")
            .expect("should parse: ...S04E03.1080p...");
        assert_eq!(r.show_name, "Marvels.Agents.of.S.H.I.E.L.D");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, Some("1080p".to_string()));
    }

    #[test]
    fn test_64() {
        let r = parse_filename("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv")
            .expect("should parse: Supernatural.S11E22.1080p...");
        assert_eq!(r.show_name, "Supernatural");
        assert_eq!(r.season, 11);
        assert_eq!(r.episode, 22);
        assert_eq!(r.resolution, Some("1080p".to_string()));
    }

    #[test]
    fn test_65() {
        // resolution appears AFTER codec string
        let r = parse_filename("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv")
            .expect("should parse: Supernatural.S11E22...720p...");
        assert_eq!(r.show_name, "Supernatural");
        assert_eq!(r.season, 11);
        assert_eq!(r.episode, 22);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    #[test]
    fn test_66() {
        let r = parse_filename("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv")
            .expect("should parse: Channel.Zero.S01E01.480p...");
        assert_eq!(r.show_name, "Channel.Zero");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 1);
        assert_eq!(r.resolution, Some("480p".to_string()));
    }

    #[test]
    fn test_67() {
        let r = parse_filename("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv")
            .expect("should parse: NCIS.S14E04...");
        assert_eq!(r.show_name, "NCIS");
        assert_eq!(r.season, 14);
        assert_eq!(r.episode, 4);
        assert_eq!(r.resolution, Some("720p".to_string()));
    }

    // --- Quintuplets parent-dir traversal suite ---
    // All 8 variants exercise insert_show_name_if_needed path-climbing logic.

    #[test]
    fn test_68() {
        // flat filename — no dir traversal needed
        let r = parse_filename("Quintuplets.S01E02.Quintagious.avi")
            .expect("should parse: Quintuplets.S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_69() {
        // one dir deep; filename starts with S01E02 → climb to "Quintuplets"
        let r = parse_filename("Quintuplets/S01E02.Quintagious.avi")
            .expect("should parse: Quintuplets/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_70() {
        // "versions" is DUPLICATES_DIRECTORY — skip it, climb to "Quintuplets"
        let r = parse_filename("Quintuplets/versions/S01E02.Quintagious.avi")
            .expect("should parse: Quintuplets/versions/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_71() {
        // "~2" version suffix is noise — parser ignores it via .*
        let r = parse_filename("Quintuplets/versions/S01E02.Quintagious~2.avi")
            .expect("should parse: Quintuplets/versions/S01E02.Quintagious~2...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_72() {
        // "Season1" starts with "season" → skip; "versions" is DUPLICATES_DIR → skip
        let r = parse_filename("Quintuplets/Season1/versions/S01E02.Quintagious~9.avi")
            .expect("should parse: Quintuplets/Season1/versions/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_73() {
        // "Season01" → skip; "versions" → skip
        let r = parse_filename("Quintuplets/Season01/versions/S01E02.Quintagious~4.avi")
            .expect("should parse: Quintuplets/Season01/versions/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_74() {
        // "Quintuplets.Season01" → EXCESS_SEASON strips ".Season01" → "Quintuplets"
        let r = parse_filename("Quintuplets.Season01/S01E02.Quintagious.avi")
            .expect("should parse: Quintuplets.Season01/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_75() {
        // "s01" matches DIR_LOOKS_LIKE_SEASON → skip; "Quintuplets" is the answer
        let r = parse_filename("Quintuplets/s01/1x02.Quintagious.avi")
            .expect("should parse: Quintuplets/s01/1x02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_76() {
        // filename starts with "01x02" (season pattern) → parent "Quintuplets" prepended
        let r = parse_filename("Quintuplets/01x02.Quintagious.avi")
            .expect("should parse: Quintuplets/01x02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_77() {
        // deep path; "Season01" → skip; "Quintuplets" found
        let r = parse_filename("Videos/TVShows/Fullscreen/LiveAction/Quintuplets/Season01/S01E02.Quintagious.avi")
            .expect("should parse: .../Quintuplets/Season01/S01E02...");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_78() {
        // complex path: skip "season1", "versions", "s01", then EXCESS_SEASON strips ".Season01"
        let r = parse_filename("Quintuplets/Quintuplets.Season01/s01/versions/season1/S01E02.Quintagious~7.avi")
            .expect("should parse: complex nested Quintuplets path");
        assert_eq!(r.show_name, "Quintuplets");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    // --- Date-in-filename suite ---

    #[test]
    fn test_79() {
        // Pattern 1 wins; date "September.22.1991" is noise consumed by .*
        let r = parse_filename("AFV.S01E03.September.22.1991.mp4")
            .expect("should parse: AFV.S01E03...");
        assert_eq!(r.show_name, "AFV");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_80() {
        let r = parse_filename("AFV.S01E03.September.22.1991.1991.09.22.mp4")
            .expect("should parse: AFV.S01E03 with double date");
        assert_eq!(r.show_name, "AFV");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 3);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_81() {
        // year "2003" inside episode title — Pattern 1 wins before Pattern 5 tries
        let r = parse_filename("The Big Bang Theory - S04E09 - The 2003 Approximation.mkv")
            .expect("should parse: The Big Bang Theory - S04E09...");
        assert_eq!(r.show_name, "The Big Bang Theory");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 9);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_82() {
        // trailing air-date "2015-10-12" is noise
        let r = parse_filename("The Big Bang Theory - S04E09 - The 2003 Approximation - 2015-10-12.mkv")
            .expect("should parse: S04E09 with trailing date");
        assert_eq!(r.show_name, "The Big Bang Theory");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 9);
        assert_eq!(r.resolution, None);
    }

    // --- Archer (year disambiguation) suite ---

    #[test]
    fn test_83() {
        // Pattern 5: "Archer.2009" — year stays in show name
        let r = parse_filename("Archer.2009.S01E02.Training.Day.mp4")
            .expect("should parse: Archer.2009.S01E02...");
        assert_eq!(r.show_name, "Archer.2009");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_84() {
        // "Archer (2009)" in parent dir; filename starts with S01E02
        let r = parse_filename("Archer (2009)/S01E02 Training Day.mp4")
            .expect("should parse: Archer (2009)/S01E02...");
        assert_eq!(r.show_name, "Archer (2009)");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_85() {
        // full show name in filename with air-date at end
        let r = parse_filename("Archer (2009)/Archer (2009) S01E02 Training Day 2010.01.14.mp4")
            .expect("should parse: Archer (2009) S01E02...");
        assert_eq!(r.show_name, "Archer (2009)");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_86() {
        let r = parse_filename("Archer (2009)/S01E02 Training Day 2010.01.14.mp4")
            .expect("should parse: Archer (2009)/S01E02 with date");
        assert_eq!(r.show_name, "Archer (2009)");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_87() {
        let r = parse_filename("Archer.2009.S01E02.Training.Day.2010.01.14.mp4")
            .expect("should parse: Archer.2009.S01E02 with trailing date");
        assert_eq!(r.show_name, "Archer.2009");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_88() {
        let r = parse_filename("Archer (2009) - S01E02 - Training Day - 2010.01.14.mp4")
            .expect("should parse: Archer (2009) - S01E02...");
        assert_eq!(r.show_name, "Archer (2009)");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_89() {
        let r = parse_filename("Archer (2009) S01E02 Training Day 2010.01.14.mp4")
            .expect("should parse: Archer (2009) S01E02 without dash");
        assert_eq!(r.show_name, "Archer (2009)");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_90() {
        let r = parse_filename("Archer.2009.S01E02.mp4")
            .expect("should parse: Archer.2009.S01E02 minimal");
        assert_eq!(r.show_name, "Archer.2009");
        assert_eq!(r.season, 1);
        assert_eq!(r.episode, 2);
        assert_eq!(r.resolution, None);
    }

    // --- Cheers air-date suite ---

    #[test]
    fn test_91() {
        let r = parse_filename("Cheers.S09E21.Its.A.Wonderful.Wife.avi")
            .expect("should parse: Cheers.S09E21...");
        assert_eq!(r.show_name, "Cheers");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_92() {
        let r = parse_filename("Cheers - S09E21 - It's a Wonderful Wife - 1991.02.28.avi")
            .expect("should parse: Cheers - S09E21 with air date");
        assert_eq!(r.show_name, "Cheers");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_93() {
        let r = parse_filename("Cheers S09E21 It's a Wonderful Wife 1991.02.28.avi")
            .expect("should parse: Cheers S09E21 no dashes");
        assert_eq!(r.show_name, "Cheers");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_94() {
        let r = parse_filename("Cheers.S09E21.Its.A.Wonderful.Wife.1991.02.28.avi")
            .expect("should parse: Cheers.S09E21 with dotted date");
        assert_eq!(r.show_name, "Cheers");
        assert_eq!(r.season, 9);
        assert_eq!(r.episode, 21);
        assert_eq!(r.resolution, None);
    }

    #[test]
    fn test_95() {
        // Pattern 8 (last resort): Dexter/407 — filename starts with season, parent prepended
        let r = parse_filename("/TV/Dexter/407.Slack.Tide.hdtv.x264-sys.mkv")
            .expect("should parse: /TV/Dexter/407...");
        assert_eq!(r.show_name, "Dexter");
        assert_eq!(r.season, 4);
        assert_eq!(r.episode, 7);
        assert_eq!(r.resolution, None);
    }
}
