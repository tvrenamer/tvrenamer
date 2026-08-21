# Native bundles for 1.0

## Context

v1.0b5 ships as a plain Gradle `distZip` per platform. Users must install Java 21
first, then find `bin/tvrenamer` inside the unpacked directory. Releases used to
ship a `.app` and a `.exe`, but that packaging came from an Ant build that no
longer runs on any current JDK, and it was lost in commit `fde37d5`.

The goal is double-click installation on macOS and Windows with no separate Java
install, keeping the small zip for people who already have Java and would rather
not download 44MB.

Targeted at 1.0 final, not another beta.

## Measured, not assumed

| | Size |
| --- | --- |
| Existing zip | 7MB |
| Full-JDK app-image | 170MB |
| Trimmed app-image | 67MB |
| Trimmed `.dmg` | 44MB |

The trimmed runtime needs six modules:

    java.base, java.desktop, java.logging, java.sql, jdk.unsupported, jdk.crypto.ec

`jdeps` reports only four. `java.logging` and `jdk.crypto.ec` load reflectively,
and without `jdk.crypto.ec` the HTTPS handshake to tvmaze.com fails, so the app
starts but cannot look up a single episode. Running the tests on the trimmed
runtime is the only thing that catches this.

44MB is near the floor. `java.desktop` is the bulk and cannot go: SWT, XStream
and TVRenamer all reference it. Dropping `java.sql` saves nothing measurable.

## Decisions

1. Native bundles only for final tags. jpackage rejects `1.0b5`, so a version
   containing `b` ships zips only. No version mapping.
2. The zip keeps shipping on every platform alongside any bundle.
3. macOS `.dmg` signed with a Developer ID Application certificate, and notarized.
4. Windows `.exe` unsigned for now.
5. Linux stays zip-only.

## Settled by inspecting a bundle built during planning

* **Classpath needs no change.** jpackage enumerates all 15 `--input` jars into
  `app.classpath` in the generated `.cfg`. No `Class-Path` manifest entry needed.
* **`--mac-app-image-sign-identity` does not exist in JDK 21.** Only
  `--mac-signing-key-user-name` and `--mac-entitlements`. Use the former.
* **No application code changes needed.** Every write goes to `~/.tvrenamer` or
  the temp dir; nothing writes into the install directory and no log file is
  created. `tvrenamer.version`, `icons/tvrenamer.png` and `logging.properties`
  are all in the packaged jar, so the classpath lookups resolve and the
  `ICON_PARENT_DIRECTORY` filesystem fallback at `UIStarter.java:70` never fires.
  The relative `etc/default-overrides.xml` seed at `UserPreferences.java:100`
  already does nothing in any released build, since `etc/` has never been in the
  zip. Leave both alone.

## The signing trap

SWT does not load its natives from the bundle. It extracts them from its jar to
`~/.swt/lib/macosx/<arch>/` on first run and loads them from there. Those are
signed by **Eclipse Foundation, Inc. (Team ID JCDTMS22B4)**, confirmed with
`codesign -dvvv`.

Notarization requires the hardened runtime, whose library validation refuses code
signed by another team. So a signed, notarized build would launch and die on the
SWT load, and only on a machine other than the one that built it, with an empty
`~/.swt`.

jpackage's default entitlements grant
`com.apple.security.cs.disable-library-validation`, which is what saves this.
Verified by extracting `entitlements.plist` from `jdk.jpackage.jmod`. That copy
contains no `com.apple.security.get-task-allow`, which would cause outright
notary rejection, but it is a JBR build and CI uses Temurin, so check it there.

Supply `etc/entitlements.plist` via `--mac-entitlements` with only:

    com.apple.security.cs.allow-jit
    com.apple.security.cs.allow-unsigned-executable-memory
    com.apple.security.cs.disable-library-validation

The first two the JVM needs, the third the Eclipse-signed SWT libraries need. Do
not drop the third whatever a linter says. This also sheds two entitlements
jpackage grants by default that TVRenamer has no use for, `cs.debugger` and
`device.audio-input`.

## Implementation

Packaging in `build.gradle`, credentials in `.github/workflows/release.yml`.
`build.gradle` already computes `osArch` and `platformLabel`, so putting jpackage
there avoids a second copy of the platform logic in two shell dialects, and lets
a developer run it locally, which is how every number above was measured.
Keychains and Apple credentials have no business in a build file.

### 1. `build.gradle`

`trimmedRuntime` (Exec) runs `jlink` with the six modules plus `--strip-debug
--no-header-files --no-man-pages --compress=zip-9` into `build/jpackage-runtime`,
deleting it first since jlink refuses an existing directory. Keep the module list
in one named variable, commented that `java.logging` and `jdk.crypto.ec` are
invisible to `jdeps` and that removing `jdk.crypto.ec` breaks TVmaze over HTTPS.
Someone will otherwise tidy them away. Check whether `jdk.crypto.ec` still exists
whenever the toolchain moves past 21.

