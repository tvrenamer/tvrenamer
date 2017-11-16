package org.tvrenamer.controller;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;

/**
 * Class to load a URL in the user's browser.
 */
public class UrlLauncher extends SelectionAdapter {

    private final String url;

    /**
     * Construct a UrlLauncher
     *
     * @param url
     *    the text of the URL to launch when the link is clicked
     */
    public UrlLauncher(String url) {
        this.url = url;
    }

    /**
     * The link has been clicked.
     *
     * @param arg0
     *    the event object itself; not used
     */
    @Override
    public void widgetSelected(SelectionEvent arg0) {
        Program.launch(url);
    }
}
