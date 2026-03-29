# Research: CI/CD & Distribution (GitHub Actions + Tauri)

> Generated: 2026-03-29
> Source: `docs/hyperpowers/research/2026-03-29-modernise-stack.md`

---

## Goal

Set up CI/CD with GitHub Actions using `tauri-action` for cross-platform builds, and replace the legacy tvrenamer.org version checker with GitHub Releases + Tauri's built-in updater plugin.

---

## Build Targets

| Platform | Format | Expected Size |
|----------|--------|---------------|
| macOS | `.dmg` with signed `.app` | ~12MB |
| Windows | `.msi` installer | ~12MB |
| Linux | `.deb`, `.rpm`, `.AppImage` | ~12MB |

Compared to current Java builds: ~80MB+ with bundled JRE.

---

## GitHub Actions: `tauri-action`

Tauri's official `tauri-action` workflow supports parallel cross-platform builds from a single workflow file. It also auto-generates and uploads `latest.json` for the updater plugin and signs all platform artifacts.

---

## Update Checker: GitHub Releases + Tauri Updater Plugin

**Replaces:** `UpdateChecker.java` which polls `http://tvrenamer.org/version` (returns plain-text "0.8", requires manual updates after each release).

**Tauri updater config** in `tauri.conf.json`:
```json
{
  "plugins": {
    "updater": {
      "active": true,
      "pubkey": "YOUR_PUBLIC_KEY",
      "endpoints": [
        "https://github.com/tvrenamer/tvrenamer/releases/latest/download/latest.json"
      ]
    }
  }
}
```

**Key generation:** `npm run tauri signer generate`

The `tauri-action` GitHub Action auto-generates `latest.json` and signs all platform artifacts. Zero custom version comparison code needed.

---

## Git History: Update Checker

- **2010-10-18**: Initial check against `http://r.ac.nz/tvrenamer.version` (commit c712a72)
- **2017-04-21**: Refactored to `TVRENAMER_VERSION_URL = "http://tvrenamer.org/version"` (commit f2a1d39)
- **Status now**: tvrenamer.org is still live, returns "0.8". Requires manual updates — no automation.

The move to GitHub Releases + Tauri updater eliminates all manual maintenance.

---

## Validated Assumptions

| Assumption | Status |
|------------|--------|
| Tauri updater config lives in `tauri.conf.json` → `"plugins": {"updater": {...}}` | ✅ Valid |
| `tauri-action` auto-generates and uploads `latest.json` | ✅ Valid |
| `tauri-action` supports parallel cross-platform CI | ✅ Valid |
