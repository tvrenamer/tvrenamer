package org.tvrenamer.controller;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.util.Environment;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
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

    /**
     * Change a file's permissions to be not writable, and not readable by others.
     * Make the file readable and executable by the owner; this method can be used
     * for both regular files and directories, and for a directory, to be able to
     * access the contents, it needs to be executable.  For a regular file, there's
     * no reason to make it executable, but it does no harm.
     *
     * @param path
     *    the file to set as read-only
     * @return
     *    true if we were able to set the file to read-only; false if we cannot
     */
    public static boolean setReadOnly(final Path path) {
        if (Environment.IS_WINDOWS) {
            // The POSIX permissions are hopeless, so try reverting to the old
            // java.io functionality.  But this really doesn't work, either.
            // TODO: find a library that we can use, just for testing, that
            // will set ACLs on Windows file systems to actually make the
            // directory not-modifiable.
            return path.toFile().setReadOnly();
        } else {
            try {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(path, perms);
                return true;
            } catch (UnsupportedOperationException ue) {
                return false;
            } catch (IOException ioe) {
                return false;
            }
        }
    }

    /**
     * Change a file's permissions to be writable, readable, and executable,
     * by everyone.
     *
     * @param path
     *    the file to set as writable
     * @return
     *    true if we were able to set the permissions on the file;
     *    false if we cannot
     */
    public static boolean setWritable(final Path path) {
        if (Environment.IS_WINDOWS) {
            return path.toFile().setWritable(true, false);
        } else {
            try {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_READ);
                perms.add(PosixFilePermission.GROUP_WRITE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_READ);
                perms.add(PosixFilePermission.OTHERS_WRITE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(path, perms);
                return true;
            } catch (UnsupportedOperationException ue) {
                return false;
            } catch (IOException ioe) {
                return false;
            }
        }
    }
}