`jpackageBundle` (Exec) depends on `installDist` and `trimmedRuntime`, `onlyIf {
isMacOs || isWindows }`, and runs jpackage with `--input
build/install/tvrenamer/lib`, `--main-jar` from `tasks.jar.archiveFileName`,
`--main-class` from `application.mainClass`, `--runtime-image`, and the icon from
`src/main/resources/icons`. Feed JVM args from
`application.applicationDefaultJvmArgs` so `-XstartOnFirstThread` stays declared
once at `build.gradle:37` and cannot drift from the start scripts.

macOS adds `--type dmg`, `--mac-package-identifier org.tvrenamer.TVRenamer`, and
when a `macSignIdentity` property or env var is present, `--mac-sign`,
`--mac-signing-key-user-name` and `--mac-entitlements`. Absent it, the build is
unsigned, so local builds need no Apple setup and take the same code path.

Windows adds `--type exe`, `--win-shortcut`, `--win-menu`, `--win-dir-chooser`
and a fixed `--win-upgrade-uuid`, generated once and never changed, so upgrades
replace the install instead of landing side by side.

Validate the version shape with a regex before invoking jpackage and fail with a
clear message. The `b` gate treats `1.0rc1` as final, and it should not reach
jpackage.

**Rename the output.** jpackage names files from `--name` and `--app-version`, so
both Mac runners emit `TVRenamer-1.0.dmg`, and the publish job's
`merge-multiple: true` makes one silently overwrite the other, shipping one
architecture twice under a single name. Rename in `doLast` to include
`platformLabel`, and fail if the expected file is absent. Rename the file only,
never `--name`, which sets the `.app` bundle name and menu bar title.

### 2. `.github/workflows/release.yml`

Add a `meta` job that runs once on ubuntu, does the tag-versus-version-file check
currently duplicated across all five matrix jobs, and outputs `version` and
`is_final`. The existing `publish` job then reads `needs.meta.outputs.is_final`
for its prerelease decision instead of repeating its own `case`, so the gate and
the prerelease flag cannot disagree.

In the existing matrix, gated on `is_final == 'true'`: on macOS import the
certificate, run `jpackageBundle`, then notarize and staple. On Windows just run
`jpackageBundle`, with a preflight that WiX is on `PATH` so a future runner image
change gives a one-line error rather than a cryptic jpackage failure.

Keychain import, into a temporary keychain so exactly one identity is present and
nothing can trigger an unanswerable GUI prompt:

    security create-keychain -p "$KC_PASS" "$KEYCHAIN"
    security set-keychain-settings -lut 21600 "$KEYCHAIN"
    security unlock-keychain -p "$KC_PASS" "$KEYCHAIN"
    security import "$P12" -k "$KEYCHAIN" -P "$P12_PASS" -T /usr/bin/codesign
    security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | tr -d '"')
    security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$KC_PASS" "$KEYCHAIN"

`set-key-partition-list` is the line everyone omits, and without it `codesign`
blocks on a prompt nothing can answer, so the job hangs to timeout instead of
failing usefully. `set-keychain-settings -lut` stops the keychain re-locking
between import and signing. Generate the keychain password per job rather than
storing it. Then assert `security find-identity -v -p codesigning` reports a
`Developer ID Application` identity, because a `.p12` exported without its
private key imports without complaint and fails much later. Delete the keychain
in an `if: always()` step.

Notarize the **dmg**, not the app, since a ticket stapled to the app is lost when
the dmg is built around it:

    xcrun notarytool submit "$DMG" --key ... --key-id ... --issuer ... --wait --output-format json
    xcrun notarytool log <id> ...
    xcrun stapler staple "$DMG" && xcrun stapler validate "$DMG"

Parse the JSON status rather than trusting the exit code, and read the log even
on success, because the unsigned-nested-code warning is the early signal for the
top risk below. Stapling is not strictly required but makes first launch work
offline and removes a stall.

Both Mac runners produce a dmg, so that is two signings and two notarizations per
release. Notarization is minutes each, roughly tripling release time.

**Fail closed on artifact counts** in `publish`, before creating the release:
assert 5 zips always, plus 2 dmgs and 1 exe when `is_final`, and 0 of each when
not. This is the real defence against the gate silently skipping, since a
mistyped job output evaluates to empty and therefore falsey. It also catches
bundles leaking into a beta.

**Fix the asset glob.** The release step currently passes `dist/*.zip`. Adding
`dist/*.dmg dist/*.exe` breaks every beta, because an unmatched glob is passed to
`gh` literally and `gh` errors. Use `shopt -s nullglob` and an array.

### 3. Supporting files

* `etc/entitlements.plist` as above.
* `CHANGELOG.md`: bundles include a Java runtime, the zip remains for people who
  have their own, and Windows will show a SmartScreen warning while unsigned.
* `README.md`: bundles as the default download, zip as the alternative, and
  replace the "run from a terminal for a stack trace" instructions with
  `TVRenamer.app/Contents/MacOS/TVRenamer`, which does still print to a terminal
  (verified by watching `JAVA_TOOL_OPTIONS` echo through it).

