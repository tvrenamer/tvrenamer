package com.google.code.tvrenamer.view;

import java.text.NumberFormat;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.gjt.sp.util.ProgressObserver;

public class FileCopyMonitor implements ProgressObserver {
	private long maximum;
	private final ProgressBar progressBar;
	private final long progressBarMaximum;
	private int loopCount = 0;
	private final NumberFormat format = NumberFormat.getPercentInstance();

	public FileCopyMonitor(ProgressBar progressBar, long maximum, int progressBarMaximum) {
		this.progressBar = progressBar;
		this.progressBarMaximum = progressBarMaximum * 1L;
		setMaximum(maximum);
		setValue(0);
		format.setMaximumFractionDigits(1);
	}

	public void setValue(final long value) {
		if (loopCount++ % 1000 == 0) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (progressBar.isDisposed()) {
						return;
					}
					double partial = value * progressBarMaximum;
					progressBar.setSelection((int) (partial / maximum));
					progressBar.setToolTipText(format.format(partial / (100 * (double) maximum)));
				}
			});
		}
	}

	public void setMaximum(long value) {
		maximum = value;
	}


	public void setStatus(final String status) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (progressBar.isDisposed()) {
					return;
				}
				progressBar.setToolTipText(status);
			}
		});
	}
}
