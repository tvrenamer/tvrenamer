package org.tvrenamer.view;

import org.tvrenamer.controller.UpdateCompleteHandler;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ProgressBarUpdater implements Runnable {
    private static Logger logger = Logger.getLogger(ProgressBarUpdater.class.getName());

    private final int totalNumFiles;
    private final Queue<Future<Boolean>> futures;

    private final UpdateCompleteHandler updateCompleteHandler;

    private final ProgressProxy proxy;

    public ProgressBarUpdater(ProgressProxy proxy, int total, Queue<Future<Boolean>> futures,
        UpdateCompleteHandler updateComplete)
    {
        this.proxy = proxy;
        this.totalNumFiles = total;
        this.futures = futures;
        this.updateCompleteHandler = updateComplete;
    }

    @Override
    public void run() {
        while (true) {
            final int size = futures.size();
            proxy.setProgress((float) (totalNumFiles - size) / totalNumFiles);

            if (size == 0) {
                this.updateCompleteHandler.onUpdateComplete();
                return;
            }

            try {
                Future<Boolean> future = futures.remove();
                logger.info("future returned: " + future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
