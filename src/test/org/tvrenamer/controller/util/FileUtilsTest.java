package org.tvrenamer.controller.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.tvrenamer.controller.util.FileUtilities.*;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public void testExistingAncestor() {
        final String dirname = "folder";

        final Path sandbox = tempFolder.getRoot().toPath();

        final Path dirpath = sandbox.resolve(dirname);
        assertTrue("cannot test existingAncestor because can't ensure writable directory"
                    + dirpath, ensureWritableDirectory(dirpath));

        assertEquals("existingAncestor(Path) failed to recognize path itself exists",
                     dirpath, existingAncestor(dirpath));

        final String dirpathName = dirpath.toString();
        assertEquals("existingAncestor(String) failed to recognize path itself exists",
                     dirpath, existingAncestor(dirpathName));

        Path uncreatable = Paths.get("/Usurs/me/Documents/oops");
        assertEquals("existingAncestor(Path) failed to find root as answer for " + uncreatable,
                     uncreatable.getRoot(), existingAncestor(uncreatable));
    }

    public void testExistingAncestorSymlinks() {
        final String dirname = "folder";

        final Path sandbox = tempFolder.getRoot().toPath();

        final Path dirpath = sandbox.resolve(dirname);
        assertTrue("cannot test existingAncestor because can't ensure writable directory"
                    + dirpath, ensureWritableDirectory(dirpath));

        // Create a "normal" symbolic link to dirpath
        Path validLink = sandbox.resolve("slink");
        String firstSubDir = "showname";
        Path toBeUnderLink = validLink.resolve(firstSubDir).resolve("season").resolve("episode");
        assertEquals("existingAncestor(Path) failed to find " + sandbox + " as answer for "
                     + validLink, sandbox, existingAncestor(validLink));
        assertEquals("existingAncestor(Path) failed to find " + sandbox + " as answer for "
                     + toBeUnderLink, sandbox, existingAncestor(toBeUnderLink));

        try {
            Files.createSymbolicLink(validLink, dirpath);
        } catch (IOException x) {
            fail("unable to create link from " + validLink + " to " + dirpath);
        }
        assertTrue("did not detect " + validLink + " as a symbolic link",
                   Files.isSymbolicLink(validLink));
        assertFalse("after creating link, " + dirpath + " not exists",
                    Files.notExists(dirpath));
        assertFalse("after creating link, " + validLink + " not exists",
                    Files.notExists(validLink));
        assertEquals("after link, existingAncestor(Path) failed to find itself"
                     + " as answer for " + dirpath,
                     dirpath, existingAncestor(dirpath));
        assertEquals("after link, existingAncestor(Path) failed to find itself"
                     + " as answer for " + validLink,
                     validLink, existingAncestor(validLink));
        assertEquals("existingAncestor(Path) failed to find " + validLink + " as answer for "
                     + toBeUnderLink, validLink, existingAncestor(toBeUnderLink));

        final Path subdir = dirpath.resolve(firstSubDir);
        assertFalse("cannot do ensureWritableDirectory because target already exists",
                    Files.exists(subdir));
        assertTrue("ensureWritableDirectory returned false",
                   ensureWritableDirectory(subdir));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(subdir));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(subdir));

        //////////////////////////////////////////////////////////////
        // We're going to do a very bad thing here.  We're going to create a recursive symbolic link.
        // There's no useful purpose for such a thing, and it should never be done, except in this
        // situation: when you want to make sure your code could handle such a erroneous situation,
        // gracefully.  We're making:  <tmpdir>/a -> <tmpdir>/a/b/c/d
        //
        // The somewhat surprising result is, Files.notExists() returns false on the *target*.
        // That is, it says "<tmpdir>/a/b/c/d" does NOT not exist.  (It also says it does not exist.
        // That's the whole reason why there are two methods.  Certain paths may be in a state where
        // they neither "exist" nor "not exist".  To "not exist" means to be completely absent.)
        Path aSubDir = dirpath.resolve("a");
        Path target = aSubDir.resolve("b").resolve("c").resolve("d");
        assertEquals("existingAncestor(Path) failed to find " + dirpath + " as answer for "
                     + target, dirpath, existingAncestor(target));

        try {
            Files.createSymbolicLink(aSubDir, target);
        } catch (IOException x) {
            fail("unable to create link from " + aSubDir + " to " + target);
        }
        assertTrue("did not detect " + aSubDir + " as a symbolic link",
                   Files.isSymbolicLink(aSubDir));
        assertFalse("after creating link, " + target + " not exists",
                    Files.notExists(target));
        assertEquals("existingAncestor(Path) failed to find itself as answer for " + target,
                     target, existingAncestor(target));
    }

    @Test
    public void testEnsureWritableDirectory() {
        final String dirname = "folder";

        final Path sandbox = tempFolder.getRoot().toPath();

        final Path dirpath = sandbox.resolve(dirname);
        assertFalse("cannot test ensureWritableDirectory because target already exists",
                    Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   ensureWritableDirectory(dirpath));
        assertTrue("dir from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("dir from ensureWritableDirectory not a directory",
                   Files.isDirectory(dirpath));

        assertTrue("rmdirs returned false", rmdir(dirpath));
        assertFalse("dir from rmdirs not removed", Files.exists(dirpath));
    }

    @Test
    public void testEnsureWritableDirectoryAlreadyExists() {
        final Path dirpath = tempFolder.getRoot().toPath();

        assertTrue("cannot test ensureWritableDirectory because sandbox does not exist",
                   Files.exists(dirpath));

        assertTrue("ensureWritableDirectory returned false",
                   ensureWritableDirectory(dirpath));
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
                    ensureWritableDirectory(dirpath));
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
            // If this file system can't support POSIX file permissions, then we just
            // punt.  We can't properly test it, so there is no failure.
            return;
        } catch (IOException ioe) {
            fail("cannot test ensureWritableDirectory because newFile failed");
            return;
        }

        assertFalse("failed to make temp dir not writable", Files.isWritable(dirpath));

        assertFalse("ensureWritableDirectory returned true when folder was not writable",
                    ensureWritableDirectory(dirpath));
        assertTrue("file from ensureWritableDirectory not found",
                   Files.exists(dirpath));
        assertTrue("file from ensureWritableDirectory is a directory",
                   Files.isDirectory(dirpath));
    }
}
