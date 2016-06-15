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

package com.TraderLight.DayTrader.MarketDataProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;;

/**
 *  This class gets the level one quotes from Quote Tracker.
 * 
 * @author Mario Visco
 *
 */

public class MarketDataProvider {

	public static final Logger log = Logging.getLogger(true);
	static String qtURL="";
	static String stockSymbols; 
	
	
 
	private MarketDataProvider() { };
	
	public static void setParameters(String qtURL, String stockSymbols) {
		MarketDataProvider.qtURL = qtURL;
		MarketDataProvider.stockSymbols = stockSymbols;
		
	}
	
	public static String[]  QTMarketDataProvider() {
		    
		try {
		    DefaultHttpClient httpclient = new DefaultHttpClient();
		    String urlToUse = MarketDataProvider.qtURL +"(" +MarketDataProvider.stockSymbols+")";		    
		    HttpGet httpGet = new HttpGet(urlToUse);
		    HttpResponse response1 = httpclient.execute(httpGet);
		    HttpEntity entity = response1.getEntity();
		
		    if (entity != null) {
		        
		        //log.info("Length of the Entity is: " + EntityUtils.toString(entity));
		        String totalQuotes = EntityUtils.toString(entity);
		        // String[] Quotes = totalQuotes.split("\r\n");
		        String[] Quotes = totalQuotes.split(System.getProperty("line.separator"));
		        int count = 0;
		        int newlength = Quotes.length-1;
		        String[] return_quotes = new String[newlength];
		        for (String quote : Quotes) {		            
		            // The first string is the string OK so remove it from the return
		            if (count != 0) {
		        	    return_quotes[count-1]= quote;	        	
		            }
		            count++;
		        }
		        		      
		      EntityUtils.consume(entity);
		      httpclient.getConnectionManager().shutdown();
		      return return_quotes;
	         } 
		 } catch(Exception e) {
			// Log the exception thrown  
			log.error("Exception Thrown: " + e);
		 }
		 return null;
   }		

	
}