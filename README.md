# TVRenamer
[![Build Status](https://travis-ci.org/tvrenamer/tvrenamer.svg?branch=master)](https://travis-ci.org/tvrenamer/tvrenamer)
## About
TVRenamer is a Java GUI utility to rename TV episodes from TV listings
It will take an ugly filename like **Lost.[6x05].DD51.720p.WEB-DL.AVC-FUSiON.mkv** and rename it to **Lost S06E05 Lighthouse.mkv**

## [Screenshot](https://github.com/tvrenamer/tvrenamer/wiki/Screenshots)
![Screenshot](https://raw.githubusercontent.com/wiki/tvrenamer/tvrenamer/tvrenamer-0.5b2.png)

## Features
 * Rename many different shows at once from information from [TheTVDB](http://thetvdb.com/)
 * Customise the format and content of the resulting filename
 * Native look & feel for your operating system
 * Drag & Drop or standard 'add file' interface
 * Optionally move renamed files, i.e. a NAS or external HDD

## Usage & Download

> ## Please Note
> Your virus software may display a false positive on the Windows executable. This is reported in the issue:
>  [#238](https://github.com/tvrenamer/tvrenamer/issues/238)
> This software is open source and contains no viruses. You can inspect the source and build it yourself if you're interested. We do not know why the virus detection software thinks there's a virus; possibly it's simply because the program will rename your files, which some programs may be overly protective about.
>
> If you get a message from your virus software, we would ask that you report it to the makers of the virus software, as a bug in their product, reporting a false positive.
>
> Again, we assure you the program contains no viruses.

[Download](http://tvrenamer.github.com) the correct version for your operating system (OSX, Windows, Linux) and architecture (32 or 64 bit)

  * On Windows:
    1. Unzip the downloaded file somewhere, possibly your Desktop or C:\Program Files
    1. Double click the .exe file
  * On OSX:
    1. Unzip the downloaded file somewhere, possibly your Desktop or /Applications
    1. Double click the .app file
  * On Linux:
    1. Unzip the downloaded file somewhere, possibly your Desktop
    1. It is easiest to add TVRenamer to the top Gnome bar (no need for the terminal).  Add a ['Custom Application Launcher'](http://library.gnome.org/users/user-guide/2.32/gospanel-34.html.en) with the below settings:
    Type: Application
    Name: TVRenamer
    Command: <location of unzipped file from (1.)>/TVRenamer-&lt;version&gt;/run-linux.sh
    Icon: Can be anything, perhaps [our icon](http://github.com/tvrenamer/tvrenamer/raw/master/src/main/resources/icons/tvrenamer.png)
    *If the application doesn't start, or if you have problems switch the Type to be 'Application in Terminal'*
    1. If you don't add it to the Gnome bar, open an terminal and `cd` to where you unzipped the file to.  Then `cd` into the TVRenamer-&lt;version&gt; folder.  There should be run-linux.sh and tvrenamer.jar file there.
    1. Execute the run script via `./run-linux.sh`

## Common Problems
### Connectivity Issues
If you are using a version that precedes [version 0.7.2](https://github.com/tvrenamer/tvrenamer/releases/tag/0.7.2), and you receive errors about "unable to connect to internet" please download a later version. Note that [Java 8](https://java.com/en/download) is required.

### Java version issues
*Java version 8* is required.  Type `java -version` into your terminal and ensure that the output is similar to:

    $ java -version
    java version "1.8.0_81"
    Java(TM) SE Runtime Environment (build 1.8.0_81-b14)
    Java HotSpot(TM) 64-Bit Server VM (build 24.81-b01, mixed mode)

### x86/ 64 bit architecture version
Ensure that you are running the same architecture of TVRenamer as Java. `java -version` displays the version on the last line, as above. If you don't have it right, you get an unhelpful error message on startup (when running on the terminal), like below:
    Exception in thread "main" java.lang.UnsatisfiedLinkError: Cannot load 32-bit SWT libraries on 64-bit JVM

### "TVRenamer can't be opened because it's from an unidentified developer" error message on OSX Mountain Lion or above.
This is because we have not signed the application with Apple (and because we use Java, they won't allow us to). To get around this, just right-click the app in Finder and select Open. You only need to do this once.
[More information from iMore](http://www.imore.com/how-open-apps-unidentified-developer-os-x-mountain-lion)

## Running in debug mode
If the application crashes it helps us greatly if you can provide us a stacktrace of what went wrong.  In order to do this, you just need to run the application in the terminal, then copy the output into [a new bug report](https://github.com/tvrenamer/tvrenamer/issues/new).

If the application fails to start due to a java error, [ensure that your JAVA_HOME environment variable is set correctly](http://www.oracle.com/technology/sample_code/tech/java/sqlj_jdbc/files/9i_jdbc/EnvSetup.html).

  * On Windows:
    1. Open the Windows Command Prompt (Windows + r, then type `cmd` and push enter)
    1. Navigate to where the TVRenamer application is.
    1. Execute `java -jar TVRenamer-<version>.exe`
  * On OSX:
    1. Open the Terminal application (at /Applications/Utilities/Terminal.app)
    1. Navigate to where the TVRenamer application is.
    1. Execute it via `./TVRenamer-<version>.app/Contents/MacOS/TVRenamer`
       * for older versions, you may need to run `./TVRenamer-<version>.app/Contents/MacOS/JavaAppLauncher`
  * On Linux:
    1. Open the Terminal application (from the Gnome Applications menu)
    1. Navigate to where the TVRenamer application is.
    1. Execute the run script via `./TVRenamer-<version>/run-linux.sh`

## Contributions

If you'd like to run from source, please see the
[Quick Start](https://github.com/tvrenamer/tvrenamer/wiki/Quick-Start)
guide, and then look over
[Development Setup](https://github.com/tvrenamer/tvrenamer/wiki/Development-Setup).

Please see the
[Development Process](https://github.com/tvrenamer/tvrenamer/wiki/Development-Setup#development-process)
section if you'd like to contribute!  Anything from Java code patches to UI/UX
recommendations would be gratefully received.

To contribute to the code side of things you should know Java and it would be helpful if you know some SWT and git. The easiest way to submit changes is via a [github pull request](http://help.github.com/forking/) based off the [master branch](http://github.com/tvrenamer/tvrenamer/tree/master).

For anything else (feature requests, comments, fanmail!), [create a new issue](https://github.com/tvrenamer/tvrenamer/issues/new) and set the label to 'Type-Enhancement'.
