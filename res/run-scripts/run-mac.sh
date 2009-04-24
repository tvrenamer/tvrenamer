#!/bin/bash
java -XstartOnFirstThread -classpath lib/swt-macosx.jar:lib/log4j.jar:tvrenamer.jar:. -Djava.library.path=lib com.google.code.tvrenamer.view.UIStarter
