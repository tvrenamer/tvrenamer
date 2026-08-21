# Changelog

## 1.0b5

TVRenamer works again. TheTVDB retired the version 1 API it depended on, and
every lookup had been returning nothing, so no file could be renamed. Episode
data now comes from [TVmaze](https://www.tvmaze.com/).

The build was equally stuck: it used Gradle configurations removed in Gradle 7
and pinned SWT to an Intel-only macOS binary. It now builds and runs on current
JDKs, on Apple silicon, and on all three platforms in CI.

### You need to know

* **Java 21 or later is required.** Earlier releases ran on Java 8. Check with
  `java -version` before installing.
* **You need your own Java.** The old `.exe` and `.app` bundles were produced by
  an Ant build that no longer runs on any current JDK. Each download is now a
  plain zip with launcher scripts in `bin/`. Repackaging with `jpackage` is not
  done yet.
* **32-bit builds are gone.** Eclipse stopped shipping 32-bit SWT natives after
  3.108.0 in 2018, so there is no way to build a 32-bit Windows or Linux
  version against a supported SWT. If you need 32-bit, stay on
  [v1.0b4](https://github.com/tvrenamer/tvrenamer/releases/tag/v1.0b4).
* **The DVD episode ordering preference no longer does anything.** TVmaze
  publishes only the aired ordering, so that setting falls back to aired
  placement. It was never exposed in the UI.

### Fixed

* Episode lookups resolve again, against TVmaze.
* On a display smaller than the window, the top left corner no longer lands off
  screen with the menu bar and buttons out of reach. The window is also placed
  in the monitor's client area, so it no longer opens under the taskbar, dock or
  menu bar.

### Build and CI

* Builds with the current Gradle wrapper on any JDK from 21 up, targeting 21.
* SWT 3.130.0, with the native library chosen to match the build machine.
* GitHub Actions builds on Linux, macOS and Windows, then launches the GUI and
  photographs it, which is the only check that catches a missing native library.
* An end-to-end rename test covers the path from filename to renamed file.
