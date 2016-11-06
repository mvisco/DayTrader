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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This class creates the system configuration parameters as a singleton object.
 * 
 * @author Mario Visco
 *
 */

public class SystemConfig {
	
	int maxNumberOfPositions;
	String OHLogin;
	String OHPassword;
	String OHAuthURL;
	String OHSourceApp;
	boolean mock;
	boolean useDB;
	String qtURL;
	private static SystemConfig sysConfig = null;
	public static final Logger log = Logging.getLogger(true);
	
	private SystemConfig() {
		
	}
	
	/**
	 * This method populate the system configuration singleton object.
	 * <p>
	 * It uses a property file configuration file located in the project root
	 *
	 * <p>
	 * @author Mario Visco
	 *
	 * @param Properties File Name
	 */
	public static void  populateSystemConfig(String fileName) {
		
		if (sysConfig == null) {
			// populate sysconfig singleton
			String prop_file=fileName;		
			Properties properties = new Properties();
			sysConfig = new SystemConfig();
			
			try {
				//ClassLoader classLoader = sysConfig.getClass().getClassLoader();				
				//properties.load(classLoader.getResourceAsStream(prop_file));
				properties.load(new FileInputStream(prop_file));
				
				sysConfig.maxNumberOfPositions=Integer.parseInt(properties.getProperty("maxNumberOfPositions"));
				sysConfig.OHLogin = properties.getProperty("OHLogin");
				sysConfig.OHPassword = properties.getProperty("OHPassword");
				sysConfig.OHAuthURL= properties.getProperty("OHAuthURL");
				sysConfig.OHSourceApp = properties.getProperty("OHSourceApp");
				if (properties.getProperty("mock").contains("true")) {
					sysConfig.mock=true;
				} else {
					sysConfig.mock=false;
				}
				sysConfig.qtURL = properties.getProperty("qtURL");
				if (properties.getProperty("useDB").contains("true")) {
					sysConfig.useDB=true;
				} else {
					sysConfig.useDB=false;
				}
				
			} catch (IOException e){
				log.info("Something went wrong in reading the config file " + e);
				log.info("Exiting program........");
				System.exit(1);
				
			}
			
			
		}
		
	}
	
	/**
	 * This method returns the system configuration singleton object. If you call it before calling populateSystemConfig it 
	 * will return a null object.	 
	 * 
	 * @author Mario Visco
	 *
	 * @param Properties File Name
	 */
	public static SystemConfig getSystemConfig() {
		return sysConfig;
	}
	

}
