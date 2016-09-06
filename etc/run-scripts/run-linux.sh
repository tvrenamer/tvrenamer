#!/bin/sh

#Determine if java exists
which java > /dev/null
javaExists=$?
if [ ${javaExists} -eq 1 ]; then
  echo 'TVRenamer ERROR Java not found on your system. Please install.'
  #Determine if zenity notifier exists
  which zenity > /dev/null
  zenityExists=$?
  if [ ${zenityExists} -eq 0 ]; then
    zenity --error \
       --title 'TVRenamer' \
       --text="Java was not found on your system. \n\nPlease install the Java Runtime Environment (JRE) by: \n* using your distribution\'s package manager or; \n* manually downloading and installing from the JRE from \nhttp://www.oracle.com/technetwork/java/javase/downloads/index.html\n\nFor help google \'install java on [distribution]\'"
    exit
  fi
fi

BASEDIR=$(dirname "$0")
java -jar "${BASEDIR}/tvrenamer.jar"