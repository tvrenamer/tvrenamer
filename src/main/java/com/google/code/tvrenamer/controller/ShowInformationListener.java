package com.google.code.tvrenamer.controller;

import com.google.code.tvrenamer.model.Show;

public interface ShowInformationListener {
	void downloaded(Show show);
	void downloadFailed(Show show);
}
