package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class TVRenamerInitiator {
	public static void initiateRenamer(final String[] fileNames, FilesAddedListener callback) {
		final List<String> files = new LinkedList<String>();
		for (final String fileName : fileNames) {
			File f = new File(fileName);
			FileTraversal traversal = new FileTraversal() {
				@Override
				public void onFile(File f) {
					// Don't add hidden files - defect 38
					if (!f.isHidden()) {
						files.add(f.getAbsolutePath());
					}
				}
			};
			traversal.traverse(f);
		}
		callback.addFiles(files);
	}

	// class adopted from http://vafer.org/blog/20071112204524
	public static abstract class FileTraversal {
		public final void traverse(final File f) {
			if (f.isDirectory()) {
				onDirectory(f);
				final File[] children = f.listFiles();
				for (File child : children) {
					traverse(child);
				}
				return;
			}
			onFile(f);
		}

		public void onDirectory(final File d) {

		}

		public void onFile(final File f) {

		}
	}
}
