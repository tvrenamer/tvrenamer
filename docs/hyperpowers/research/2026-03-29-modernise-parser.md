# Research: Filename Parser Module (Rust Port)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Port all 8 filename parsing regex patterns from Java to Rust, preserving identical behaviour for all 95 existing test cases. The parser extracts show name, season number, episode number, and optional resolution from TV episode filenames.

---

## The 8 Regex Patterns (Verbatim from Java)

Located at `src/main/java/org/tvrenamer/controller/FilenameParser.java:26-52`:

```java
private static final String[] REGEX = {
    // Pattern 1: SxxExx (e.g., "Show.S01E05")
    "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d*)[eE](\\d\\d*).*",

    // Pattern 2: "Season-XX-Episode-XX"
    "(.+?[^a-zA-Z0-9]\\D*?)Season[- ](\\d\\d*)[- ]?Episode[- ](\\d\\d*).*",

    // Pattern 3: "sXX.eXX" flexible separators
    "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d*)\\D*?[eE](\\d\\d*).*",

    // Pattern 4: "SSxEE" with optional "S"
    "(.+[^a-zA-Z0-9]\\D*?)[Ss](\\d\\d?)x(\\d\\d\\d?).*",

    // Pattern 5: titles with 4-digit year
    "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*",

    // Pattern 6: "SXXYY" (exactly 4 digits: season+episode)
    "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d)(\\d\\d)\\D.*",

    // Pattern 7: Fallback — show name + two 1-2 digit numbers
    "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*",

    // Pattern 8: Last resort — minimal structure
    "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*"
};
```

Each pattern is compiled twice: once with a resolution suffix regex (`\\D(\\d+[pk]).*`) and once without, giving 16 compiled patterns total. Patterns are tried in order; first match wins.

**Rust regex crate compatibility:** All 8 patterns use standard character classes and quantifiers only — no lookahead/lookbehind. They translate directly to the Rust `regex` crate. Use named capture groups for clarity: `(?P<show>...)(?P<season>...)(?P<episode>...)`.

---

## Representative Test Cases

The full 95-case test suite is in `src/test/java/org/tvrenamer/controller/FilenameParserTest.java`. Key cases covering each pattern:

| Input Filename | Expected Show | Season | Episode | Resolution |
|----------------|--------------|--------|---------|------------|
| `The.Daily.Show.S22E105.D.L.Hughley.HDTV.x264` | `The.Daily.Show` | 22 | 105 | — |
| `Futurama.S07E14.2-D Blacktop.HDTV.x264` | `Futurama` | 7 | 14 | — |
| `House Hunters International.S103E02.mkv` | `House Hunters International` | 103 | 2 | — |
| `24.s08.e01.720p.hdtv.x264-immerse.mkv` | `24` | 8 | 1 | 720p |
| `dexter.407.720p.hdtv.x264-sys.mkv` | `dexter` | 4 | 7 | 720p |
| `game.of.thrones.5x01.mp4` | `game.of.thrones` | 5 | 1 | — |
| `human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv` | `human.target.2010` | 1 | 2 | 720p |
| `castle.2009.s01e09.720p.hdtv.x264-ctu.mkv` | `castle.2009` | 1 | 9 | 720p |
| `the.100.208.hdtv-lol.mp4` | `the.100` | 2 | 8 | — |
| `Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET` | `Marvels.Agents.of.S.H.I.E.L.D` | 3 | 3 | — |
| `law.and.order.svu.1705.hdtv-lol` | `law.and.order.svu` | 17 | 5 | — |
| `Archer.2009.S01E02.Training.Day.mp4` | `Archer.2009` | 1 | 2 | — |
| `Archer (2009)/S01E02 Training Day.mp4` | `Archer (2009)` | 1 | 2 | — |
| `Quintuplets/versions/S01E02.Quintagious~2.avi` | `Quintuplets` | 1 | 2 | — |

All 95 cases must become Rust `#[test]` functions. Port inputs and expected outputs verbatim from the Java file.

---

## Parser Edge Cases

1. **Shows with numbers in name** (`the.100`, `warehouse.13`): Patterns 7 and 8 are the fallback for these; they must be tested explicitly. Pattern priority order matters.
2. **Shows with year disambiguation** (`castle.2009`, `human.target.2010`): Pattern 5 handles these. The year becomes part of the extracted show name — the overrides system is meant to strip it (but is currently orphaned; see preferences module).
3. **Three-digit episodes** (`S22E105` for The Daily Show): Pattern 1 handles `\d\d*` (one or more digits) — already accounted for.
4. **Three-digit seasons** (`S103E02` for House Hunters International): Added in 2018 (commit 5d664dc); all patterns use `\d\d*` or `\d\d?` — verify Rust regex handles this identically.
5. **Filenames inside deeply nested paths** (`Quintuplets/versions/S01E02.Quintagious~2.avi`): The parser must handle `~2` version suffixes as extraneous noise; confirmed by 8 test cases.
6. **TMDB special episodes (Season 0)**: Not handled by any pattern — the parser doesn't extract Season 0 from typical filenames. Known gap; document but don't fix now.

---

## Git History

- Patterns grew incrementally as real-world edge cases were encountered.
- Added 3-digit season support in 2018 (commit 5d664dc) for shows like House Hunters International (S103E02).
- Added numeric-only pattern in 2017 (commit a93031b) for filenames like `dexter.407.mp4`.
- The test suite grew in parallel — this is a well-validated component.

---

## Rust Module Design

```rust
// src-tauri/src/parser/mod.rs
pub mod patterns;
pub mod filename;

// src-tauri/src/parser/filename.rs
pub struct ParseResult {
    pub show_name: String,
    pub season: u32,
    pub episode: u32,
    pub resolution: Option<String>,
}

pub fn parse_filename(filename: &str) -> Option<ParseResult> { ... }
```

Compile all 16 patterns (8 base + 8 with resolution suffix) using `std::sync::OnceLock` for lazy initialisation. Try patterns in order; first match wins.

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| Java patterns use no lookahead/lookbehind | ⚠️ Appears true — verify each of the 8 patterns before porting. Use `fancy_regex` if any requires lookaround. |
| Rust `regex` crate handles all pattern syntax | ⚠️ Unverified — test each pattern individually |
