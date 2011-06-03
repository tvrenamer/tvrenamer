package com.google.code.tvrenamer.controller;

import java.io.File;
import java.text.NumberFormat;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.FileEpisode;

public class TVRenamer {
	private static Logger logger = Logger.getLogger(TVRenamer.class.getName());

	
	
	public static final String[] REGEX = { 
		"(.+?\\W\\D*?)\\s(\\d\\d?)\\D(\\d\\d?).*?((\\d{4})).*?",
		"(.+?\\d{4}\\W\\D*?)\\s[sS]?(\\d\\d?)\\D*?(\\d\\d).*?((\\d{4})).*?",
		"(.+?\\W\\D*?)\\s[sS]?(\\d\\d?)\\D*?(\\d\\d).*?((\\d{4})).*?",
		"(.+?\\W\\D*?)\\s[sS]?(\\d\\d?).*?((\\d{4})).*?"
		};
	
	public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length];
	
	
	static {
		for (int i = 0; i < REGEX.length; i++) {
			COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
		}
	}

	private TVRenamer() {
		// singleton
	}

	public static FileEpisode parseFilename(String fileName) {
		File f = new File(fileName);
		String fName = insertShowNameIfNeeded(f);

		int idx = 0;
		Matcher matcher = null;
	

		while (idx < COMPILED_REGEX.length) {
			
			matcher = COMPILED_REGEX[idx++].matcher(fName);
			if (matcher.matches()) {
				logger.info("Matched on " + matcher.pattern().pattern());
				for (int i=0; i <matcher.groupCount(); i++) {
					logger.info("Group " + i + " = " + matcher.group(i));
				}
				
				String show = matcher.group(1);
				show = StringUtils.replacePunctuation(show).trim().toLowerCase();
				
				String year = matcher.group(matcher.groupCount());
				int expectedGroupCountWithSeasonAndEpisode = 4;
				if (year.length() == 4) {
					expectedGroupCountWithSeasonAndEpisode++;
				} else {
					year = null;
				}
				
				int season;
				int episode;
				if (matcher.groupCount() == expectedGroupCountWithSeasonAndEpisode) {
					season = Integer.parseInt(matcher.group(2));
					episode = Integer.parseInt(matcher.group(3));
				} else {
					
					episode = Integer.parseInt(matcher.group(2));
					
					String fullName = fileName.toLowerCase();
					int sIdx = fullName.indexOf("season");
					if (sIdx > 0) {
						
						String seasonNumber = fileName.substring(sIdx + 7, sIdx + 9);
						if (seasonNumber.endsWith("\\") || seasonNumber.endsWith("/")) {
							seasonNumber = seasonNumber.substring(0,1);
						}		
						season = Integer.parseInt(seasonNumber.trim());
						logger.info("Found season from file path " + seasonNumber);
					} else {
						// Just assume its season 1 since we cant determine it right now
						logger.info("Season not found. Default to Season 1");
						season = 1;
					}
				}
				

				FileEpisode ep = new FileEpisode(show, year, season, episode, f);
				return ep;
			}
		}

		return null;
	}

	private static String insertShowNameIfNeeded(File file) {
		String fName = file.getName();
		if (fName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
			String parentName = file.getParentFile().getName();
			if (parentName.toLowerCase().startsWith("season")) {
				parentName = file.getParentFile().getParentFile().getName();
			}
			logger.info("appending parent directory '" + parentName + "' to filename '" + fName + "'");
			return parentName + " " + fName;
		} else {
			return fName;
		}
	}
}
