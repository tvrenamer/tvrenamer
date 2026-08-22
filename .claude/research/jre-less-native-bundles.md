# Shipping TVRenamer without a bundled JRE

Research for the 1.0 packaging decision. Question: instead of the bundled runtime
in `.claude/plans/native-bundles-1.0.md`, can we ship a Windows `.exe` and a macOS
`.app`/`.dmg` that find an already-installed Java 21, and prompt the user to
install one when there is none?

Everything below is checked against launch4j's own source (3.50 tarball), the JDK
tool docs, Apple's documentation, Microsoft's documentation, and this machine.
Where I ran a command, the output is quoted.

## The short answer

**Windows: technically viable, practically worse than bundling.** launch4j 3.50
still works and its missing-JRE flow is exactly what TVRenamer used to have: a
message box, then it opens a download URL you choose. But launch4j's registry
search only knows the `JavaSoft` and `IBM` keys. Temurin and Microsoft's OpenJDK
do not write `JavaSoft` keys, so detection falls back to `%JAVA_HOME%;%PATH%`,
which the Temurin installer only populates if the user ticked the right optional
features. So the exe will tell some users with a perfectly good Java 21 that they
have no Java. That is a worse failure than a 44MB download.

**macOS: viable to build, but the missing-JRE story is broken at the OS level.**
The `/usr/bin/java` stub still exists on macOS 26 and still refuses to run, but
what it prints is now a dead end. On this machine, with no JDK installed:

```
$ /usr/libexec/java_home -V
The operation couldn't be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

It points at java.com, which no longer offers a JRE that runs TVRenamer. Apple
has not shipped an installer trigger for years. So a JRE-less `.app` has to
detect and prompt itself, in a shell script, before any JVM exists to draw a
dialog with.

**jpackage cannot do this at all.** There is no mode where jpackage emits a
launcher that searches for a system JVM. `--runtime-image` copies the runtime you
point it at into the bundle. This is definitive, see the jpackage section.

**Recommendation: keep the bundled-runtime plan.** Do not build a JRE-less
launcher. The detail is in the recommendation section.

## What TVRenamer used to do

`git show fde37d5^:build.xml` lines 226-235:

```xml
<launch4j>
  <config headerType="gui"
          outfile="${dist}/${rel.name}-@{platform}.exe"
          dontWrapJar="false"
          jar="${build.jar}/@{platform}/tvrenamer.jar"
          icon="${res}\icons\oldschool-tv-icon.ico"
          chdir=".">
    <jre minVersion="1.8.0" />
  </config>
