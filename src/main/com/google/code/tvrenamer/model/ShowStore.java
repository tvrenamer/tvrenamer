package com.google.code.tvrenamer.model;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.TVRageProvider;

public class ShowStore {

	private static Logger logger = Logger.getLogger(ShowStore.class.getName());
	private static Map<String, Show> _shows = new HashMap<String, Show>();

	public static void addShow(String showName) throws ConnectException, UnknownHostException {
		ArrayList<Show> options = TVRageProvider.getShowOptions(showName);
		Show thisShow = options.get(0);

		TVRageProvider.getShowListing(thisShow);
		synchronized (_shows) {
			logger.fine("Put show " + showName + " as " + thisShow.getName());
			_shows.put(showName, thisShow);
		}
	}

	public static Show getShow(String showName) {
		Show s = null;
		synchronized (_shows) {
			s = _shows.get(showName);
		}
		if (s == null) {
			String message = "Show not found for show name: '" + showName + "'";
			logger.warning(message);
			throw new ShowNotFoundException(message);
		}
		return s;
	}
}
