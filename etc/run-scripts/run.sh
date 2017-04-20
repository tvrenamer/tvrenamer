#!/bin/bash

# run.sh -- in theory, detects your platform and runs the right script.
#  In practice, used to redirect run-mingw to a logfile, without bothering
#  to learn how to properly configure the logger.

# logdir -- change this if you want the logfile written elsewhere
logdir=~/Documents/Logs

# The program doesn't appear to print to stdout; redirect stderr to log file.
# I'm sure the logger is configurable to do this directly.  TODO: do that
`dirname $0`/run-mingw.sh $* 2> ${logdir}/tvrenamer.log
