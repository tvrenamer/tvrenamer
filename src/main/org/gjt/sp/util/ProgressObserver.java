/*
 * ProgressObserver.java - Progression monitor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.util;

/**
 * Interface used to monitor things that can progress.
 *
 * @author Matthieu Casanova
 * @version $Id: ProgressObserver.java 12504 2008-04-22 23:12:43Z ezust $
 * @since jEdit 4.3pre3
 */
public interface ProgressObserver
{
	/**
	 * Update the progress value.
	 *
	 * @param value the new value
	 * @since jEdit 4.3pre3
	 */
	void setValue(long value);
	 
	/**
	 * Update the maximum value.
	 *
	 * @param value the new maximum value
	 * @since jEdit 4.3pre3
	 */
	void setMaximum(long value);
	 
	/**
	 * Update the status label.
	 *
	 * @param status the new status label
	 * @since jEdit 4.3pre3
	 */
	void setStatus(String status);
}
