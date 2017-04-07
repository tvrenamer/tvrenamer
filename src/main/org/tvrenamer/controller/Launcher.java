package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.view.UIStarter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Launcher {
    private static Logger logger = Logger.getLogger(Launcher.class.getName());

    // Static initalisation block
    static {
        // Find logging.properties file inside jar
        InputStream loggingConfigStream = Launcher.class.getResourceAsStream(LOGGING_PROPERTIES);

        if (loggingConfigStream == null) {
            System.err.println("Warning: logging properties not found.");
        } else {
            try {
                LogManager.getLogManager().readConfiguration(loggingConfigStream);
            } catch (IOException e) {
                System.err.println("Exception thrown while loading logging config");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        UserPreferences prefs = UserPreferences.getInstance();

        UIStarter ui = new UIStarter();
        ui.run();
    }
}
