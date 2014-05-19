package com.google.code.tvrenamer.controller;

import java.io.File;
import java.io.IOException;

import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
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

	private void watchDirectory(File target) throws IOException {
		WatchService watcher = null;

		try {
			Path targetPath = Paths.get(target.getAbsolutePath());
			// create a watch service for that path
			watcher = FileSystems.getDefault().newWatchService();
			targetPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

			for (;;) { // loop forever
				WatchKey key;
				try {
					key = watcher.take();
				} catch (InterruptedException e) {
					return;
				}

				List<String> files = new LinkedList<>();

				for (WatchEvent<?> event : key.pollEvents()) {
					Kind<?> kind = event.kind();
					if (kind == OVERFLOW) {
						continue;
					}

					WatchEvent<Path> ev = cast(event);
					Path name = ev.context();
					Path child = targetPath.resolve(name);
					String absolutePath = child.toFile().getAbsolutePath();

					if (kind == ENTRY_CREATE) {
						logger.info("Recieved create event on " + child);
						files.add(absolutePath);
					} else if (kind == ENTRY_MODIFY) {
						logger.info("Recieved modify event on " + child);
						files.add(absolutePath);
					}

				}
				// send files off to be renamed
				this.addFiles(files);

				boolean valid = key.reset();
				if (!valid) {
					// key is no longer valid, we're done here
					logger.severe("watch key became invalid");
					return;
				}

			}

		} finally {
			if (watcher != null) {
				watcher.close();
			}
		}
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConsoleStarterParameters params = new ConsoleStarterParameters();

		// parse the command line args
		JCommander commander = null;
		try {
			commander = new JCommander(params, args);
		} catch (ParameterException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		commander.setProgramName("TVRenamer");

		ConsoleStarter console = new ConsoleStarter();

		if (params.help) {
			commander.usage();
			System.exit(0);
		}

		if (params.watchDir != null) {
			intiateWatcher(console, new File(params.watchDir));
		} else {
			logger.info("calling initiate renamed with " + params.targets);
			TVRenamerInitiator.initiateRenamer(params.targets, console);
		}
	}

	private static void intiateWatcher(ConsoleStarter console, File directory) {
		if (!directory.exists()) {
			logger.severe("Given path to watch " + directory.getAbsolutePath() + " does not exist!");
			System.exit(1);
		} else if (!directory.isDirectory()) {
			logger.severe("Given path to watch " + directory.getAbsolutePath() + " is not a directory!");
			System.exit(2);
		} else {

			try {
				console.watchDirectory(directory);
			} catch (IOException e) {
				logger.log(Level.SEVERE,
						   "Unable to watch the given path " + directory.getAbsolutePath() + ": " + e.getMessage(), e);
			}
		}
	}

	private static class ConsoleStarterParameters {
		@Parameter
		public List<String> targets = Lists.newArrayList();

		@Parameter(names = "--watch", description = "Run TVRenamer as a daemon, watching the given directory for files")
		public String watchDir = null;

		@Parameter(names = { "--help", "--usage" }, help = true)
		private boolean help;
	}

}
