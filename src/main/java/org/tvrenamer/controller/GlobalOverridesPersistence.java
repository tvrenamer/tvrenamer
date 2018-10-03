package org.tvrenamer.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.GlobalOverrides;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalOverridesPersistence {
    private static final Logger logger = Logger.getLogger(GlobalOverridesPersistence.class.getName());

    // Use reflection provider so the default constructor is called, thus calling the superclass constructor
    private static final XStream xstream = new XStream(new PureJavaReflectionProvider());

    static {
        xstream.alias("overrides", GlobalOverrides.class);
    }

    /**
     * Save the overrides object to the file.
     *
     * @param overrides
     *            the overrides object to save
     * @param path
     *            the path to save it to
     */
    @SuppressWarnings("SameParameterValue")
    public static void persist(GlobalOverrides overrides, Path path) {
        String xml = xstream.toXML(overrides);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(xml);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occurred when writing overrides file", e);
        }
    }

    /**
     * Load the overrides from path.
     *
     * @param path
     *            the path to read
     * @return the populated overrides object
     */
    @SuppressWarnings("SameParameterValue")
    public static GlobalOverrides retrieve(Path path) {
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return (GlobalOverrides) xstream.fromXML(in);
            } catch (IllegalArgumentException | UnsupportedOperationException
                     | IOException  | SecurityException e)
            {
                logger.log(Level.SEVERE, "Exception reading overrides file '"
                           + path.toAbsolutePath().toString(), e);
                logger.info("assuming no overrides");
            }
        }

        // If file doesn't exist, assume defaults
        return null;
    }
}
