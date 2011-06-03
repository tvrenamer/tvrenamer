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
	
	// static initaliser block as there are static methods
	static {
		populateFireflyShow();
	}

	public static Show getShow(String showName) {
		Show s = null;
		try {
			_showStoreLock.acquire();
			s = _shows.get(showName.toLowerCase());
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

	/**<p>Download the show details if required, otherwise notify listener.</p>
	 * <ul>
	 *   <li>if we have already downloaded the show (exists in _shows) then just call the method on the listener</li>
	 *   <li>if we don't have the show, but are in the process of downloading the show (exists in _showRegistrations)
	 *       then add the listener to the registration</li>
	 *   <li>if we don't have the show and aren't downloading, then create the registration, add the listener and kick
	 *       off the download</li>
	 * </ul>
	 * @param showName the name of the show
	 * @param listener the listener to notify or register
	 */
	public static void getShow(String showName, String year, ShowInformationListener listener) {
		try {
			_showStoreLock.acquire();
			Show show = _shows.get(showName.toLowerCase());
			if (show != null) {
				listener.downloaded(show);
			} else {
				ShowRegistrations registrations = _showRegistrations.get(showName.toLowerCase());
				if (registrations != null) {
					registrations.addListener(listener);
				} else {
					registrations = new ShowRegistrations();
					registrations.addListener(listener);
					_showRegistrations.put(showName.toLowerCase(), registrations);
					downloadShow(showName, year);
				}
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE,
					   "Error: interrupted while getting show information '" + showName + "': " + e.getMessage(), e);
		} finally {
			_showStoreLock.release();
		}
	}

	private static void downloadShow(final String showName, final String year) {
		Callable<Boolean> showFetcher = new Callable<Boolean>() {
			public Boolean call() throws Exception {
				ArrayList<Show> options = TVRageProvider.getShowOptions(showName);
				
				Show thisShow = null;
				
				if (options.size() == 1) {
					thisShow = options.get(0);
				} else {
					if (year != null) {
						for (Show show : options) {
							if (year.equals(show.getYear())) {
								thisShow = show;
							}
						}
					}
				}
				
				if (thisShow == null) {
					thisShow = options.get(0);
				}
								
				TVRageProvider.getShowListing(thisShow);
								
				addShow(showName, thisShow);

				return true;
			}
		};
		threadPool.submit(showFetcher);
	}

	private static void notifyListeners(String showName, Show show) {
		ShowRegistrations registrations = _showRegistrations.get(showName.toLowerCase());

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
	
	/**
	 * Add a show to the store, registered by the show name.<br />
	 * Added this distinct method to enable unit testing
	 * @param showName the show name
	 * @param show the {@link Show}
	 * @throws InterruptedException when there is a problem acquiring or releasing the lock
	 */
	static void addShow(String showName, Show show) throws InterruptedException {
		_showStoreLock.acquire();
		logger.info("Show listing for '" + show.getName() + "' downloaded");
		
		_shows.put(showName.toLowerCase(), show);
		notifyListeners(showName, show);
		_showStoreLock.release();
	}

	private static void populateFireflyShow() {
		Show firefly = new Show("3548", "Firefly", "http://www.tvrage.com/Firefly");

		Season season = new Season(1);
		season.addEpisode(1, "Serenity");
		season.addEpisode(2, "The Train Job");
		season.addEpisode(3, "Bushwhacked");
		season.addEpisode(4, "Shindig");
		season.addEpisode(5, "Safe");
		season.addEpisode(6, "Our Mrs Reynolds");
		season.addEpisode(7, "Jaynestown");
		season.addEpisode(8, "Out of Gas");
		season.addEpisode(9, "Ariel");
		season.addEpisode(10, "War Stories");
		season.addEpisode(11, "Trash");
		season.addEpisode(12, "The Message");
		season.addEpisode(13, "Heart of Gold");
		season.addEpisode(14, "Objects in Space");

		firefly.setSeason(1, season);

		try {
			addShow("firefly", firefly);
		} catch (InterruptedException e) {
			// Should never happen
			logger.log(Level.SEVERE, "InterruptedException when attempting to add Firefly to cache", e);
		}
	}
}
