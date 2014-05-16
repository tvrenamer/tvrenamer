package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import com.google.code.tvrenamer.controller.util.FileUtilities;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.FileMoveIcon;
import com.google.code.tvrenamer.view.FileCopyMonitor;
import com.google.code.tvrenamer.view.UIStarter;

public class FileMover implements Callable<Boolean> {
	private static Logger logger = Logger.getLogger(FileMover.class.getName());

	private final File destFile;

	private final TableItem item;

	private final FileEpisode episode;

	private final Label progressLabel;

	private final Display display;

	public FileMover(Display display, FileEpisode src, File destFile, TableItem item, Label progressLabel) {
		this.display = display;
		this.episode = src;
		this.destFile = destFile;
		this.item = item;
		this.progressLabel = progressLabel;
	}

	@Override
	public Boolean call() {
		File srcFile = this.episode.getFile();
		if (destFile.getParentFile().exists() || destFile.getParentFile().mkdirs()) {
			UIStarter.setTableItemStatus(display, item, FileMoveIcon.RENAMING);
			boolean succeeded = false;
			if (areSameDisk(srcFile.getAbsolutePath(), destFile.getAbsolutePath())) {
				succeeded = srcFile.renameTo(destFile);
			}
			if (succeeded) {
				UIStarter.setTableItemStatus(display, item, FileMoveIcon.SUCCESS);
				logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
				episode.setFile(destFile);
				return true;
			} else {
				FileCopyMonitor monitor = new FileCopyMonitor(progressLabel, srcFile.length());
				succeeded = FileUtilities.moveFile(srcFile, destFile, monitor, true);
				if (!display.isDisposed()) {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							if (progressLabel.isDisposed()) {
								return;
							}
							progressLabel.setText("");
						}
					});
				}
				if (succeeded) {
					logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
					episode.setFile(destFile);
					UIStarter.setTableItemStatus(display, item, FileMoveIcon.SUCCESS);
				} else {
					logger.severe("Unable to move " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
					UIStarter.setTableItemStatus(display, item, FileMoveIcon.FAIL);
				}
				return succeeded;
			}
		}
		return false;
	}

	private static boolean areSameDisk(String pathA, String pathB) {
		File[] roots = File.listRoots();
		if (roots.length < 2) {
			return true;
		}
		for (File root : roots) {
			String rootPath = root.getAbsolutePath();
			if (pathA.startsWith(rootPath)) {
				if (pathB.startsWith(rootPath)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}
}
