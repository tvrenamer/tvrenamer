#!/bin/sh
BASEDIR=`dirname "$0"`
java -jar "$BASEDIR/tvrenamer.jar" -Xdebug -Xrunjdwp:server=y, transport=dt_socket,address=4000, suspend=n com.google.code.tvrenamer.view.ConsoleStarter
