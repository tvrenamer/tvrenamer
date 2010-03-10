---
layout: post
title: TV Renamer Brainstorm
---
# TV Renamer brainstorm: features, ideas and notes
## 0.4.1
Release this version ASAP

## 0.5
 * Using git for source control, and [github](http://github.com) for project hosting: [TVRenamer](http://github.com/tvrenamer/tvrenamer). Downloads will continue to be available at [Google Code](http://code.google.com/p/tv-renamer), and the source will be uploaded every release (0.5, 0.6 etc)
 * The project is now called *TVRenamer* or *TV Renamer*, *tv-renamer* will be setup as a redirect
 * Main features of 0.5:
   1. correctly detecting show information (title, season and episode) from filenames only (i.e not in the show's folder) - [Issue #7](http://code.google.com/p/tv-renamer/issues/detail?id=7)
   1. naming multiple show files at once - this may be deferred to 0.6 - [Issue #8](http://code.google.com/p/tv-renamer/issues/detail?id=8)
   1. moving files to a selected destination folder
 * Change to SAX based parsing from DOM based parsing
 * Defer long running series performance issue to 0.6 - [Issue #15](http://code.google.com/p/tv-renamer/issues/detail?id=15)
 * Get a list of filename inputs we will support
   - Vipul has a list from downloading stuff
   - Add Dave Keane's regex
   - Add regex from [Issue #25](http://code.google.com/p/tv-renamer/issues/detail?id=25)
 * Add tvdb.org as a provider
 * Add runtime caching of XML data - [Issue #21](http://code.google.com/p/tv-renamer/issues/detail?id=21)
 * Stop overwrites of existing files - [Issue #22](http://code.google.com/p/tv-renamer/issues/detail?id=22)

## 0.6
 * proxy support
 * preferences, backed by files
 * non-interactive mode / run as shell script