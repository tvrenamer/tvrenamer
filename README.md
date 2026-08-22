# TVRenamer
[![Build Status](https://github.com/tvrenamer/tvrenamer/actions/workflows/build.yml/badge.svg)](https://github.com/tvrenamer/tvrenamer/actions/workflows/build.yml)
## About
TVRenamer is a Java GUI utility to rename TV episodes from TV listings
It will take an ugly filename like **Lost.[6x05].DD51.720p.WEB-DL.AVC-FUSiON.mkv** and rename it to **Lost S06E05 Lighthouse.mkv**

## [Screenshot](https://github.com/tvrenamer/tvrenamer/wiki/Screenshots)
![Screenshot](https://raw.githubusercontent.com/wiki/tvrenamer/tvrenamer/tvrenamer-0.5b2.png)

## Features
 * Rename many different shows at once from information from [TVmaze](https://www.tvmaze.com/)
 * Customise the format and content of the resulting filename
 * Native look & feel for your operating system
 * Drag & Drop or standard 'add file' interface
 * Optionally move renamed files, i.e. a NAS or external HDD

## Usage & Download

> ## Please Note
> Your virus software may display a false positive on TVRenamer. This is reported in the issue:
>  [#238](https://github.com/tvrenamer/tvrenamer/issues/238)
> This software is open source and contains no viruses. You can inspect the source and build it yourself if you're interested. We do not know why the virus detection software thinks there's a virus; possibly it's simply because the program will rename your files, which some programs may be overly protective about.
>
> If you get a message from your virus software, we would ask that you report it to the makers of the virus software, as a bug in their product, reporting a false positive.
>
> Again, we assure you the program contains no viruses.

[Download](https://github.com/tvrenamer/tvrenamer/releases) the bundle for
your operating system and processor. It carries its own Java runtime, so there
is nothing to install first:

| Download | For |
| --- | --- |
| `macos-aarch64.dmg` | Macs with Apple silicon (M1 and later) |
| `macos-x86_64.dmg` | Intel Macs |
| `windows-x86_64-portable.zip` | Windows on Intel or AMD |

On macOS, open the `.dmg` and drag TVRenamer to your Applications folder.

On Windows, unzip it anywhere and run `TVRenamer\TVRenamer.exe` from inside.
There is no installer, so it needs no admin rights and you remove it by
deleting the folder. It is not signed yet, so SmartScreen will warn you the
first time: choose 'More info', then 'Run anyway'.

### The zip

Every platform also has a plain zip. It is much smaller, around 7MB against
50MB, because it has no Java in it. Linux has only this. Note the Windows one
is `windows-x86_64.zip`, not the `-portable` zip above. Install
[Java 21 or later](https://adoptium.net/), then download the zip matching your
operating system and processor:

| Download | For |
| --- | --- |
| `linux-x86_64` | Linux on Intel or AMD |
| `linux-aarch64` | Linux on ARM |
| `macos-aarch64` | Macs with Apple silicon (M1 and later) |
| `macos-x86_64` | Intel Macs |
| `windows-x86_64` | Windows on Intel or AMD |

Unzip it wherever you like, then start it from the `bin` directory inside:

  * On Windows: double click `bin\tvrenamer.bat`
  * On macOS and Linux: run `bin/tvrenamer` from a terminal

On Linux you can add TVRenamer to your desktop's application menu with a
['Custom Application Launcher'](http://library.gnome.org/users/user-guide/2.32/gospanel-34.html.en):

    Type: Application
    Name: TVRenamer
    Command: <where you unzipped it>/bin/tvrenamer
    Icon: Can be anything, perhaps [our icon](http://github.com/tvrenamer/tvrenamer/raw/main/src/main/resources/icons/tvrenamer.png)

*If the application doesn't start, switch the Type to 'Application in Terminal'
so you can read the error.*

## Common Problems
### Connectivity Issues
Releases before 1.0b5 looked up episodes on TheTVDB, whose version 1 API is now
retired. Those versions cannot find any show, so upgrade if you are still on
one. If you see errors about "unable to connect to internet" on a version older
than [0.7.2](https://github.com/tvrenamer/tvrenamer/releases/tag/0.7.2), upgrade
for the same reason.

### Java version issues
*Java version 21* or later is required.  Type `java -version` into your terminal and ensure the version is at least 21:

    $ java -version
    openjdk version "21.0.11" 2026-04-21

### Processor architecture
The download carries a native GUI library for one processor architecture, so it
has to match the Java you run it with. `java -version` prints the architecture on
its last line. Mixing them gives an `UnsatisfiedLinkError` on startup, visible
only when you run from a terminal:

    Exception in thread "main" java.lang.UnsatisfiedLinkError: Can't load library: .../libswt-pi3-gtk.so

32-bit builds are no longer available. Eclipse stopped shipping 32-bit SWT
libraries after 2018, so the last 32-bit release is
[v1.0b4](https://github.com/tvrenamer/tvrenamer/releases/tag/v1.0b4).

### macOS blocks the download
The `.dmg` is signed and notarised, so it opens without complaint. The zip is
not, so macOS marks everything in it as quarantined. If the unzipped copy
refuses to start, clear that mark on the unpacked directory:

    xattr -d -r com.apple.quarantine TVRenamer-<version>-macos-<arch>

## Running in debug mode
If the application crashes it helps us greatly if you can provide us a stacktrace of what went wrong.  In order to do this, you just need to run the application in the terminal, then copy the output into [a new bug report](https://github.com/tvrenamer/tvrenamer/issues/new).

If the application fails to start due to a java error, [ensure that your JAVA_HOME environment variable is set correctly](http://www.oracle.com/technology/sample_code/tech/java/sqlj_jdbc/files/9i_jdbc/EnvSetup.html).

  * On Windows:
    1. Open the Windows Command Prompt (Windows + r, then type `cmd` and push enter)
    1. Navigate to the unzipped TVRenamer directory.
    1. Execute `bin\tvrenamer.bat`
  * On macOS and Linux:
    1. Open a terminal.
    1. Navigate to the unzipped TVRenamer directory.
    1. Execute `bin/tvrenamer`

If you installed the macOS `.dmg` rather than the zip, run the executable inside
the bundle instead. It prints to the terminal the same way:

    /Applications/TVRenamer.app/Contents/MacOS/TVRenamer

## Building from source

The build uses the Gradle wrapper, so you only need a JDK 21 or later:

    ./gradlew build     # compile and run the tests
    ./gradlew run       # build and start the application

Gradle picks the SWT native library that matches the machine you build on, so
the same build file works on macOS (Intel and Apple silicon), Windows and Linux.

Some of the tests query TVmaze and need an internet connection.

## Contributions

If you'd like to run from source, please see the
[Quick Start](https://github.com/tvrenamer/tvrenamer/wiki/Quick-Start)
guide, and then look over
[Development Setup](https://github.com/tvrenamer/tvrenamer/wiki/Development-Setup).

Please see the
[Development Process](https://github.com/tvrenamer/tvrenamer/wiki/Development-Setup#development-process)
section if you'd like to contribute!  Anything from Java code patches to UI/UX
recommendations would be gratefully received.

To contribute to the code side of things you should know Java and it would be helpful if you know some SWT and git. The easiest way to submit changes is via a [github pull request](http://help.github.com/forking/) based off the [main branch](http://github.com/tvrenamer/tvrenamer/tree/main).

For anything else (feature requests, comments, fanmail!), [create a new issue](https://github.com/tvrenamer/tvrenamer/issues/new) and set the label to 'Type-Enhancement'.
