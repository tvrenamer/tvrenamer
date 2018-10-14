#!/bin/bash

# run-test.sh -- run a test main program
#
# This script sets up the environment so that all TVRenamer main classes,
# all TVRenamer test classes, and all TVRenamer libraries are in the classpath,
# and then runs the main method of org.tvrenamer.controller.TestMain.
#
# "src/test/java/org/tvrenamer/controller/TestMain.java" is not checked in
# to git; when you do a fresh clone, it will not exist.  And it should not
# be checked in; in fact, it is designated in .gitignore as ignorable.
#
# TestMain.java is intended to be used for whatever temporary purpose you
# are interested in testing while doing development.  Put your code in
# there, run this script, and as you learn things about how the code behaves,
# put that code into the real classes!

os=`uname`

# We assume 64-bit
if [ "$MSYSTEM" != "MINGW32" ]
then
  libs="org.eclipse.swt.win32.win32.x86_64-4.3.jar"
elif [ "${os}" = "Darwin" ]
then
  libs="org.eclipse.swt.cocoa.macosx.x86_64-4.3.jar"
else
  libs="org.eclipse.swt.gtk.linux.x86_64-4.3.jar"
fi

# other libraries -- hard-coded.
libs="${libs} junit-4.12.jar commons-codec-1.4.jar xstream-1.4.9.jar xmlpull-1.1.3.1.jar xpp3_min-1.1.4c.jar okhttp-3.8.0.jar okio-1.13.0.jar"

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

gfile=src/test/resources/TestMain.java
jfile=src/test/java/org/tvrenamer/controller/TestMain.java

if [ ! -f "${jfile}" ]
then
  egrep -v '#' ${gfile} > ${jfile}
fi

# Now we're in the project's top level directory.  We can run ant, if
# so requested.
if [ "${dobuild}" = "1" ]
then
  echo "building"
  shift
  ant compile.test || exit 2
fi

# Could return to where we started, but then resources are not found
# cd $startdir

# Library files are downloaded here
ivydir=${pdir}/lib

CLASSPATH=${pdir}/out/main:${pdir}/out/test
for lib in ${libs}
do
  CLASSPATH=${CLASSPATH}':'${ivydir}/${lib}
done
export CLASSPATH

java org.tvrenamer.controller.TestMain $*
