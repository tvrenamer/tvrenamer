package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * EndToEndRenameTest -- rename a real file using real data from the provider.
 *
 * The other tests each cover one link in the chain: parsing the filename,
 * querying the provider, moving the file.  This one runs the whole chain the way
 * the program does, from an ugly filename on disk to a renamed file on disk, with
 * only the UI left out.  It needs a working internet connection.
 */
public class EndToEndRenameTest {

    private static final int TIMEOUT_SECONDS = 30;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void initializePrefs() {
        FileMover.userPrefs.setCheckForUpdates(false);
        FileMover.userPrefs.setSeasonPrefix("Season ");
        FileMover.userPrefs.setSeasonPrefixLeadingZero(false);
        FileMover.userPrefs.setMoveSelected(true);
        FileMover.userPrefs.setRenameSelected(true);
        FileMover.userPrefs.setRemoveEmptiedDirectories(false);
        FileMover.userPrefs.setRenameReplacementString("%S S%0sE%0e %t");

        FileMover.logger.setLevel(Level.SEVERE);
    }

    /**
     * Look the show up with the provider, blocking until it answers.
     *
     * @param filenameShow
     *   the part of the filename believed to name the show
     * @return the Show the provider matched
     */
    private static Show resolveShow(final String filenameShow) throws Exception {
        final CompletableFuture<Show> futureShow = new CompletableFuture<>();
        ShowStore.mapStringToShow(filenameShow, new ShowInformationListener() {
            @Override
            public void downloadSucceeded(Show show) {
                futureShow.complete(show);
            }

            @Override
            public void downloadFailed(FailedShow failedShow) {
                futureShow.completeExceptionally(
                    new IllegalStateException("provider found no show for " + filenameShow));
            }

            @Override
            public void apiHasBeenDeprecated() {
                futureShow.completeExceptionally(
                    new IllegalStateException("provider API is no longer available"));
            }
        });
        return futureShow.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Download the given series' episode listings, blocking until they arrive.
     *
     * @param series
     *   the series to download listings for
     */
    private static void downloadListings(final Series series) throws Exception {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        series.addListingsListener(new ShowListingsListener() {
            @Override
            public void listingsDownloadComplete() {
                future.complete(true);
            }

            @Override
            public void listingsDownloadFailed(Exception err) {
                future.completeExceptionally(
                    new IllegalStateException("could not download listings for " + series, err));
            }
        });
        future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private Path createInputFile(final String filename) throws IOException {
        final Path inputDir = tempFolder.newFolder("input").toPath();
        final Path inputFile = inputDir.resolve(filename);
        Files.write(inputFile, new byte[] {0});
        return inputFile;
    }

    @Test
    public void testRenameFromUglyFilename() throws Exception {
        final Path outputDir = tempFolder.getRoot().toPath().resolve("output");
        FileMover.userPrefs.setDestinationDirectory(outputDir.toString());

        final Path inputFile
            = createInputFile("quintuplets.s01e02.hdtv.x264-fake.avi");

        // Everything from here down is what the program does when a file is
        // added, minus the UI.
        final FileEpisode episode = new FileEpisode(inputFile);
        assertTrue("could not parse " + inputFile.getFileName(), episode.wasParsed());
        assertEquals("quintuplets", episode.getFilenameShow());

        final Show show = resolveShow(episode.getFilenameShow());
        assertTrue("provider did not return a real series, but " + show,
                   show.isValidSeries());
        assertEquals("Quintuplets", show.getName());
        episode.setEpisodeShow(show);

        downloadListings(show.asSeries());
        episode.listingsComplete();

        assertEquals("Quintuplets S01E02 Quintagious",
                     episode.getDestinationBasename());

        final boolean moved = new FileMover(episode).call();
        if (!moved) {
            fail("FileMover did not move " + inputFile);
        }

        final Path expected = outputDir
            .resolve("Quintuplets")
            .resolve("Season 1")
            .resolve("Quintuplets S01E02 Quintagious.avi");
        assertTrue("expected renamed file at " + expected
                   + " but episode is now at " + episode.getPath(),
                   Files.exists(expected));
    }
}
