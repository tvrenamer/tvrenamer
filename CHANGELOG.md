# Changelog

## 1.0

Double-click installation is back on macOS and Windows. The `.dmg` and the
`.exe` carry their own Java runtime, so there is nothing to install first.

### You need to know

* **The bundles include Java.** That is why they are around 50MB against the
  zip's 7MB. You do not need your own Java to run them.
* **The zip still ships for every platform.** If you already have Java 21 it is
  the smaller download, and nothing about it has changed.
* **The macOS bundle is signed and notarised**, so it opens without the warning
  the zip gives.
* **Windows will warn you.** The `.exe` is not signed yet, so SmartScreen shows
  a warning. Choose 'More info', then 'Run anyway'.
* **Linux stays zip-only.**

### Fixed

* Saving preferences failed with "No converter available" on Java 17 and later,
  and once a preferences file existed the application died on startup before it
  could show a window. Both came from XStream reflecting over classes that the
  module system and XStream's own defaults refuse to open.

## 1.0b6

Fixes two preferences faults in 1.0b5. **Upgrade if you are on 1.0b5**, and
especially if you upgraded to it from 0.8 or 1.0b4.

* If you already had a preferences file from an earlier release, 1.0b5 died
  before drawing a window. XStream has refused to build any type not explicitly
  allowed since 1.4.18, so reading the file threw from a static initialiser and
  took the application with it. Nothing was wrong with your preferences file,
  and this release reads it normally.
* Saving preferences failed on every supported JDK. `java.base` does not open
  `java.util` to code on the classpath, and XStream needs that to reach a field
  on the deprecated `Observable` class that `UserPreferences` extends. Any
  setting you changed in 1.0b5 was lost when you quit.

Neither fault showed up in testing because no machine running the tests had ever
saved a preferences file. The test task now runs with the same JVM arguments as
the launcher, so a failure like this cannot pass again.

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
* The update check compared version numbers as text, so it ranked `1.0b5` above
  `1.0` and `0.10` below `0.9`. Every 1.0 beta would have refused to see the 1.0
  release. Versions are now compared as numbers, with a beta sorting before the
  release it leads up to.
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
