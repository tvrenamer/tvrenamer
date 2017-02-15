package org.tvrenamer.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.GlobalOverrides;

public class GlobalOverridesPersistence {
    private static Logger logger = Logger.getLogger(GlobalOverridesPersistence.class.getName());

    // Use reflection provider so the default constructor is called, thus calling the superclass constructor
    private static final XStream xstream = new XStream(new PureJavaReflectionProvider());

    static {
        xstream.alias("overrides", GlobalOverrides.class);
    }

    /**
     * Save the overrides object to the file.
     *
     * @param prefs
     *            the overrides object to save
     * @param file
     *            the file to save it to
     */
    public static void persist(GlobalOverrides overrides, File file) {
        String xml = xstream.toXML(overrides);
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(xml);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occured when writing overrides file", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception occured when closing overrides file", e);
            }
        }
    }

    /**
     * Load the overrides from file.
     *
     * @param file
     *            the file to read
     * @return the populated overrides object
     */
    public static GlobalOverrides retrieve(File file) {
        // Instantiate the object so the Observable superclass is called corrected
        GlobalOverrides overrides = null;

        try {
            overrides = (GlobalOverrides) xstream.fromXML(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            // If file doesn't exist, assume defaults
            return null;
        }

        return overrides;
    }
}
