package org.tvrenamer.controller;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.FileMoveIcon;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.view.FileCopyMonitor;
import org.tvrenamer.view.UIStarter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class FileMover implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(FileMover.class.getName());

    private final Path destPath;

    private final TableItem item;

    private final FileEpisode episode;

    private final Label progressLabel;

    private final Display display;

    public FileMover(Display display, FileEpisode src, Path destPath, TableItem item, Label progressLabel) {
        this.display = display;
        this.episode = src;
        this.destPath = destPath;
        this.item = item;
        this.progressLabel = progressLabel;
    }

    public Boolean call(FileCopyMonitor monitor) {
        Path srcPath = episode.getPath();
        Path destDir = destPath.getParent();
        if (Files.notExists(destDir)) {
            FileUtilities.mkdirs(destDir);
        }
        if (Files.exists(destDir) && Files.isDirectory(destDir)) {
            UIStarter.setTableItemStatus(display, item, FileMoveIcon.RENAMING);
            boolean succeeded = false;
            try {
                if (FileUtilities.areSameDisk(srcPath, destDir)) {
                    Path actualDest = Files.move(srcPath, destPath);
                    succeeded = Files.isSameFile(destPath, actualDest);
                }
                if (!succeeded) {
                    long size = Files.size(srcPath);
                    monitor.setMaximum(size);
                    succeeded = FileUtilities.copyAndDelete(srcPath, destPath, monitor);
                }
                if (succeeded) {
                    updateFileModifiedDate(destPath);
                    episode.setPath(destPath);
                    logger.info("Moved " + srcPath.toAbsolutePath() + " to " + destPath.toAbsolutePath());
                    UIStarter.setTableItemStatus(display, item, FileMoveIcon.SUCCESS);
                } else {
                    logger.severe("Unable to move " + srcPath.toAbsolutePath() + " to "
                                  + destPath.toAbsolutePath());
                    UIStarter.setTableItemStatus(display, item, FileMoveIcon.FAIL);
                }
            } catch (IOException ioe) {
                logger.warning("IO Exception");
            }
            return succeeded;
        } else {
            logger.severe("Unable to move file to " + destPath.toAbsolutePath().toString());
        }
        return false;
    }

    @Override
    public Boolean call() {
        FileCopyMonitor monitor = new FileCopyMonitor(display, progressLabel);
        Boolean status = null;
        try {
            status = call(monitor);
        } catch (Exception e) {
            logger.warning("exception caught doing file move: " + e);
        } finally {
            // We only use the label if we're doing the "copy-and-delete" method.
            // But we have created it beforehand, not knowing which method we'd be using.
            // So, regardless of how we tried to move the file, and regardless of whether
            // it succeeded or not, we need to get rid of the label.
            monitor.cleanUp();
        }
        return status;
    }

    private void updateFileModifiedDate(Path path)
        throws IOException
    {
        FileTime timestamp = FileTime.from(Instant.now());

        // update the modified time on the file, the parent, and the grandparent
        Files.setLastModifiedTime(path, timestamp);
        if (UserPreferences.getInstance().isMoveEnabled()) {
            Files.setLastModifiedTime(path.getParent(), timestamp);
            Files.setLastModifiedTime(path.getParent().getParent(), timestamp);
        }
    }
}
