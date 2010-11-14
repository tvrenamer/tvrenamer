package com.google.code.tvrenamer.view;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;

public class ProgressBarUpdater implements Runnable {
	private static Logger logger = Logger.getLogger(ProgressBarUpdater.class.getName());

	private final Display display;
	private final ProgressBar progressBar;
	private final int totalNumFiles;
	private final Queue<Future<Boolean>> futures;

	private final UpdateCompleteHandler updateCompleteHandler;

	public ProgressBarUpdater(Display display, ProgressBar progressBar, int total, Queue<Future<Boolean>> futures,
		UpdateCompleteHandler updateComplete) {
		this.display = display;
		this.progressBar = progressBar;
		this.totalNumFiles = total;
		this.futures = futures;
		this.updateCompleteHandler = updateComplete;
	}

	public void run() {
		while (true) {
			if (display.isDisposed()) {
				return;
			}

			final int size = futures.size();
			display.asyncExec(new Runnable() {
				public void run() {
					if (progressBar.isDisposed()) {
						return;
					}
					progressBar.setSelection((int) Math
						.round(((double) (totalNumFiles - size) / totalNumFiles * progressBar.getMaximum())));
				}
			});

			if (size == 0) {
				this.updateCompleteHandler.onUpdateComplete();
				return;
			}

			try {
				Future<Boolean> future = futures.remove();
				logger.info("future returned: " + future.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

}
