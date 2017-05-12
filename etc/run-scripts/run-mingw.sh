#!/bin/bash

# run-mingw.sh -- quick and dirty (albeit heavily-commented) script to run
#  the TVRenamer app without "launch4j" or any similar functionality.

# This will optionally run "ant compile", which builds a bunch of class
# files in the "out" directory.  (If you don't enable that option, we
# assume it's because you've already run ant.)

# I wrote this to be run from "git bash" for Windows, which is mingw32.
# It has specific functionality to use Windows paths, and to choose the
# right SWT jarfile.  To run on a different platform, the script would
# need to be edited slightly.  To try to make sure you do that, it actually
# doesn't run on any other platforms.  But again, trivial to change that.

# This script automatically redirects stdout into a hard-coded location.
# It would be very easy to edit the script if you don't like that.

# If anyone wants to run anywhere else, make it work
if [ "$MSYSTEM" != "MINGW32" ]
then
  echo "Script only tested on MinGW 6.1 and 6.2.  Edit it for your platform."
  exit 1
fi

# local libraries -- hard-coded.  The first one is platform-specific.
loclibs="swt-win64-4.3.jar commons-codec-1.4.jar xstream-1.4.9.jar xmlpull-1.1.3.1.jar xpp3_min-1.1.4c.jar"

# Other Windows Bourne shells, like Cygwin, provide specific functionality
# for going between Unix-style and Windows-style paths.  I don't find any
# for mingw32.  Note we don't need to change slashes.  Java is ok with
# forward slashes on Windows.  But it doesn't understand mingw's virtual
# drives, so we need a regular drive letter.
windowsize ()
{
  echo $1 | sed 's,^/\([a-zA-Z]\)/,\1:/,'
}

usage ()
{
  echo "Error: unrecognized argument $1"
  echo "$0 [ -build ]"
  exit 3
}

# The script should be runnable from anywhere.  Stash the current location
# to come back to it.
startdir=`pwd`

# If the configuration is in the older style, transform it.
# This actually is also done by the UserPreferences class, but simpler to
# take care of it here, beforehand.
if [ ! -d ~/.tvrenamer ]
then
  if [ -f ~/.tvrenamer ]
  then
    /bin/mv ~/.tvrenamer ~/prefs.xml
    /bin/mkdir ~/.tvrenamer
    /bin/mv ~/prefs.xml ~/.tvrenamer/prefs.xml
  fi
fi

# Figure out where the script lives.  That tells us the project's root directory.
proj=`dirname $0`
cd ${proj}/../..
pdir=`pwd`
proj=`windowsize $pdir`

if [ -n "$1" ]
then
  if [ "$1" = "-build" ]
  then
    shift
    if [ -n "$1" ]
    then
      usage $1
    fi
    ant compile || exit 2
  else
    usage $1
  fi
fi

# Return to where we started
cd $startdir

# Library files are checked in here
loclibdir=${pdir}/jars/main

CLASSPATH=${proj}/out
for lib in ${loclibs}
do
  CLASSPATH=${CLASSPATH}';'`windowsize ${loclibdir}/${lib}`
done
export CLASSPATH

java org.tvrenamer.controller.Launcher