</launch4j>
```

No `downloadUrl`, no `maxVersion`, no custom messages. The jar was wrapped inside
the exe. macOS came from `fx:deploy nativeBundles="image"` at lines 186-209, which
did bundle a JRE, so the JRE-less behaviour was Windows only even then.

## launch4j

I downloaded `launch4j-3.50-macosx-x86.tgz` from SourceForge and read the source
rather than the docs where the two might differ.

### Maintenance status

3.50, released 13 November 2022, is the newest release
([changelog](https://launch4j.sourceforge.net/changelog.html),
[files](https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/)).
Nearly four years without a release. It is not dead in the sense of broken, but
nobody is fixing it either. That matters below, because the registry search has a
real gap and nobody is going to close it.

### Version strings

`Jre.java:49` gives the accepted format for `minVersion`/`maxVersion`:

```java
public static final String VERSION_PATTERN = "(1\\.\\d\\.\\d(_\\d{1,3})?)|[1-9][0-9]{0,2}(\\.\\d{1,3}){0,2}";
```

So both `1.8.0_51` and `21` or `21.0.5` are accepted. `JreVersion.parseString`
branches on whether the first component is 1, and handles the JEP 223 scheme.
`maxVersion` requires `minVersion` to be set and to be strictly lower
(`Jre.java:69-73`).

Detection itself does not parse a registry string. `head.c:1435` runs the
candidate:

```c
snprintf(cmdline, MAX_ARGS, "\"%s\" -version", launcherPath);
```

then parses the output and normalises it. That is version-scheme agnostic and
will read `21.0.x` correctly. This is the part of launch4j that ages well.

### Where it looks

3.50 made path search primary and registry secondary
([changelog](https://launch4j.sourceforge.net/changelog.html)). The default path
is `%JAVA_HOME%;%PATH%` (`Jre.java:50`). `pathJreSearch` (`head.c:885`) splits on
`;`, strips a trailing `\bin` or `/bin` from each entry, and takes the first entry
that both has a launcher and reports an in-range version.

The registry fallback, `findRegistryJavaHome` (`head.c:581`), searches exactly
these keys and no others:

```
SOFTWARE\JavaSoft\Java Runtime Environment
SOFTWARE\JavaSoft\Java Development Kit
SOFTWARE\JavaSoft\JRE
SOFTWARE\JavaSoft\JDK
SOFTWARE\IBM\Java Runtime Environment
SOFTWARE\IBM\Java Development Kit
SOFTWARE\IBM\Java2 Runtime Environment
```

There is no Eclipse Adoptium key, no Azul key, no Microsoft key. Adoptium's
Windows install docs list five MSI features and mark the defaults:
`FeatureMain`, `FeatureEnvironment` (updates PATH) and `FeatureJarFileRunWith`
are default, while `FeatureJavaHome` (sets `JAVA_HOME`) and
`FeatureOracleJavaSoft` (writes the HKLM JavaSoft keys) are **not**
([adoptium.net/installation/windows](https://adoptium.net/installation/windows/)).

So a stock Temurin install is found through `%PATH%`, not the registry, and only
because `FeatureEnvironment` is on by default. A user who unticked it, or who
installed from a zip, or who uses a version manager, gets told they have no Java.

`requires64Bit` (default false) limits candidates to 64-bit runtimes. `requiresJdk`
(default false) also requires `bin\javac.exe` (`head.c:540-546`). Both are
plain booleans in 3.50. `jdkPreference` is gone, replaced by these two in 3.50.

### What the user sees with no JRE

Two different messages, and the difference matters. `createJreSearchError`
(`head.c:959`):

```c
if (*search.javaMinVer)
{
    loadString(JRE_VERSION_ERR, error.msg);
    strcat(error.msg, " ");
    strcat(error.msg, search.originalJavaMinVer);
    ...
    loadString(DOWNLOAD_URL, error.url);
}
else
{
    loadString(JRE_NOT_FOUND_ERR, error.msg);
}
```

The download URL is only opened when `minVersion` is set. TVRenamer sets it, so
the exe takes the first branch. Defaults from `Msg.java:66-96`:

* `jreVersionErr`: `This application requires a Java Runtime Environment`, with
  the min version appended, then ` - ` and the max version if set, then ` (64-bit)`
  if `requires64Bit`. So the box reads `This application requires a Java Runtime
  Environment 21`.
* `jreNotFoundErr`: `This application requires a Java Runtime Environment.`
* `launcherErr`: `The registry refers to a nonexistent Java Runtime Environment
  installation or the runtime is corrupted.`, appended when a corrupt JRE was found.

`signalError` (`head.c:172`) shows the message box, and only after the user
dismisses it does it call `ShellExecute(NULL, "open", error.url, ...)`. The
browser opens after the dialog closes, not from a button in it.

`downloadUrl` is a free-text config field (`Config.java:92`) written into the exe
as a resource (`RcBuilder.java:94,132`), so it can point at Adoptium. It has no
hardcoded default in the source. All five messages are overridable, so the dialog
text can name Java 21 and Adoptium explicitly.

**A too-old JRE and no JRE produce the same dialog.** The code path is identical.
Someone on Java 17 sees "This application requires a Java Runtime Environment 21"
and no statement of what they actually have. That is a poor error, and there is no
config knob to improve it.

### Modules

`head.c` builds either `java <opts> -jar "app.jar"` (line 1151) or
`java <opts> -classpath "..." mainClass` (line 1097). There is no module launch.
`<opt>` values are arbitrary text and are variable-expanded, so `--module-path`
could be passed, but the main class is still appended positionally, so `-m` style
launch does not fit. Irrelevant for TVRenamer, which is a classpath app.

`dontWrapJar="false"` as in the old build wraps the jar inside the exe. That
worked because the Ant `build.jar` macro built a fat jar, folding every runtime
dependency and the SWT natives in with `zipgroupfileset`
(`git show fde37d5^:build.xml`, lines 143-146). The Gradle build does not do that.
It produces `build/install/tvrenamer/lib` with 15 separate jars, so a launch4j exe
today needs either a new shadow-jar step or `dontWrapJar="true"` with an explicit
`<classPath>` and a `lib` directory shipped beside the exe. The second option means
the download is no longer one file, which was half the point.

## jpackage cannot do this

Definitive. The JDK 21 jpackage man page describes `--runtime-image` as:

> Path of the predefined runtime image that will be copied into the application
> image (absolute path or relative to the current directory). If --runtime-image
> is not specified, jpackage will run jlink to create the runtime image using
> options specified by --jlink-options.

([jpackage(1), JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html))

"Copied into the application image" is the whole answer. Pointing `--runtime-image`
at a system JRE copies that JRE into the bundle. It does not reference it.

The Packaging Tool User's Guide states the intent directly:

> To eliminate the need for users to install a Java runtime, one is packaged with
> your applications.

and every documented app-image layout contains a `runtime/` directory
([Packaging Overview, JDK 21](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)).

There is no flag, no `--type`, and no launcher mode that resolves a JVM at run
time. If we want JRE-less, jpackage is off the table and we are choosing a
different tool per platform.

## Other Windows options

**WinRun4J.** Last release 0.4.5, March 2018. Last push to
[poidasmith/winrun4j](https://github.com/poidasmith/winrun4j) was January 2019.
A fork, [dcgoodridge/winrun4j9](https://github.com/dcgoodridge/winrun4j9), exists
only because the original does not support Java 9. Dead. Do not use.

**packr.** [libgdx/packr](https://github.com/libgdx/packr) last released 4.0.0 in
March 2021. It also does not solve this problem: its own README says it "Packages
your JAR, assets and a JVM for distribution", and `--jdk` is a required parameter
pointing at a JRE archive to embed. Same category as jpackage, minus the
maintenance.

**exe4j / install4j.** ej-technologies still sell both and they do support
searching for an installed JRE. install4j is free for "non-profit open source
projects with an established web site and a released product", in exchange for a
do-follow text link to their site containing "multi-platform installer builder",
and licences are issued for core committers only
([ej-technologies open source licences](https://www.ej-technologies.com/install4j/openSource)).
That is a real option cost-wise, but it puts a release-blocking dependency on a
vendor licence that has to be renewed, applied for, and kept in CI. For a project
with one maintainer that is a bad trade.

**A `.bat`, `.vbs` or PowerShell shim.** Free, and it can produce a genuinely good
error message because we write it. But it is not an exe, it has no icon, `.bat`
flashes a console window, `.vbs` and `.ps1` are commonly blocked by policy or
SmartScreen, and none of them can be Authenticode signed in a way that helps.
This is a downgrade from the plain zip we already ship, which at least has
`bin\tvrenamer.bat`.

**Verdict for Windows.** launch4j 3.50 is the only credible JRE-less option. It
works. Its weakness is detection coverage, not the tool.
## The macOS "no Java installed" prompt is gone

This machine runs macOS 26 (`Darwin 27.0.0`) and has no JDK installed:
`/Library/Java/JavaVirtualMachines/` is empty, `JAVA_HOME` is unset, and there is
no Homebrew JDK. That makes it a clean test of the missing-Java path.

The stub still exists and is still a real Mach-O:

```
$ ls -l /usr/bin/java
-rwxr-xr-x  37 root  wheel  135376  8 Aug 05:29 /usr/bin/java
$ file /usr/bin/java
/usr/bin/java: Mach-O universal binary with 2 architectures: [x86_64] [arm64e]
$ otool -L /usr/bin/java
	/System/Library/PrivateFrameworks/JavaLaunching.framework/Versions/A/JavaLaunching
	/System/Library/Frameworks/Foundation.framework/...
