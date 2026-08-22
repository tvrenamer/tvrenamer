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

## Where this got to

Implementation is done on the `native-bundles` branch, and the Apple
credentials are in place as of 22 August 2026. The branch has been rebased onto
master and is not yet pushed.

| Step | State |
| --- | --- |
| 1. `build.gradle` | Done. Builds, installs and runs an unsigned dmg. |
| 2. `release.yml` | Written, actionlint clean. Never executed. |
| 3. entitlements, CHANGELOG, README | Done. |
| 4. Certificate and notarisation keys | Done. All six secrets set, none exercised. |

Verification steps 1, 2 and 3 pass. Verification step 4, the `workflow_dispatch`
run, is now the next thing to do and the first that exercises signing,
notarisation and the Windows exe. It is blocked on one thing: GitHub only
offers the Run workflow button for definitions present on the default branch,
and `workflow_dispatch` lives on this branch, so `release.yml` has to reach
master before a manual run can start. Verification step 5 needs a second
machine. Step 6 needs Windows or that CI run.

Every risk except 4 and 7 is closed. Risk 1 was settled by two real notary
submissions on 22 August 2026, both Accepted with no issues. Risks 4 and 7 are
written but unexercised, and only the CI run touches them.

### The certificate, for the record

Developer ID Application: Vipul Delwadia (TLX7RVSV2G), G2 sub-CA, valid to
22 August 2031. A Team key, not an Individual key, because `release.yml` passes
`--issuer` unconditionally and `notarytool` rejects that for Individual keys.

Two traps found doing this. Keychain Access writes `.p12` bags with RC2-40-CBC,
which OpenSSL 3 refuses without `-legacy`, so the obvious private-key check
reports zero keys on a perfectly good export. Use `security import` into a
throwaway keychain instead, which is what CI does anyway. And the `.p8`
downloads exactly once, so losing it means revoking and reissuing.

### Corrected by building it

* The dmg is 40MB, not 44MB, on a 50MB jlink runtime.
* The suite is 64 tests, not 45. `TVmazeProviderTest` and `EndToEndRenameTest`
  hold 4 between them, so whatever produced 45 was not those two classes.
  Removing `jdk.crypto.ec` does fail all 3 TVmaze tests, as claimed.
* Temurin 21's default entitlements carry no `get-task-allow`, which closes
  risk 5. They also grant three things we do not want, not two:
  `allow-dyld-environment-variables` alongside `cs.debugger` and
  `device.audio-input`.
* **"No application code changes needed" was wrong.** Two XStream bugs made
  1.0b5 unusable, neither of them anything to do with packaging. Saving
  preferences failed on Java 17 and later, because java.base does not open
  java.util and XStream makes every field accessible before it consults
  `omitField`. Reading either the preferences or the overrides file threw
  `ForbiddenClassException`, because XStream has denied all types by default
  since 1.4.18, and that one killed the application on startup before it could
  show a window. So saving preferences once bricked the app. Both are fixed.
  Only double-clicking the built app found them.
* The test task now takes the same JVM arguments as the start scripts. The
  suite was green only because no machine that ran it had ever saved
  preferences.
* **jpackage leaves the dmg unsigned.** It signs the `.app` inside and stops
  there. `spctl --assess` on the dmg reported `no usable signature` while the
  app inside the same dmg came back `accepted, source=Notarized Developer ID`.
  Gatekeeper assesses what the user downloads, so the wrapper matters. A
  `signDmg` task, attached with `finalizedBy` so `gradlew jpackageBundle` in the
  workflow needs no change, signs it with `--timestamp` between jpackage and
  notarisation. The dmg now assesses as `Notarized Developer ID`.
* Stapling an unsigned dmg succeeds and `stapler validate` passes, so stapling
  alone proves nothing about Gatekeeper. Assess the dmg itself, not the app.

### Added beyond this plan

* `-PappVersion` overrides the version file, which verification step 4 needs,
  since the file holds a beta number that jpackage refuses.
* Signing and notarisation fail closed on a tag push when the secrets are
  absent, rather than quietly publishing an unsigned dmg that Gatekeeper will
  refuse. A manual run still builds unsigned, so the workflow is testable now.
* The WiX preflight looks under Program Files and appends what it finds to
  PATH, rather than only checking PATH.
* `.claude/research/jre-less-native-bundles.md` records why bundles carry a
  runtime rather than relying on an installed JRE. Briefly: jpackage cannot
  build a runtime-less image at all, and a Temurin JRE download is larger than
  the whole bundle.

## Measured, not assumed

| | Size |
| --- | --- |
| Existing zip | 7MB |
| Full-JDK app-image | 170MB |
| Trimmed app-image | 67MB |
| Trimmed `.dmg` | 44MB, measured again at 40MB once built |

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
3. **Locally, signed.** Done, 22 August 2026. The app came back with
   `flags=0x10000(runtime)`, the full chain to Apple Root CA,
   `TeamIdentifier=TLX7RVSV2G`, a secure timestamp, exactly the three
   entitlements and no `get-task-allow`. The dmg wrapper was unsigned, as this
   step anticipated, so `signDmg` now signs it. Both notary submissions were
   Accepted with `issues: null`. The dmg has to be signed before submission,
   since a signature applied after stapling would discard the ticket.
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

1. **Unsigned SWT dylibs inside the jar. Closed, and the reasoning here was
   wrong.** The premise was that jpackage does not open jars, leaving the
   Eclipse natives unsigned, and that the notary might warn. The notary opened
   the jar. All three `libswt-*.jnilib` files appear in `ticketContents` under
   paths that run straight through the archive, for example
   `TVRenamer.app/Contents/app/org.eclipse.swt.cocoa.macosx.aarch64-3.130.0.jar/libswt-cocoa-4969r18.jnilib`.
   They were ticketed, not flagged, and `issues` was null. The escape hatch,
   extracting the natives and passing `-Dswt.library.path=$APPDIR`, is not
   needed. Keep reading the log on every release anyway, since Apple has
   tightened this before.
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
