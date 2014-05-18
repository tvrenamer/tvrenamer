package com.google.code.tvrenamer.controller;

import org.gjt.sp.util.ProgressObserver;

public class FileMoveProgressListener {
	public void moveStarted() {
		
	}

	public ProgressObserver moveProgress(long length) {
		return new ProgressObserver() {
			@Override
			public void setValue(long arg0) {
				// do nothing
			}
			
			@Override
			public void setStatus(String arg0) {
				// do nothing
			}
			
			@Override
			public void setMaximum(long arg0) {
				// do nothing
			}
		};
	}

	public void moveSuccess() {
		
	}

	public void moveFail() {
		
	}
}
