#!/bin/sh
BASEDIR=`dirname "$0"`
java -XstartOnFirstThread -jar "$BASEDIR/tvrenamer.jar" com.google.code.tvrenamer.view.UIStarter
