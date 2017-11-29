package org.tvrenamer.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.AppData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppDataPersistence {
    private static final Logger logger = Logger.getLogger(AppDataPersistence.class.getName());

    // Use reflection provider so the default constructor is called, thus calling the superclass constructor
    // Instantiate the object so the Observable superclass is called corrected
    private static final XStream xstream = new XStream(new PureJavaReflectionProvider());

    static {
        xstream.alias("appdata", AppData.class);
        xstream.omitField(AppData.class, "proxy");
        // Make the fields of Observable transient
        xstream.omitField(Observable.class, "obs");
        xstream.omitField(Observable.class, "changed");
    }

    /**
     * Save the app data object to the path.
     * @param data the app data object to save
     * @param path the path to save it to
     */
    @SuppressWarnings("SameParameterValue")
    public static void persist(AppData data, Path path) {
        String xml = xstream.toXML(data);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(xml);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.log(Level.SEVERE, "Exception occurred when writing app data file", e);
        }
    }

    /**
     * Load the app data from path.
     * @param path the path to read
     * @return the populated app data object
     */
    @SuppressWarnings("SameParameterValue")
    public static AppData retrieve(Path path) {
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return (AppData) xstream.fromXML(in);
            } catch (IllegalArgumentException | UnsupportedOperationException
                     | IOException  | SecurityException e)
            {
                logger.log(Level.SEVERE, "Exception reading app data file '"
                           + path.toAbsolutePath().toString(), e);
                logger.info("assuming default app data");
            }
        } else {
            // If file doesn't exist, assume defaults
            logger.log(Level.FINE, "App Data file '" + path.toAbsolutePath().toString()
                       + "' does not exist - assuming defaults");
        }

        return AppData.getInstance();
    }
}
