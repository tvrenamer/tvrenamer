package com.google.code.tvrenamer.view;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.gjt.sp.util.ProgressObserver;

public class FileCopyMonitor implements ProgressObserver {
	private long maximum;
	private final ProgressBar progressBar;
	private final long progressBarMaximum;
	private int loopCount = 0;
	private final NumberFormat format = DecimalFormat.getPercentInstance();

	public FileCopyMonitor(ProgressBar progressBar, long maximum, int progressBarMaximum) {
		this.progressBar = progressBar;
		this.progressBarMaximum = progressBarMaximum * 1L;
		this.setMaximum(maximum);
		this.setValue(0);
		format.setMaximumFractionDigits(1);
	}

	@Override
	public void setValue(final long value) {
		if ((loopCount++ % 1000) == 0) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (progressBar.isDisposed()) {
						return;
					}
					double partial = (value * FileCopyMonitor.this.progressBarMaximum);
					progressBar.setSelection((int) (partial / FileCopyMonitor.this.maximum));
					progressBar.setToolTipText(format.format(partial / (100 * (double) FileCopyMonitor.this.maximum)));
				}
			});
		}
	}

	@Override
	public void setMaximum(long value) {
		this.maximum = value;
	}

	@Override
	public void setStatus(final String status) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (progressBar.isDisposed()) {
					return;
				}
				progressBar.setToolTipText(status);
			}
		});
	}
}
