// # This file is checked in as a test resource.
// # It is not usable from its checked-in location
// # To use, copy it to src/test/java/org/tvrenamer/controller/TestMain.java
// # and then use etc/run-scripts/run-test.sh
// # (or just run the script first, and it will put the file into place)
// # Then edit the file in src/test/java/org/tvrenamer/controller/TestMain.java
// # to test whatever you want to test.

package org.tvrenamer.controller;

import java.util.logging.Logger;

public class TestMain {
    private static final Logger logger = Logger.getLogger(TestMain.class.getName());

    /**
     * Test any arbitrary code, within or used by TVRenamer.
     *
     * @param args
     *   whatever the user passed in on the command line
     */
    public static void main(String[] args) {
        // You may or may not want to initialize the logger as TVRenamer does.
        Launcher.initializeLogger();

        // Replace this with whatever code you want to test out
        logger.info("hello, tvrenamer!");
    }
}
