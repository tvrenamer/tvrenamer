#!/bin/bash

# run-osx.sh -- quick and dirty (albeit heavily-commented) script to run
#  the TVRenamer app without "launch4j" or any similar functionality.

# This will optionally run "ant compile", which builds a bunch of class
# files in the "out" directory.  (If you don't enable that option, we
# assume it's because you've already run ant.)

# This script automatically redirects stdout into a hard-coded location.
# It would be very easy to edit the script if you don't like that.

# If anyone wants to run anywhere else, make it work
if [ "`uname`" != "Darwin" ]
then
  echo "Script only tested on Darwin.  Edit it for your platform."
  exit 1
fi

# libraries -- hard-coded.  The first one is platform-specific.
loclibs="swt-osx64-4.3.jar commons-codec-1.4.jar xstream-1.4.9.jar xmlpull-1.1.3.1.jar xpp3_min-1.1.4c.jar"

usage ()
{
  echo "Error: unrecognized argument $1"
  echo "$0 [ -build ]"
  exit 3
}

# The script should be runnable from anywhere.  Stash the current location
# to come back to it.
startdir=`pwd`

userhome=${HOME}

# If the configuration is in the older style, transform it.
# This actually is also done by the UserPreferences class, but simpler to
# take care of it here, beforehand.
if [ ! -d ${userhome}/.tvrenamer ]
then
  if [ -f ${userhome}/.tvrenamer ]
  then
    /bin/mv ${userhome}/.tvrenamer ${userhome}/prefs.xml
    /bin/mkdir ${userhome}/.tvrenamer
    /bin/mv ${userhome}/prefs.xml ${userhome}/.tvrenamer/prefs.xml
  fi
fi

# Figure out where the script lives.  That tells us the project's root directory.
proj=`dirname $0`
cd ${proj}/../..
pdir=`pwd`
proj=$pdir

if [ -n "$1" ]
then
  if [ "$1" = "-build" ]
  then
    shift
    ant compile || exit 2
  fi
fi

# Return to where we started
cd $startdir

# Library files are checked in here
loclibdir=${pdir}/jars/main

CLASSPATH=${proj}/out
for lib in ${loclibs}
do
  CLASSPATH=${CLASSPATH}':'${loclibdir}/${lib}
done
export CLASSPATH

java -XstartOnFirstThread org.tvrenamer.controller.Launcher $*
