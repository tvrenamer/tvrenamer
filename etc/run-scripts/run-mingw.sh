#!/bin/bash

# run-mingw.sh -- quick and dirty (albeit heavily-commented) script to run
#  the TVRenamer app without "launch4j" or any similar functionality.

# This will optionally run "ant compile", which builds a bunch of class
# files in the "out" directory.  (If you don't enable that option, we
# assume it's because you've already run ant.)  The ant task
# doesn't copy the version file, so we do that as an explicit step here.

# I wrote this to be run from "git bash" for Windows, which is mingw32.
# It has specific functionality to use Windows paths, and to choose the
# right SWT jarfile.  To run on a different platform, the script would
# need to be edited slightly.  To try to make sure you do that, it actually
# doesn't run on any other platforms.  But again, trivial to change that.

# This script automatically redirects stdout into a hard-coded location.
# It would be very easy to edit the script if you don't like that.

# If anyone wants to run anywhere else, make it work
if [ "`uname`" != "MINGW32_NT-6.2" ]
then
  echo "Script only tested on MinGW 6.2.  Edit it for your platform."
  exit 1
fi

# libraries -- hard-coded.  The first one is platform-specific.
libraries="swt-win64-4.3.jar commons-codec-1.4.jar jedit-4.3.2-IOUtilities.jar xpp3_min-1.1.4.jar xstream-1.3.1.jar"

# Other Windows Bourne shells, like Cygwin, provide specific functionality
# for going between Unix-style and Windows-style paths.  I don't find any
# for mingw32.  Note we don't need to change slashes.  Java is ok with
# forward slashes on Windows.  But it doesn't understand mingw's virtual
# drives, so we need a regular drive letter.  (If your files are on a
# different drive than C:, this would need to be edited.)
windowsize ()
{
  echo $1 | sed 's,^/c/,c:/,'
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

# Copy the version file.  This, and other things, are done by the build file
# if you build the jar, but that wants launch4j, and takes longer in any case.
# We can run just using the class files, with this one addition.
cp ${pdir}/src/main/tvrenamer.version ${pdir}/out/

# Library files are checked in here
libdir=${pdir}/lib/main

CLASSPATH=${proj}/out
for lib in ${libraries}
do
  CLASSPATH=${CLASSPATH}';'`windowsize ${libdir}/${lib}`
done
export CLASSPATH

java com.google.code.tvrenamer.view.UIStarter
