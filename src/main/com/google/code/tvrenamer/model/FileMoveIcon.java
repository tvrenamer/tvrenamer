package com.google.code.tvrenamer.model;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public enum FileMoveIcon {
	ADDED("res/icons/SweetieLegacy/16-circle-blue.png"), DOWNLOADING("res/icons/SweetieLegacy/16-clock.png"), RENAMING(
		"res/icons/SweetieLegacy/16-em-pencil.png"), SUCCESS("res/icons/SweetieLegacy/16-em-check.png"), FAIL(
		"res/icons/SweetieLegacy/16-em-cross.png");

	public final Image icon;

	private FileMoveIcon(String path) {
		icon = new Image(Display.getCurrent(), path);
	}
}
