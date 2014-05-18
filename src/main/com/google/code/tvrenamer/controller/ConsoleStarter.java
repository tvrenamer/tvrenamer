package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.code.tvrenamer.model.EpisodeStatus;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.ShowStore;
import com.google.code.tvrenamer.model.UserPreferences;

public class ConsoleStarter implements FilesAddedListener {
	private static Logger logger = Logger.getLogger(ConsoleStarter.class.getName());

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private Map<String, FileEpisode> files;

	private UserPreferences prefs;

	public ConsoleStarter() {
		files = new HashMap<String, FileEpisode>();
		prefs = UserPreferences.getInstance();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConsoleStarter starter = new ConsoleStarter();
		TVRenamerInitiator.initiateRenamer(args, starter);
	}

	@Override
	public void addFiles(List<String> fileNames) {
		for (String fileName : fileNames) {
			final FileEpisode episode = TVRenamer.parseFilename(fileName);
			if (episode == null) {
				logger.severe("Couldn't parse file: " + fileName);
			} else {
				String showName = episode.getShowName();
				files.put(fileName, episode);

				ShowStore.getShow(showName, new ShowInformationListener() {
					@Override
					public void downloaded(Show show) {
						episode.setStatus(EpisodeStatus.DOWNLOADED);
						renameFile(episode);
					}

					@Override
					public void downloadFailed(Show show) {
						episode.setStatus(EpisodeStatus.BROKEN);
					}
				});
			}
		}
	}

	public void renameFile(FileEpisode fileEpisode) {
		File currentFile = fileEpisode.getFile();
		File newFile = null;
		if (prefs.isMovedEnabled()) {
			newFile = new File(fileEpisode.getNewFilePath());
		} else {
			newFile = new File(currentFile.getParent() + File.separator + fileEpisode.getNewFilePath());
		}
		if (newFile.exists()) {
			logger.severe("Target file already exists " + newFile.getAbsolutePath());
		} else {
			logger.info("Going to move " + currentFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
			FileMoveProgressListener progressListener = new FileMoveProgressListener();
			Callable<Boolean> moveCallable = new FileMover(fileEpisode, newFile, progressListener);
			executor.submit(moveCallable);
		}
	}
}
