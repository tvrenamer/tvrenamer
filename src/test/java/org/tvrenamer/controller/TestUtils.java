package org.tvrenamer.controller;

import org.tvrenamer.controller.util.FileUtilities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestUtils extends FileUtilities {
    static final Logger logger = Logger.getLogger(TestUtils.class.getName());

    public static final Charset TVDB_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Creates a file.  In order to have it not be an empty file, will write the
     * filepath into the file upon creation.
     *
     * However, note that if the file already exists, this method does not attempt
     * to overwrite it or change it in any way.  This method assumes that the caller
     * simply wants the file to exist, and doesn't actually care about its contents.
     *
     * @param rootDir - a directory below which to create the file.  Does not need
     *    to exist beforehand.
     * @param filepath - the rest of the path of the file to create.  This obviously
     *    includes the filename, at the end.  It may or may not include other
     *    subdirectories to be created under the rootDir.
     * @return
     *    true if the the file exists at the conclusion of this method:
     *    that is, true if the file already existed, or if we created it;
     *    false if it we could not create the file
     */
    public static boolean createFile(final Path rootDir, final String filepath) {
        Path file = rootDir.resolve(filepath);
        if (Files.notExists(file)) {
            try {
                Path parent = file.getParent();
                boolean madeDir = mkdirs(parent);
                if (madeDir) {
                    Files.createFile(file);
                    // Zero-byte files are anomalous.  To give the file some content,
                    // simply write its own filepath, into the file.
                    Files.write(file, filepath.getBytes(TVDB_CHARSET));
                } else {
                    logger.warning("unable to create directory " + parent);
                    return false;
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "exception trying to create file " + file, ioe);
                return false;
            }
        }
        return Files.exists(file);
    }
}
