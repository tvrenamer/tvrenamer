package org.tvrenamer.controller;

import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.util.Constants;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

class HttpConnectionHandler {

    private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;

    /**
     * Download the URL and return as a String
     *
     * @param urlString the URL as a String
     * @return String of the contents
     * @throws TVRenamerIOException when there is an error connecting or reading the URL
     */
    public String downloadUrl(String urlString) throws TVRenamerIOException {
        try {
            URL url = new URL(urlString);
            logger.fine("Downloading URL " + urlString);
            return downloadUrl(url);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, urlString + " is not a valid URL ", e);
            return "";
        }
    }

    /**
     * Download the URL and return as a String. Gzip handling from http://goo.gl/J88WG
     *
     * @param url the URL to download
     * @return String of the URL contents
     * @throws TVRenamerIOException when there is an error connecting or reading the URL
     */
    private String downloadUrl(URL url) throws TVRenamerIOException {
        InputStream inputStream = null;
        String downloaded = Constants.EMPTY_STRING;

        try {
            if (url != null) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                HttpURLConnection.setFollowRedirects(true);
                // allow both GZip and Deflate (ZLib) encodings
                conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                // create the appropriate stream wrapper based on the encoding type
                String encoding = conn.getContentEncoding();
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    inputStream = new GZIPInputStream(conn.getInputStream());
                } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                    inputStream = new InflaterInputStream(conn.getInputStream(), new Inflater(true));
                } else {
                    inputStream = conn.getInputStream();
                }

                logger.finer("Before reading url stream");

                // always specify encoding while reading streams
                try (BufferedReader reader
                     = new BufferedReader(new InputStreamReader(inputStream, Constants.TVR_CHARSET)))
                {
                    StringBuilder contents = new StringBuilder();
                    String s;
                    while ((s = reader.readLine()) != null) {
                        contents.append(s);
                    }
                    downloaded = contents.toString();
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "error reading from stream: ", ioe);
                    throw(ioe);
                }

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Url stream:\n{0}", downloaded);
                }
            }
        } catch (FileNotFoundException e) {
            String message = "FileNotFoundException when attempting to download"
                + " and parse URL " + url;
            // We don't necessarily consider FileNotFoundException to be "severe".
            // That's why it's handled first, as a special case.  We create and
            // throw the TVRenamerIOException in the same way as any other exception;
            // we just don't log it at the same level.
            logger.fine(message);
            throw new TVRenamerIOException(message, e);
        } catch (IOException e) {
            String message = "I/O Exception when attempting to download and parse URL " + url;
            logger.log(Level.SEVERE, message, e);
            throw new TVRenamerIOException(message, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception when attempting to close input stream", e);
            }
        }

        return downloaded;
    }
}
