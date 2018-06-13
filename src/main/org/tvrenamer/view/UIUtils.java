package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

class UIUtils {
    private static final Logger logger = Logger.getLogger(UIUtils.class.getName());

    private UIUtils() {
        // utility class; prevent instantiation
    }

    /**
     * Read an image.
     *
     * @param resourcePath
     *     the relative path to try to locate the file as a resource
     * @param filePath
     *     the path to try to locate the file directly in the file system
     * @return an Image read from the given path
     */
    public static Image readImageFromPath(final String resourcePath,
                                          final String filePath)
    {
        Display display = Display.getCurrent();
        Image rval = null;
        try (InputStream in = UIUtils.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                rval = new Image(display, in);
            }
        } catch (IOException ioe) {
            logger.warning("exception trying to read image from stream " + resourcePath);
        }
        if (rval == null) {
            rval = new Image(display, filePath);
        }
        return rval;
    }

    /**
     * Read an image.
     *
     * @param resourcePath
     *     the relative path to try to locate the file as a resource
     * @return an Image read from the given path
     */
    public static Image readImageFromPath(final String resourcePath) {
        return readImageFromPath(resourcePath, ICON_PARENT_DIRECTORY + "/" + resourcePath);
    }
}
