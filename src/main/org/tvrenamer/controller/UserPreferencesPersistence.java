package org.tvrenamer.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.tvrenamer.model.UserPreferences;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserPreferencesPersistence {
    private static final Logger logger = Logger.getLogger(UserPreferencesPersistence.class.getName());

    // Use reflection provider so the default constructor is called, thus calling the superclass constructor
    // Instantiate the object so the Observable superclass is called corrected
    private static final XStream xstream = new XStream(new PureJavaReflectionProvider());

    static {
        xstream.alias("preferences", UserPreferences.class);
        xstream.omitField(UserPreferences.class, "proxy");
        // Make the fields of Observable transient
        xstream.omitField(Observable.class, "obs");
        xstream.omitField(Observable.class, "changed");
    }

    /**
     * Save the preferences object to the path.
     * @param prefs the preferences object to save
     * @param path the path to save it to
     */
    @SuppressWarnings("SameParameterValue")
    public static void persist(UserPreferences prefs, Path path) {
        String xml = xstream.toXML(prefs);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(xml);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.log(Level.SEVERE, "Exception occurred when writing preferences file", e);
        }
    }

    /**
     * Load the preferences from path.
     * @param path the path to read
     * @return the populated preferences object
     */
    @SuppressWarnings("SameParameterValue")
    public static UserPreferences retrieve(Path path) {
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return (UserPreferences) xstream.fromXML(in);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception reading preferences file '"
                           + path.toAbsolutePath().toString(), e);
                logger.info("assuming default preferences");
            }
        } else {
            // If file doesn't exist, assume defaults
            logger.log(Level.FINE, "Preferences file '" + path.toAbsolutePath().toString()
                       + "' does not exist - assuming defaults");
        }

        return null;
    }
}
