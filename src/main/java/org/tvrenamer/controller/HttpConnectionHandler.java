package org.tvrenamer.controller;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.tvrenamer.model.TVRenamerIOException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class HttpConnectionHandler {
    private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 60000;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build();

    /**
     * Try to keep {@link #downloadUrl} as clean as possible by not doing error handling
     * there.  On any kind of failure, we'll get here.  We may need to re-discover something
     * that was already known, but it's really not an issue, especially since this only
     * happens in the error case.
     *
     * @param response
     *   the Response object that we got back after trying to download the URL
     * @param url
     *   the URL we tried to download, as a String
     * @param ioe
     *   an I/O exception that may give some indication of what went wrong (and, at least,
     *   gives us a stack trace...)
     * @return
     *   does not actually return anything; always throws an exception
     * @throws TVRenamerIOException in all cases; the fact of this method being called
     *   means something went wrong; creates it from the given arguments
     */
    private String downloadUrlFailed(final Response response, final String url,
                                     final IOException ioe)
        throws TVRenamerIOException
    {
        String msg;
        if (ioe == null) {
            msg = "attempt to download " + url + " failed with response code "
                + response.code();
        } else {
            msg = "exception downloading " + url;
        }
        logger.log(Level.WARNING, msg, ioe);
        throw new TVRenamerIOException(msg, ioe);
    }

    /**
     * Download the URL and return as a String
     *
     * @param urlString the URL as a String
     * @return String of the contents
     * @throws TVRenamerIOException when there is an error connecting or reading the URL
     */
    public String downloadUrl(String urlString) throws TVRenamerIOException {
        logger.fine("Downloading URL " + urlString);

        Request request;
        Response response = null;
        try {
            request = new Request.Builder().url(urlString).build();
            response = CLIENT.newCall(request).execute();
            if (response != null) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        String downloaded = body.string();
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Url stream:\n{0}", downloaded);
                        }
                        return downloaded;
                    }
                } else if (response.code() == 404) {
                    throw new FileNotFoundException(urlString);
                }
            }
            throw new TVRenamerIOException(urlString);
        } catch (IOException ioe) {
            return downloadUrlFailed(response, urlString, ioe);
        }
    }
}
