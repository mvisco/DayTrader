/*
 * Copyright 2016 Mario Visco, TraderLight LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 **/

package com.TraderLight.DayTrader.StockTrader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Logging! Will always write to the console, but can specify to also write to a
 * file.
 * 
 * @author Mario Visco
 * 
 */
public class Logging {
	private final static String DEFAULT_OUTPUT_DIR = "logs/";
	private final static String CONVERSION_PATTERN = "(%d{HH:mm:ss,SSS}) %C{1} : %-5p - %m%n";
	private static boolean rootSet = false;
	private static String callingClass;
	private final static String TOP_PACKAGE_NAME = "com.traderlight.StockTrader";

	/**
	 * Get the class that is calling the logging method. This will only be
	 * gotten once. The first class that calls the attempts to create the logger
	 * should set up logging to a file.
	 */
	static {
		callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
	}

	/**
	 * Change the logging level from the default value of INFO. This will affect
	 * all the loggers that get initialized after changing the level.
	 * 
	 * Should be run before initializing the logger whose level needs
	 * changed.
	 * 
	 * @param level
	 */
	public static void setLogLevel(Level level) {
		Logger.getLogger(TOP_PACKAGE_NAME).setLevel(level);
	}


	/**
	 * Logging with the option to write to a file.
	 * 
	 * -Uses default filename and output path for empty strings
	 * 
	 * @param writeFile
	 *            - Write to file?
	 * @return The log4j Logger to use
	 */
	public static Logger getLogger(boolean writeFile) {
		if (!rootSet) {
			if (writeFile) {
				String defaultFileName = getFileNameFromClass(callingClass);
				String relativeDir = getDirFromClass(callingClass)
						+ getDirFromDate();
				setRootLogger(defaultFileName, DEFAULT_OUTPUT_DIR + relativeDir);
			} else {
				setRootLogger();
			}
			rootSet = true;
		}
		String log_level="DEBUG";
    	Level lev = Level.toLevel(log_level);
    	setLogLevel(lev);
		return Logger.getLogger(callingClass);
	}

	/**
	 * Set root logger information for writing to a file -Still writes to
	 * console
	 * 
	 * @param fName
	 * @param fDir
	 */
	private static void setRootLogger(String fName, String fDir) {
		setRootLogger();

		try {
			// Check if directory exists
			File dir = new File(fDir);
			if (!(dir.exists())) {
				dir.mkdirs();
			}
			Logger.getRootLogger().addAppender(
					new FileAppender(new PatternLayout(CONVERSION_PATTERN),
							fDir + fName, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set root logger information for writing solely to console
	 */
	private static void setRootLogger() {
		PatternLayout pattern = new PatternLayout();
		pattern.setConversionPattern(CONVERSION_PATTERN);

		Logger root = Logger.getRootLogger();
		root.removeAllAppenders();
		root.addAppender(new ConsoleAppender(pattern));
		root.setLevel(Level.INFO);

	}

	/**
	 * 
	 * @param callingClass
	 * @return
	 */
	private static String getFileNameFromClass(String callingClass) {
		String currTime = (new SimpleDateFormat("HHmmss")).format(new Date());

		int start = callingClass.lastIndexOf('.') + 1;
		String output = (start > 0) ? callingClass.substring(start)
				: callingClass;		
		// ClassName_Time.log
		String fileName = output + "_";
		fileName += currTime + ".log";
		return fileName;
	}

	/**
	 * Get the name of the directory we should put our output in from the
	 * calling class package. If the last part of the package is 'test' get the
	 * second to last part of the package.
	 * 
	 * 
	 * 
	 * @param callingClass
	 * @return
	 */
	private static String getDirFromClass(String callingClass) {
		int end = callingClass.lastIndexOf('.');

		if (end > 0) {
			String packName = callingClass.substring(0, end);
			end = packName.lastIndexOf('.');
			String topName = packName.substring(end + 1);
			if (topName.equalsIgnoreCase("test")) {
				int start = packName.substring(0, end).lastIndexOf('.') + 1;
				return packName.substring(start) + "/";
			} else {
				return packName.substring(end + 1) + "/";
			}
		} else {
			return callingClass + "/";
		}
	}

	/**
	 * Get the name of the directory based on the current date
	 * 
	 * @return
	 */
	private static String getDirFromDate() {
		return (new SimpleDateFormat("yyyy_MM_dd").format(new Date())) + "/";
	}
}


