package org.tvrenamer.controller.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class FileUtilsTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setLogging() {
        FileUtilities.loggingOff();
    }

    @Test
    public void testEnsureWritableDirectory() {
        final String dirname = "folder";

        final Path sandbox = tempFolder.getRoot().toPath();

        final Path dirpath = sandbox.resolve(dirname);
        assertFalse("cannot test ensureWritableDirectory because target already exists",
                    Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   FileUtilities.ensureWritableDirectory(dirpath));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(dirpath));

        assertTrue("rmdirs returned false", FileUtilities.rmdir(dirpath));
        assertFalse("dir from rmdirs not removed", Files.exists(dirpath));
    }

    @Test
    public void testEnsureWritableDirectoryAlreadyExists() {
        final Path dirpath = tempFolder.getRoot().toPath();

        assertTrue("cannot test ensureWritableDirectory because sandbox does not exist",
                   Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   FileUtilities.ensureWritableDirectory(dirpath));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(dirpath));
    }

    @Test
    public void testEnsureWritableDirectoryFileInTheWay() {
        final String dirname = "file";
        Path dirpath;

        try {
            dirpath = tempFolder.newFile(dirname).toPath();
        } catch (IOException ioe) {
            fail("cannot test ensureWritableDirectory because newFile failed");
            return;
        }

        assertTrue("cannot test ensureWritableDirectory because file does not exist",
                   Files.exists(dirpath));

        assertFalse("ensureWritableDirectory returned true when file was in the way",
                    FileUtilities.ensureWritableDirectory(dirpath));
        assertTrue("file from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertFalse("file from ensureWritableDirectory is a directory",
                    Files.isDirectory(dirpath));
    }

    @Test
    public void testEnsureWritableDirectoryCantWrite() {
        final String dirname = "folder";
        File myFolder;

        try {
            myFolder = tempFolder.newFolder(dirname);
        } catch (Exception e) {
            fail("cannot test ensureWritableDirectory because newFolder failed");
            return;
        }

        Path dirpath = myFolder.toPath();
        assertTrue("cannot test ensureWritableDirectory because folder does not exist",
                   Files.exists(dirpath));

        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(dirpath, perms);
        } catch (UnsupportedOperationException ue) {
            // If this platform can't support POSIX file permissions, then we just
            // punt.  We can't properly test it, so there is no failure.
            return;
        } catch (IOException ioe) {
            fail("cannot test ensureWritableDirectory because newFile failed");
            return;
        }

        assertFalse("failed to make temp dir not writable", Files.isWritable(dirpath));

        assertFalse("ensureWritableDirectory returned true when folder was not writable",
                    FileUtilities.ensureWritableDirectory(dirpath));
        assertTrue("file from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("file from ensureWritableDirectory is a directory",
                   Files.isDirectory(dirpath));
    }
}
