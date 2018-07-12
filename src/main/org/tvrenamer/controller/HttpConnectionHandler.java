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
     * Return the proper type of InputStream given the HttpURLConnection and its encoding.
     *
     * @param conn
     *    an HttpURLConnection that we're going to read from
     * @return an InputStream suitable for the HttpURLConnection
     */
    private InputStream getInputStream(final HttpURLConnection conn) throws IOException {
        final InputStream in = conn.getInputStream();
        final String encoding = conn.getContentEncoding();
        if (encoding != null) {
            if (encoding.equalsIgnoreCase("gzip")) {
                return new GZIPInputStream(in);
            }
            if (encoding.equalsIgnoreCase("deflate")) {
                return new InflaterInputStream(in, new Inflater(true));
            }
        }
        return in;
    }

    /**
     * Download the URL and return as a String. Gzip handling from http://goo.gl/J88WG
     *
     * @param url the URL to download
     * @return String of the URL contents
     * @throws TVRenamerIOException when there is an error connecting or reading the URL
     */
    private String downloadUrl(URL url) throws TVRenamerIOException {
        String downloaded = Constants.EMPTY_STRING;
        if (url != null) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                HttpURLConnection.setFollowRedirects(true);
                // allow both GZip and Deflate (ZLib) encodings
                conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                // create the appropriate stream wrapper based on the encoding type
                try (InputStream inputStream = getInputStream(conn)) {
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
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Url stream:\n{0}", downloaded);
                        }
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
            }
        }

        return downloaded;
    }
}
