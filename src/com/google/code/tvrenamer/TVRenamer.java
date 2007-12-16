package com.google.code.tvrenamer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class TVRenamer {
  private String showName = "";
  private String season = "";
  private List<File> files;
  private Map<Integer, Episode> episodeListing;
  private static Logger logger = Logger.getLogger(TVRenamer.class);

  private static final boolean READ_FROM_FILE = false;

  public TVRenamer(String[] args) {
    files = new ArrayList<File>(30);
    episodeListing = new HashMap<Integer, Episode>();

    handleArgs(args);
    downloadListing();
  }

  private void handleArgs(String[] args) {
    if (args.length == 0) {
      logger.fatal("Usage: tvRenamer [-s] [-p] {filename(s)/*} \n"
          + "to rename all/specified files according to parent directory\n");
      System.exit(1);
    }
    for (int i = 0; i < args.length; i++) {
      files.add(new File(args[i].toString()));
    }
    File parentFile = files.get(0).getParentFile();
    showName = parentFile.getName();
    // String regex = ".*(\\d).*\\d\\d.*$"; // previous one

    // if we keep the episode matching stuff, it will be non-greedy ^_^
    String regex = "[^\\d]*?(\\d\\d?).*[^\\d]*?(\\d\\d).*";
    Matcher matcher = Pattern.compile(regex).matcher(files.get(0).getName());
    logger.debug("season input -> " + files.get(0).getName());
    if (matcher.matches()) {
      season = matcher.group(1);
      // remove leading zero
      if (season.charAt(0) == '0')
        season = season.substring(1);
      logger.debug("season -> " + season);
    }
  }

  private void downloadListing() {
    String showURL = "";
    InputStream ioStream = null;
    BufferedReader reader = null;
    try {
      showURL = "http://www.tvrage.com/" + showName.replace(" ", "_")
          + "/episode_list/" + season;
      URL url = new URL(showURL);

      if (READ_FROM_FILE) {
        String filename = "C:/Documents and Settings/Dave/Eclipse Workspace/TVRenamer/ep_list.html";
        logger.debug("Reading episode details from file " + filename);
        FileReader inputReader = new FileReader(filename);
        reader = new BufferedReader(inputReader);
      } else {
        logger.debug("Reading episode details from " + showURL);
        ioStream = url.openStream();
        reader = new BufferedReader(new InputStreamReader(ioStream));
      }

      String line = reader.readLine();

      while (line != null) {
        String webRegex = ".*<td style=.*(\\d{2})\'>(.*?)<.*";
        Pattern webPattern = Pattern.compile(webRegex);
        Matcher webMatcher = webPattern.matcher(line);
        if (webMatcher.matches()) {
          String webEpNum = webMatcher.group(1);

          for (File file : files) {
            String fileName = file.getName();
            // String regex = ".*(\\d\\d).*$"; // previous one
            String regex = "[^\\d]*?\\d\\d?[^\\d]*?(\\d\\d).*";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
              String epNum = matcher.group(1);
              if (epNum.equals(webEpNum)) {
                Episode ep = new Episode(file.getAbsolutePath(), webEpNum,
                    webMatcher.group(2), season, showName);
                logger.debug("Found episode " + fileName
                    + "; episode number is " + ep.getNumber()
                    + " with title \'" + ep.getTitle() + "\'");
                episodeListing.put(Integer.parseInt(webEpNum), ep);

                logger.debug("The html line we found was \n " + line);
              }
            }
          }
        }
        line = reader.readLine();
      }

    } catch (MalformedURLException e) {
      logger.warn("Unable to read malformed URL, URL is: " + showURL);
    } catch (IOException e) {
      logger.warn("Caught IO Exception while reading " + showURL);
    } catch (NumberFormatException e) {
      logger.warn("Caught Number Format Exception while reading " + showURL);
    } finally {
      // close the stream
      try {
        if (!READ_FROM_FILE) {
          ioStream.close();
        }
        reader.close();
      } catch (IOException e) {
        logger.warn("Caught IO Exception while closing " + showURL + " stream");
      }
    }
  }

  public Map<Integer, Episode> getEpisodeListing() {
    return episodeListing;
  }
}