### 4. What only the maintainer can do, and signing is blocked until it is done

1. Create a **Developer ID Application** certificate. The keychain currently has
   only Apple Development identities, which cannot sign for distribution and
   cannot be notarized. On an Organization account only the Account Holder can
   create one, and Apple caps how many you may hold, so do not lose the `.p12`.
2. Export it as `.p12` **with its private key**, selecting the key in Keychain
   Access rather than the certificate alone.
3. Create an App Store Connect API key for notarization, preferred over an Apple
   ID and app-specific password because it is not tied to one person's account.
4. Add secrets: base64 `.p12`, its password, the full identity string, and the
   API key `.p8` plus its key ID and issuer ID. Use `base64 < cert.p12 | tr -d
   '\n'`, since BSD `base64` has no `-w0`.

## Verification

Ordered so the cheap checks settle the uncertain things first.

1. **Locally, unsigned.** `./gradlew clean build installDist jpackageBundle`,
   install the dmg, `rm -rf ~/.swt` to force the native-extraction path, launch
   by double-clicking. Confirm the window appears, a TVmaze search returns
   results, and a rename works. Double-clicking rather than a terminal is the
   point: a missing native library or missing `-XstartOnFirstThread` shows up
   only here.
2. **Trimmed runtime completeness.** Run the tests against the jlink runtime, not
   the default JDK:

       <runtime>/bin/java -XstartOnFirstThread -cp "<test classes>:<app jars>:<junit>:<hamcrest>" \
         org.junit.runner.JUnitCore org.tvrenamer.controller.TVmazeProviderTest \
         org.tvrenamer.controller.EndToEndRenameTest

   `TVmazeProviderTest` makes real HTTPS calls, which is the check that matters.
   45 tests passed this way during investigation.
3. **Locally, signed**, once the certificate exists. Verify
   `flags=0x10000(runtime)`, a real `TeamIdentifier`, no `get-task-allow`, and
   whether the dmg wrapper itself is signed:

       codesign --verify --deep --strict --verbose=2 build/jpackage/TVRenamer.app
       codesign -d --entitlements :- build/jpackage/TVRenamer.app
       codesign -dv --verbose=4 build/jpackage/TVRenamer-*.dmg

   If the dmg is unsigned, add an explicit `codesign` of the dmg before
   notarizing.
4. **CI signing path without burning a tag.** Add `workflow_dispatch` to
   `release.yml` with an app-version override input, force `is_final` for that
   event, and gate the `publish` job to `push` only. The override is needed
   because the version file says `1.0b5`, which jpackage rejects; it exists only
   for `workflow_dispatch`, never for a tag. Watch for `find-identity` printing
   one identity, jpackage not hanging, and `Accepted` from notarytool.
5. **Gatekeeper on a machine that never built it.** Download through a browser so
   it carries the quarantine flag, then `xcrun stapler validate` and `spctl
   --assess -vvv`. Expect `source=Notarized Developer ID`. Passing on the build
   machine proves nothing, since it already trusts the local certificate. Test an
   Intel Mac too; separate dmg, separate notarization.
6. **Windows.** Install the `.exe`, expect a SmartScreen warning, launch from the
   Start menu, rename a file, uninstall. Then reinstall a bumped version to
   confirm `--win-upgrade-uuid` upgrades rather than installing side by side.
7. **The gate, both directions.** A beta tag must produce 5 zips and no bundles;
   a final tag must produce both. A slip here fails silently by shipping zips
   only, which is the outcome most likely to go unnoticed.

## Risks, ranked

1. **Unsigned SWT dylibs inside the jar.** jpackage does not open jars, so the
   Eclipse natives are unsigned by the project. Apple's notary service scans
   inside archives and has historically warned rather than rejected, but has
   tightened over time. Read the notary log every time, even on success. Escape
   hatch if rejected: extract the natives into the app image so jpackage signs
   them as loose Mach-O files, and pass `--java-options
   -Dswt.library.path=$APPDIR`, which SWT checks before extracting to `~/.swt`.
   Do not build that speculatively.
2. **Missing `set-key-partition-list`.** Hangs to job timeout with no useful
   message.
3. **The dmg filename collision** silently ships one architecture twice.
4. **The asset glob** breaks every beta the moment `dist/*.dmg` is added.
5. **`get-task-allow`** in Temurin's jpackage default entitlements would fail
   notarization. Verified absent in a local JBR build; check on CI.
6. **`1.0rc1`-shaped versions** pass the `b` gate. Regex-validate in Gradle.
7. **`notarytool` exiting 0 with status `Invalid`.** Parse the status.
8. **WiX missing from `PATH`** on a future `windows-latest` image.

## Not in scope

* Windows code signing. Needs a purchased certificate.
* Linux `.deb` and `.rpm`.
* The `ICON_PARENT_DIRECTORY` fallback and the dead `etc/default-overrides.xml`
  seed.
