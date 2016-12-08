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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;

/**
 *  Basic strategy class that is the ancestor of all strategy classes.
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
	boolean display=true;
	double impVol;
	double previousClose = 0;
	
	public Strategy(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, double impVol, List<Integer> v) {
		
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
		this.impVol = impVol;
		
		
	}
	
	public void updateLot(int lotSize) {
		this.lot=lotSize;
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

	public void closePositions(Level1Quote newQuote) {
		return;
		
	}

	public void setStateToS0() {
		// TODO Auto-generated method stub
		
	}
	
	public void updateChangeProfitLoss(double change, double profit, double loss) {
		return;
	}
	
	public void updateDisplay(Boolean display) {
		this.display=display;
	}

	public double getImpVol() {
		return impVol;
	}

	 public void getPreviousClose(Date date) {
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		// if Monday go get the Friday before. Remember that the field 7 is the DAY_OF_THE_WEEK. it could be accessed also as 
		// Calendar.DAY_OF_THE_WEEK
		
		if (cal.get(Calendar.DAY_OF_WEEK) == 2) {
			cal.add(Calendar.DAY_OF_WEEK, -3);
		} else {
			// just get the day before
			cal.add(Calendar.DAY_OF_WEEK, -1);
		}
		
		int day = cal.get(Calendar.DATE);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		
		//String str="http://ichart.finance.yahoo.com/table.csv?s=QQQ&a=10&b=25&c=2016&d=11&e=02&f=2016&g=d";
		String str = "http://ichart.finance.yahoo.com/table.csv?s="+symbol+"&a="+month+"&b="+day+"&c="+year+"&d="+month+"&e="+day+"&f="+year;
		URL url=null;
		try {
			url = new URL(str);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		try {
			System.out.println(inputStreamtoString(url.openStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  //System.out.println("res is " + res);
		  //log.info("Response is " + res);
		*/
		String str_response="";
		try {
			str_response = inputStreamtoString(url.openStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		String delim = " \n,";
		StringTokenizer st2 = new StringTokenizer(str_response, delim);
		double d = 0;
		int n=0;
		while (st2.hasMoreElements()) {
			//System.out.println(st2.nextElement()+ " " + this.symbol);
			String next = (String) st2.nextElement();
			//log.info("Content for the Yahoo Query for symbol "+ symbol);
			//log.info(next);
			if ( n == 11) {
				d = Double.parseDouble((String)st2.nextElement());
				log.info("This is the previous price " + d + " for symbol " + symbol);
			}
			n++;
			//response.add((String)st2.nextElement());
		}
		this.previousClose = d;
		
		
	}
	
	private  String inputStreamtoString(InputStream fi) throws IOException
	{
		ByteArrayOutputStream bout=new ByteArrayOutputStream();

		byte buffer[] = new byte[1000];
		int len;
		while( (len = fi.read(buffer)) != -1 ) {
			bout.write(buffer,0,len);
		}

		bout.close();
		fi.close();
		return bout.toString();
	}
	
	double getPreviousClose() {
		
		return this.previousClose;
	}
	
}
