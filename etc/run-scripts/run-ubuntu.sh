#!/bin/bash

# run-ubuntu.sh -- quick and dirty (albeit heavily-commented) script to run
#  the TVRenamer app without "launch4j" or any similar functionality.

# This will optionally run "ant compile", which builds a bunch of class
# files in the "out" directory.  (If you don't enable that option, we
# assume it's because you've already run ant.)

# There are other scripts in this directory for other platforms.  If anyone
# wants to run on a platform which currently lacks a script, edit one of
# the scripts, and make it work!
if [ "`uname`" != "Linux" ]
then
  echo "Script only tested on Linux.  Edit it for your platform."
  exit 1
fi

# libraries -- hard-coded.  The first one is platform-specific.
libs="org.eclipse.swt.gtk.linux.x86_64-4.3.jar commons-codec-1.4.jar xstream-1.4.9.jar xmlpull-1.1.3.1.jar xpp3_min-1.1.4c.jar okhttp-3.8.0.jar okio-1.13.0.jar"

usage ()
{
  echo "$0 [ -build ]"
  exit 3
}

dobuild=0

# Make sure we weren't given too many arguments
if [ -n "$2" ]
then
  echo "Error: too many arguments"
  usage
fi

# Parse the argument if there is one
if [ -n "$1" ]
then
  if [ "$1" = "-build" ]
  then
    shift
    dobuild=1
  else
    echo "Error: unrecognized argument $1"
    usage
  fi
fi

# If the configuration is in the older style, transform it.
# This actually is also done by the UserPreferences class, but simpler to
# take care of it here, beforehand.
userhome=${HOME}

if [ ! -d ${userhome}/.tvrenamer ]
then
  if [ -f ${userhome}/.tvrenamer ]
  then
    /bin/mv ${userhome}/.tvrenamer ${userhome}/prefs.xml
    /bin/mkdir ${userhome}/.tvrenamer
    /bin/mv ${userhome}/prefs.xml ${userhome}/.tvrenamer/prefs.xml
  fi
fi

# The script should be runnable from anywhere.  But the JVM needs to be
# launched from the top level of the project directory.  (Otherwise it
# doesn't find the necessary resources.  This could presumably be fixed,
# but it doesn't seem worth the trouble.  It's easy enough to just cd
# before launching the Java process.
#
# Stash the current directory in case we need to come back to it.
startdir=${PWD}

# Figure out where the script lives.  We "know" where it is relative to
# the project root, so we can get to the project root by starting at
# the script.
proj=`dirname $0`
cd ${proj}/../..
pdir=${PWD}

# Now we're in the project's top level directory.  We can run ant, if
# so requested.
if [ "${dobuild}" = "1" ]
then
  echo "building"
  shift
  ant compile || exit 2
fi

# Could return to where we started, but then resources are not found
# cd $startdir

# Library files are downloaded here
ivydir=${pdir}/lib

CLASSPATH=${pdir}/out/main
for lib in ${libs}
do
  CLASSPATH=${CLASSPATH}':'${ivydir}/${lib}
done
export CLASSPATH

java org.tvrenamer.controller.Launcher $*