```

`/usr/bin/java` and `/usr/libexec/java_home` are the same binary, hard linked 37
ways along with the rest of the Java command stubs. Both do this:

```
$ /usr/bin/java -version
The operation couldn't be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
$ /usr/libexec/java_home -V
The operation couldn't be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
$ /usr/libexec/java_home; echo $?
...
1
```

**The historical "No Java runtime present, requesting install." string no longer
exists anywhere in the system.** I grepped every dyld shared cache subcache under
`/System/Volumes/Preboot/Cryptexes/OS/System/Library/dyld/`. The phrase "No Java
runtime present" returns nothing. "requesting install" appears only in
`xcode-select` text. What does exist, in
`dyld_shared_cache_arm64e.50` next to `com.apple.java_home` and `JAVA_HOME`, is:

```
Unable to locate a Java Runtime that supports %@.
Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

So the JDK-install request is dead. What remains is a message pointing at
java.com, which today offers a consumer Java 8 runtime that will not run a Java 21
application. If a JRE-less `.app` falls through to the system stub, the user is
sent somewhere actively unhelpful.

The `java_home` man page on this machine (dated 15 July 2020) still documents
`-v` version filtering, `-a` architecture filtering, `-F`/`--failfast`, and
`-X`/`--xml`. So the tool is intact and usable as a JVM locator. It just has
nothing to say when there is no JVM.

