package com.google.code.tvrenamer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.ShowInformationListener;
import com.google.code.tvrenamer.controller.TVRageProvider;

public class ShowStore {

	private static Logger logger = Logger.getLogger(ShowStore.class.getName());

	private static final Map<String, Show> _shows = new HashMap<String, Show>();
	private static final Map<String, ShowRegistrations> _showRegistrations = new HashMap<String, ShowRegistrations>();

	private static final Semaphore _showStoreLock = new Semaphore(1);

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();

	public static Show getShow(String showName) {
		Show s = null;
		try {
			_showStoreLock.acquire();
			s = _shows.get(showName);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE,
					   "Error: interrupted while getting show information '" + showName + "': " + e.getMessage(), e);
		} finally {
			_showStoreLock.release();
		}

		if (s == null) {
			String message = "Show not found for show name: '" + showName + "'";
			logger.warning(message);
			throw new ShowNotFoundException(message);
		}

		return s;

	}

	public static void getShow(String showName, ShowInformationListener listener) {

		/*
		 * Basic outline for this method:
		 * - if we have already downloaded the show (exists in _shows) then just call the method on the listener
		 * - if we don't have the show, but are in the process of downloading the show (exists in _showRegistrations)
		 * then add the listener to the registration
		 * - if we don't have the show and aren't downloading, then create the registration, add the listener and kick
		 * off the download
		 */

		try {
			_showStoreLock.acquire();
			Show show = _shows.get(showName);
			if (show != null) {
				listener.downloaded(show);
			} else {
				ShowRegistrations registrations = _showRegistrations.get(showName);
				if (registrations != null) {
					registrations.addListener(listener);
				} else {
					registrations = new ShowRegistrations();
					registrations.addListener(listener);
					_showRegistrations.put(showName, registrations);
					downloadShow(showName);
				}
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE,
					   "Error: interrupted while getting show information '" + showName + "': " + e.getMessage(), e);
		} finally {
			_showStoreLock.release();
		}
	}

	private static void downloadShow(final String showName) {
		Callable<Boolean> showFetcher = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				ArrayList<Show> options = TVRageProvider.getShowOptions(showName);
				Show thisShow = options.get(0);

				TVRageProvider.getShowListing(thisShow);

				_showStoreLock.acquire();
				logger.info("Show listing for '" + thisShow.getName() + "' downloaded");
				_shows.put(showName, thisShow);
				notifyListeners(showName, thisShow);
				_showStoreLock.release();
				return true;
			}
		};
		threadPool.submit(showFetcher);
	}

	private static void notifyListeners(String showName, Show show) {
		ShowRegistrations registrations = _showRegistrations.get(showName);

		if (registrations != null) {
			for (ShowInformationListener informationListener : registrations.getListeners()) {
				informationListener.downloaded(show);
			}
		}
	}

	private static class ShowRegistrations {
		private final List<ShowInformationListener> _listeners;

		public ShowRegistrations() {
			this._listeners = new LinkedList<ShowInformationListener>();
		}

		public void addListener(ShowInformationListener listener) {
			this._listeners.add(listener);
		}

		public List<ShowInformationListener> getListeners() {
			return Collections.unmodifiableList(_listeners);
		}
	}

	public static void cleanUp() {
		threadPool.shutdownNow();
	}
}
