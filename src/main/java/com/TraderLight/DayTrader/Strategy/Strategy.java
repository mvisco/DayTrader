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

package com.TraderLight.DayTrader.Strategy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;

/**
 *  Basic strategy class to be extended.
 * 
 * @author Mario Visco
 *
 */
public abstract class  Strategy {
	
	public static final Logger log = Logging.getLogger(true);
	String symbol;
    int lot;
	double objective_change;
	double profit;
	double price;
	double loss;
	boolean isTradeable;
	AccountMgr account;
	List<Integer> averageVolume;	
	DescriptiveStatistics stats;
	List<Level1Quote> rollingWindow;
	int sizeOfTheList;
	Level1Quote lastQuote;
	int minute_mean_position = 0;
	
	public Strategy(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, List<Integer> v) {
		
		this.symbol = symbol;
		this.lot = symbol_lot;
		this.objective_change=change;
		this.profit=profit;
		this.price = 0;
		this.isTradeable = isTradeable;
		this.account = account;
		this.loss = loss;
		this.averageVolume = v;
		this.stats = new DescriptiveStatistics();
		this.lastQuote = new Level1Quote();
		
	}
	
	public double calculateMean(Level1Quote newQuote, Level1Quote prevQuote) {
		
		// we do not want to add to sample space quotes that do not have difference in volumes. The reason is that 
		// there have been no trades between these two quotes so from a mean calculation stand point we do not want to include them.
		this.lastQuote = newQuote;
		if (prevQuote == null) {
			// this should happen only once when the program is started and the strategy receives the first message
			prevQuote = newQuote;
			stats.addValue(newQuote.getLast());
		}
		
		if (newQuote.getVolume() != prevQuote.getVolume()) {
			stats.addValue(newQuote.getLast());
		}
		
		// this should not happen but just in case the sample space is empty add the last one
		if (stats.getN() == 0) {
			stats.addValue(newQuote.getLast());			
		}
		return stats.getMean();		
	}
	
    public void clearMean() {
    	stats.clear();
    }
      
	public int minutePosition(Level1Quote quote) {
		
 	   int minutes;
 	   int hours;
 	   
       Date getDate =  quote.getCurrentDateTime();
       TimeZone mst = TimeZone.getTimeZone("America/Denver");
       SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
       sdf.setTimeZone(mst);
       String[] time = sdf.format(getDate).split(":");       
	   minutes = Integer.parseInt(time[1]);
	   hours =  Integer.parseInt(time[0]);
		
	   for (int i = 0; i <= 59; i++) {
			if (minutes == i) {
				if (hours == 7) {
					return (i-30);
				} else if (hours == 8) {
					return(i+30);
				} else if (hours == 9) {
					return(i+90);
				} else if (hours == 10) {
					return (i+150);
				} else if (hours == 11) {
					return (i+210);
				} else if (hours == 12) {
					return(i+270);
				} else if (hours ==13) {
					return (i+329);
				} else
					return 0;
			}
		}
    	return 0;
    	
    }
    
    public void setTradeableFlag(boolean tradeable) {
    	this.isTradeable=tradeable;
    	return;
    }
	
    public void dumpStats(String symbol) {
    	
    	log.info("---------------------------------------------------------");
    	log.info("---------------------------------------------------------");
    	log.info("Dumping Stats for symbol: " + symbol);
		log.info("The mean is: " + stats.getMean());
		log.info("The standard deviation is: " + stats.getStandardDeviation());
		log.info("Minimum is : " + stats.getMin());
		log.info("Maximum is : " + stats.getMax());
		log.info("Skewness is : " + stats.getSkewness());
		log.info("Kurtosis is : " + stats.getKurtosis());		
		log.info("Population size n is:  : " + stats.getN());
    }

	public void strategyCallback(boolean b, String symbol2, String buy_sell, String price) {
		// To be overridden in children
		return;		
	}

	public void stateTransition(Level1Quote newQuote) {
		// To be overridden in children
		return;		
	}

	 public void updateStrategyParameters(double change, double profit, double loss, int lot, boolean tradeable, boolean closePosition, 
			   boolean openLongPosition, boolean openShortPosition, boolean openLongPositionWithPrice, boolean openShortPositionWithPrice,
			   double longPrice, double shortPrice)  {
		// To be overridden in children
		return;
	 }
}
