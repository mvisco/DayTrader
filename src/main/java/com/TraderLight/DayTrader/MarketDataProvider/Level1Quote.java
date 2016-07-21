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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;

/**
 *  This class creates the object that includes all the information that we receive for a Level One Quote from the market.
 * 
 * @author Mario Visco
 *
 */

public class Level1Quote {
	
	public static final Logger log = Logging.getLogger(true);
		
    String symbol = null;     
    Date currentDateTime= null;    
    Date lastDateTime = null;        
    double last = 0;    
    double bid = 0;    
    double ask = 0;   
    double change = 0;   
    char tick = 0;    
    int volume = 0;    
    double high = 0;   
    double low = 0;   
    int bidSize = 0;    
    int askSize = 0;   
    int lastVolume = 0;    
    int avgTrade = 0;    
    int numTrades = 0;    
    double open = 0;  
    double low52week = 0;   
    double high52week = 0;
    int id = 0;
    String isin = "";
    
    public Level1Quote() { } ;

    public Level1Quote (String quote) {
    	String day = null;
        String time = null; 
    	// log.info("Entering : " + function);
    	
    	String[] quoteElements = quote.split(",");
    	int count = 0;
    	for (String quoteElement : quoteElements) {
    		// Parse the string and store the elements. There are 19 elements in the string count gives us the position 
    		// log.info("quoteElement is " + quoteElement);
    		if (count == 0 ) {
    			this.symbol = quoteElement;
    		} else if (count == 1){
    			day = quoteElement;  			
    		} else if (count ==2) {
    			time = quoteElement;
    		} else if (count ==3) {
    			this.last = Double.parseDouble(quoteElement);
    		} else if (count ==4) {
    			this.bid = Double.parseDouble(quoteElement);
    		} else if (count ==5) {
    			this.ask = Double.parseDouble(quoteElement);
    		} else if (count ==6) {
    			this.change = Double.parseDouble(quoteElement);
    		} else if (count ==7) {
    			this.tick = quoteElement.charAt(0);
    		} else if (count ==8) {
    			this.volume = Integer.parseInt(quoteElement);
    		} else if (count ==9) {
    			this.high = Double.parseDouble(quoteElement);
    		} else if (count == 10) {
    			this.low = Double.parseDouble(quoteElement);
    		} else if (count ==11) {
    			this.bidSize = Integer.parseInt(quoteElement);
    		} else if (count ==12) {
    			this.askSize = Integer.parseInt(quoteElement);
    		} else if (count ==13) {
    			this.lastVolume = Integer.parseInt(quoteElement);
    		} else if (count ==14) {
    			this.avgTrade = Integer.parseInt(quoteElement);
    		} else if (count ==15) {
    			this.numTrades = Integer.parseInt(quoteElement);
    		} else if (count ==16) {
    			this.open = Double.parseDouble(quoteElement);
    		} else if (count ==17) {
    			this.low52week = Double.parseDouble(quoteElement);
    		} else if (count ==18) {
    			this.high52week = Double.parseDouble(quoteElement);
    		}
    		count++;
    	}
    	// Convert the dates from String to SQL dates
    	
    	Date sqlDayTime = null;
    	DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
    	String dayTime = day + " " + time;
    	// The last trade time we get it into Eastern time zone so convert  to locale
    	TimeZone est = TimeZone.getTimeZone("EST");
    	sdf.setTimeZone(est);    	  	
    	try {
    		// sqlDatTime should be in locale time zone now in Date type
			sqlDayTime = sdf.parse(dayTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
    	this.lastDateTime = sqlDayTime;
    	//log.info("lastDateTime is: " + lastDateTime);
    	
    	// Get the current date now from Calendar and convert it to Date
    	Calendar rightNow = Calendar.getInstance();
    	Date rightNowTime = rightNow.getTime();  	
    	this.currentDateTime = rightNowTime;
    	//log.info("currentDateTime is: " + currentDateTime);
    }
      
    public String getSymbol() {return symbol;}
    public double getLast() {return last;}
    public double getBid() {return bid;}
    public double getAsk() {return ask;}
    public double getChange() {return change;}
    public char getTick() { return tick;}
    public int  getVolume() {return volume;}    
    public double getHigh() {return high;}
    public double getLow() {return low;}
    public int  getBidSize() {return bidSize;}
    public int  getAskSize() {return askSize;}   
    public int  getLastVolume() {return lastVolume;}
    public int getAvgTrade() {return avgTrade;}
    public int  getNumTrades() {return numTrades;}
    public double getOpen() {return open;}
    public double getLow52week() {return low52week;}
    public double getHigh52week() {return high52week;}
    public Date getLastDateTime() {return lastDateTime;}
    public Date getCurrentDateTime() {return currentDateTime;}

}
