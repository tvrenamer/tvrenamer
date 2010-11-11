package com.google.code.tvrenamer.model;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.ShowInformationListener;
import com.google.code.tvrenamer.controller.TVRageProvider;

public class ShowStore {

	private static Logger logger = Logger.getLogger(ShowStore.class.getName());
	private static Map<String, Show> _shows = new HashMap<String, Show>();
	private static Map<String, ShowRegistrations> _showRegistrations = new HashMap<String, ShowRegistrations>();

	public static void addShow(String showName, ShowInformationListener listener) {
		ShowRegistrations registrations = _showRegistrations.get(showName);

		if (registrations == null) {
			registrations = new ShowRegistrations();
			synchronized (_showRegistrations) {
				_showRegistrations.put(showName, registrations);
			}
		}

		registrations.addListener(listener);
	}

	public static Show fetchShow(String showName) throws ConnectException, UnknownHostException {
		ArrayList<Show> options = TVRageProvider.getShowOptions(showName);
		Show thisShow = options.get(0);

		TVRageProvider.getShowListing(thisShow);

		synchronized (_shows) {
			logger.fine("Put show " + showName + " as " + thisShow.getName());
			_shows.put(showName, thisShow);
			notifyListeners(showName, thisShow);
		}

		return thisShow;
	}

	public static Show getShow(String showName) {
		Show s = _shows.get(showName);

		if (s == null) {
			String message = "Show not found for show name: '" + showName + "'";
			logger.warning(message);
			throw new ShowNotFoundException(message);
		}

		return s;
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
}
