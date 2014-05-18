package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.gjt.sp.util.ProgressObserver;

import com.google.code.tvrenamer.controller.util.FileUtilities;
import com.google.code.tvrenamer.model.FileEpisode;

public class FileMover implements Callable<Boolean> {
	private static Logger logger = Logger.getLogger(FileMover.class.getName());

	private final File destFile;

	private final FileEpisode episode;

	private final FileMoveProgressListener callback;

	public FileMover(FileEpisode src, File destFile, FileMoveProgressListener callback) {
		this.episode = src;
		this.destFile = destFile;
		this.callback = callback;
	}

	@Override
	public Boolean call() {
		File srcFile = this.episode.getFile();
		if (destFile.getParentFile().exists() || destFile.getParentFile().mkdirs()) {
			callback.moveStarted();
			boolean succeeded = false;
			if (areSameDisk(srcFile.getAbsolutePath(), destFile.getAbsolutePath())) {
				succeeded = srcFile.renameTo(destFile);
			}
			if (succeeded) {
				callback.moveSuccess();
				logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
				episode.setFile(destFile);
				return true;
			} else {
				ProgressObserver observer = callback.moveProgress(srcFile.length());
				succeeded = FileUtilities.moveFile(srcFile, destFile, observer, true);
				if (succeeded) {
					logger.info("Moved " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
					episode.setFile(destFile);
					callback.moveSuccess();
				} else {
					logger.severe("Unable to move " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
					callback.moveFail();
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
				} else {
					return false;
				}
			}
		}
		return false;
	}
}