Practical consequence: a JRE-less `.app` must do its own detection and its own
dialog, in a launcher that runs before any JVM exists. That means shell script
plus `osascript`, or a compiled stub with `NSAlert`.

## macOS options

### universalJavaApplicationStub

A bash script that goes in `Contents/MacOS` and reads JVM settings out of
`Info.plist`. It does everything TVRenamer needs:

* Reads `StartOnMainThread` from the plist and appends `-XstartOnFirstThread`
  (`src/universalJavaApplicationStub` lines 221-225). This is the SWT requirement,
  handled directly.
* Enumerates JVMs with `/usr/libexec/java_home --xml` and filters them against a
  `JVMVersion` min, or `min;max` pair, from `Info.plist` (lines 640-700).
* On no suitable JVM, shows an `osascript` dialog with three buttons and opens the
  chosen URL (lines 835-851):

  ```
  buttons {" OK ", "Java by Oracle", "Java by Adoptium"}
  ...
  if response is "Java by Adoptium" then open location "https://adoptium.net/"
  ```

  Localised into several languages. The English text is
  `No suitable Java version found on your system!\nThis program requires Java %s`.

That is a better missing-JRE experience than anything else on this list, macOS or
Windows, and it distinguishes "wrong version" from "no Java at all".

**But the repository is archived.** The GitHub API reports `archived: true` for
[tofi86/universalJavaApplicationStub](https://github.com/tofi86/universalJavaApplicationStub),
last release v3.3.0 on 4 February 2023, last push the same day. Read only. No
fixes are coming, and the script does no architecture filtering at all: there is
no `uname -m`, no `-a` passed to `java_home`. On Apple Silicon with an x86_64 JDK
installed under Rosetta, it will pick that JDK, and an arm64 SWT jar will then
fail to load. We would be adopting an archived script and adding the arch check
ourselves.

### appbundler

The Oracle Ant task, maintained as
[TheInfiniteKind/appbundler](https://github.com/TheInfiniteKind/appbundler). Last
commit 8 July 2025, so this one is genuinely alive. Its `<runtime>` element is
optional. With no `<runtime>`, the native launcher in `appbundler/native/main.m`
falls back to `/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home`
and then shells out to `/usr/libexec/java_home` (lines 61, 815, 837), `dlopen`s
`libjli.dylib` from whatever it found, and calls `JLI_Launch`. It supports
`JVMArchs` and `LSArchitecturePriority`, and shows an `NSAlert` on failure
(line 115).

Two problems. First, an in-process `JLI_Launch` means the JVM runs inside our
signed launcher, which brings back exactly the library validation problem the
existing plan describes. Second, `-XstartOnFirstThread` in the appbundler
`<option>` list is untested here, and SWT plus an in-process JLI launch is a
combination nobody in that repo is exercising.

### Hand-rolled bundle

`Contents/Info.plist` plus a shell script in `Contents/MacOS` that calls
`/usr/libexec/java_home -v 21 -a arm64 -F`, and on failure runs `osascript` to
show a dialog pointing at adoptium.net. Roughly 30 lines. No dependency, no
archived upstream, and we control the error text and the arch check.

Honestly, if we were going JRE-less on macOS, this is what I would write. The
universalJavaApplicationStub is 908 lines because it supports Apple Java 6,
Oracle plist syntax, drag and drop onto the Dock icon, and eight languages.
TVRenamer needs none of that.
## Signing, notarization and Gatekeeper

### macOS

Apple's requirement list for notarization
([Notarizing macOS software before distribution](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)):

> - Enable code-signing for all of the executables you distribute...
> - Use a "Developer ID" application, kernel extension, system extension, or installer certificate...
> - Enable the Hardened Runtime capability for your app and command line targets...
> - Include a secure timestamp with your code-signing signature.
> - Don't include the `com.apple.security.get-task-allow` entitlement...
> - Link against the macOS 10.9 or later SDK...

A JRE-less `.app` is still a download, still quarantined, and still needs all of
that. Size does not exempt it.

**A script-based bundle does sign.** I built a minimal `.app` whose
`CFBundleExecutable` is a `/bin/sh` script and signed it ad hoc on this machine:

```
$ codesign -f -s - -o runtime -v T.app
T.app: signed app bundle with generic
$ codesign -dvvv T.app
Format=app bundle with generic
CodeDirectory v=20100 size=195 flags=0x10002(adhoc,runtime) ...
Sealed Resources version=2 rules=13 files=0
```

`codesign` accepts a non-Mach-O main executable, records the hardened runtime
flag, and writes a detached signature into `Contents/_CodeSignature/`. So the
mechanics work. Whether the notary service accepts a bundle containing no Mach-O
at all is not something I can settle without submitting one, and Apple's
"link against the macOS 10.9 or later SDK" requirement is written for Mach-O.
That goes in open questions.

**The hardened runtime problem inverts, in our favour.** The existing plan
documents the trap: SWT extracts Eclipse-signed dylibs to `~/.swt/lib/macosx/<arch>/`,
and a hardened-runtime JVM inside our bundle refuses to load code signed by
another team without `com.apple.security.cs.disable-library-validation`.

With a script stub, our signed code is a shell script. It `exec`s `java` from a
separately installed JDK. That JVM is a new process with its own signature and its
own (absent) hardened runtime flag, not ours. Library validation never applies to
the SWT load. The entitlements file the plan carefully constructs stops being
needed at all.

That is a genuine advantage of the JRE-less approach, and it is worth writing
down even though I do not think it wins the argument.

**The appbundler route does not get this benefit.** `main.m` `dlopen`s
`libjli.dylib` and calls `JLI_Launch` in process, so the JVM and SWT run inside
our signed, hardened launcher. Same trap as jpackage, same entitlement needed.

### Windows

Microsoft's developer-facing SmartScreen page is blunt about it
([SmartScreen reputation for Windows app developers](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation)):

> | No signature | Warning — "Windows protected your PC"; User must choose "Run anyway" before the app can run. Enterprise policy can prevent continuation entirely. |

and:

> When a file is not signed, SmartScreen reputation must build for each new
> version of your files, starting with zero reputation.

Reputation is per file hash and per signing certificate. It has nothing to do with
which tool produced the exe. **An unsigned launch4j exe and an unsigned jpackage
exe get identical SmartScreen treatment.** There is no advantage either way, and
the plan's decision to ship Windows unsigned for now is unaffected by this
research.

Two details worth carrying forward. Microsoft states EV certificates no longer
bypass SmartScreen, so the expensive certificate is not a shortcut:

> EV certificates no longer bypass SmartScreen... Paying a premium for EV solely
> to avoid SmartScreen warnings is no longer justified.

And Smart App Control on Windows 11 "will block execution of unsigned files unless
the file has a positive reputation", with no "Run anyway". That is a harder wall
than SmartScreen, and it applies to whatever exe we ship.
## What "go install a JRE" means to a user in 2026

There is no consumer JRE for Java 21. Oracle's JDK 11 release notes are explicit:

> In this release, the JRE or Server JRE is no longer offered. Only the JDK is
> offered. Users can use jlink to create smaller custom runtimes.

and

> Auto-update, which was available for JRE installations on Windows and macOS, is
> no longer available.

([Oracle JDK 11 release notes](https://www.oracle.com/java/technologies/javase/11-relnote-issues.html))

java.com still exists and still ships. Its own help page says:

> The Java Runtime Environment (JRE) version 8 is what you get when you download
> Java software from java.com.

([java.com, What is Java](https://www.java.com/en/download/help/whatis_java.html))

Java 8. Which will not run TVRenamer. So both fallback URLs baked into the tools
we would use are wrong: launch4j's documented `downloadUrl` default and the macOS
system stub's message both point at java.com. Any JRE-less build must override
both to point at Adoptium.

Adoptium does publish a real Temurin JRE 21, which is the right target. Sizes,
from the Adoptium API, release `jdk-21.0.12+8` / `21.0.12.1+1`:

| Package | Size |
| --- | --- |
| Windows x64 JRE msi | 33.3MB |
| Windows aarch64 JRE msi | 26.5MB |
| macOS x64 JRE pkg | 40.3MB |
| macOS aarch64 JRE pkg | 46.2MB |

**Read that table against the plan's 44MB dmg.** Telling a Mac user on Apple
Silicon to go install a JRE costs them a 46.2MB download, plus finding the right
page, plus picking JRE over JDK, plus picking aarch64 over x64, and only then do
they get to download TVRenamer. The bundled `.dmg` is smaller than the prerequisite
we would be asking them to install. The JRE-less approach does not save the user
anything on macOS. It costs them.

Auto-install exists but not in a form we can drive from a launcher. `winget
install EclipseAdoptium.Temurin.21.JRE` has manifests in
[microsoft/winget-pkgs](https://github.com/microsoft/winget-pkgs) up to 21.0.12.8,
and `brew install --cask temurin@21` exists. Neither is something a GUI app can
invoke on a user's behalf without asking for elevation and explaining itself, and
neither is present on a machine belonging to the kind of user who does not already
have Java. So the realistic prompt is a dialog and a browser.

## Version compatibility

TVRenamer targets Java 21. Three cases matter: no Java, Java 17, Java 25.

**launch4j.** `minVersion="21"` catches all three. Java 17 and no Java produce the
identical dialog, because `createJreSearchError` takes the same branch whenever
`minVersion` is set (`head.c:959-993`). The user is told "This application
requires a Java Runtime Environment 21" and is never told what they have. Setting
`maxVersion` would let us also reject Java 25, and the dialog would then read
"...Environment 21 - 25", but that turns a probably-fine runtime into a hard
failure. I would set `minVersion` and no `maxVersion`.

**universalJavaApplicationStub.** Better. `JVMVersion` accepts `min;max` (lines
319-323), it enumerates all JVMs via `java_home --xml` and filters, and the
no-suitable-version message is `This program requires Java %s`, distinct from the
no-Java-at-all message `You need to have JAVA installed on your Mac!`. Still does
not print the version the user actually has.

**A hand-rolled stub.** We can say exactly the right thing, because we can run
`/usr/libexec/java_home -V` and read the list. This is the only option that can
produce "You have Java 17.0.9. TVRenamer needs Java 21 or later."

**jpackage.** Not applicable. The runtime is ours, so version mismatch cannot
happen. That is a real, if boring, advantage: an entire class of support question
disappears.

Java 25 is the more interesting risk. A system JRE that is newer than we test
against will run TVRenamer, probably fine, but any SWT or XStream incompatibility
lands on us as a bug report we cannot reproduce. With a bundled runtime the
runtime is pinned and every user is on the version CI tested.
## Comparison

| Option | Platform | Maintained | Signs / notarizes | No JRE found | Effort |
| --- | --- | --- | --- | --- | --- |
| jpackage + jlink (current plan) | mac, win | JDK, permanently | Yes, plan already solved it | Cannot happen | Plan written, ~2 days |
| launch4j 3.50 | win | Last release Nov 2022 | Authenticode possible, same SmartScreen result as any unsigned exe | Message box, then opens configured URL | 1 day, plus a shadow jar |
| WinRun4J | win | Dead since 2019 | n/a | n/a | Do not use |
| packr | win, mac | Last release 2021 | n/a | Bundles a JVM, does not apply | Do not use |
| install4j | win, mac | Commercial, active | Yes | Configurable, good | Licence application, vendor lock |
| `.bat` / `.vbs` shim | win | n/a | No | Whatever we write | Worse than the zip we ship |
| universalJavaApplicationStub | mac | **Archived** Feb 2023 | Yes, bundle signs | Dialog with Adoptium and Oracle buttons | 1 day, plus our own arch check |
| appbundler (TheInfiniteKind) | mac | Active, last commit Jul 2025 | Yes | `NSAlert` | Ant task inside a Gradle build, plus in-process JVM brings the library-validation trap back |
| Hand-rolled script `.app` | mac | Ours | Signs, notarization untested | Whatever we write, can be the best of any option | 1 day, ongoing ownership |

One point in launch4j's favour that has nothing to do with JREs: launch4j ships
platform work directories and runs on macOS and Linux, so it can build a Windows
exe on any runner. jpackage must run on the target OS. That does not change the
recommendation, but it is why the old Ant build could produce a `.exe` from a Mac.
[gradle-launch4j](https://github.com/TheBoegl/gradle-launch4j) defaults to
launch4j 3.50 and was last pushed in January 2026, so the Gradle integration is
alive even though launch4j itself is not moving.

## Recommendation

**Execute the bundled-runtime plan. Do not build a JRE-less launcher.**

The arithmetic settles it. A user on Apple Silicon with no Java downloads 46.2MB
of Temurin JRE before they can run a JRE-less TVRenamer. The bundled `.dmg` is
44MB and it is one file, one drag, done. There is no size argument for JRE-less on
macOS. On Windows the Temurin JRE msi is 33.3MB against a bundle in the same
range. The premise that JRE-less means a smaller download for the user is only
true for users who already have Java 21, and those users are already served by the
7MB zip we ship and will keep shipping.

The rest is downside. On Windows, launch4j's registry search does not know
Adoptium, Azul or Microsoft, so detection rests on `%PATH%` and a Temurin
installer default the user can untick. On macOS the OS-level "install Java" prompt
is gone and the system message points at a Java 8 download that will not work,
so we would be writing and owning our own detection stub. Both platforms give a
too-old JRE the same dialog as no JRE. And a system runtime we did not test
against is a permanent source of bug reports we cannot reproduce.

The one real technical win for JRE-less is that it dissolves the hardened runtime
and SWT library validation problem the plan describes, because the JVM would run
outside our signature. That problem is already solved in the plan with three
entitlements. It is not worth restructuring packaging to avoid.

**On the hybrid.** The plan already is the right hybrid: bundle as the default
download, 7MB zip as the alternative for people who have their own Java. Adding a
third artefact, a JRE-less exe or app, adds a build path, a CI job, a
notarization submission, a download-page choice for users, and a class of support
question ("I downloaded the small one and it says I need Java"), in exchange for
saving nobody any bytes. Three downloads on a release page is one too many. Do
not add it.

**If this gets revisited**, the case to revisit is Windows only, and only if the
bundled exe turns out to be a problem for reasons unrelated to size, for example
Smart App Control blocking an unsigned installer more aggressively than a portable
exe. In that case launch4j with `minVersion="21"`, a `downloadUrl` pointing at
`https://adoptium.net/temurin/releases/?version=21`, and all five `<msg>` strings
rewritten to name Java 21 and Adoptium, is the shape to build. Not before.

## Open questions only testing can settle

1. **Does the notary service accept an `.app` whose main executable is a shell
   script?** `codesign` accepts it, verified above. Apple's stated requirement to
   "link against the macOS 10.9 or later SDK" has no meaning for a script, and the
   only way to know is to submit one to `notarytool`. Cheap to test, about ten
   minutes, if anyone wants the answer.
2. **Does a launch4j exe find a stock Temurin 21 install on a clean Windows box?**
   The path search reads `%PATH%`, the Temurin MSI adds to `PATH` by default, and
   the registry fallback does not know Adoptium. Whether that holds on a real
   machine with a long `PATH` and possibly several JDKs, and which one it picks,
   needs a VM.
3. **Does `pathJreSearch` pick a sensible JVM when several are installed?** It
   takes the first `PATH` entry that satisfies the range, not the newest
   (`head.c:930-944`). A user with Java 21 and Java 25 gets whichever is earlier in
   `PATH`. Untested.
4. **Does `/usr/libexec/java_home` without `-a` return an x86_64 JDK on Apple
   Silicon when only that is installed, and does the arm64 SWT jar then fail?**
   Expected, but I could not test it: this machine has no JDK at all.
5. **Does the macOS stub still show a GUI dialog, or only stderr text?** I
   confirmed the stderr text and confirmed the old "requesting install" string is
   absent from the system, but I could not observe what a Finder double-click of a
   Java app shows on macOS 26 with no JVM.
