package com.TraderLight.DayTrader.StockTrader;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.OptionPosition;
import com.TraderLight.DayTrader.AccountMgmt.StockPosition;
import com.TraderLight.DayTrader.MarketDataProvider.GetFromStorageJDBC;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;



import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.ui.RefineryUtilities;

public class MeanAnalysis {
	
	public static final Logger log = Logging.getLogger(true);
	public static Level1Quote prevQuote;
	public static double totalTradeCost=0;
	private static String broker=""; // The only brokers supported right now is TM
	public static int maxNumberOfPositions;
	public static double capital_allocated;
	public static int spreadTrading = 0;  // Default value is that we do not trade spread
	public static Map<String,List<StockPosition>> allTrades=new HashMap<String,List<StockPosition>>();
	public static Map<String, Map<Integer, List<OptionPosition>>> optionAllTrades = new HashMap<String, Map<Integer, List<OptionPosition>>>();
	public static int capital_available;
	public static Map<Date, Double> stats_trade = new HashMap<Date,Double>();
	
	

	public static void main(String[] args) {
		
		
		List<Integer> initialDayIndex = new ArrayList<Integer>();
		List<Integer> finalDayIndex = new ArrayList<Integer>();
		int morningOpenIndex;
		int closeOpeningIndex;
		int length = args.length;
		

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
		String initialDate = "2017-06-19";
		String finalDate = "2017-06-19";
		Date iDate=null;
		try {
			iDate = sdf.parse(initialDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Date fDate=null;
		try {
			fDate = sdf.parse(finalDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// determine the db to connect to. Assume right now that both initial date and final date belong to the same DB eventually this needs to be changed.
		String db= getDB(initialDate);
		log.info("db is " + db);
		
		// get list of dates  to run simulation on 
		List<Date> dates = getDaysBetweenDates(iDate, fDate);
		String[] tempDate;	
		
		// set the time to add to query DB on
		String initialTimeMorning = "07:30:00";
		String finalTimeMorning = "07:30:10";
		String initialTimeAfternoon = "13:59:54";
		String finalTimeAfternoon = "13:59:59";
		
		//clone the dates list to be able to do remove....
		List<Date> tempDates = new ArrayList<Date>(dates);
		
		GetFromStorageJDBC getRecord = new GetFromStorageJDBC();		
		// get connection
		Connection con = getRecord.establishConnection(db); 
		if (con == null) {
			System.out.println("connection error" + " " +db);
			return;
		}
		
		//get indexes
		for (Date date : tempDates) {
			// date.toString() looks like this "Fri Aug 05 00:00:00 MDT 2016"
			tempDate = date.toString().split(" ");
			// remove holidays.... It will need to be changed to also remove holidays that fall in the week ex. 4th of July etc...
			if (tempDate[0].contentEquals("Sat") || tempDate[0].contentEquals("Sun")) {
				dates.remove(date);
				continue;
			}
			
			// create the DB query arguments
			String query1 = tempDate[5]+"-"+monthToNumber(tempDate[1])+"-"+tempDate[2]+" "+ initialTimeMorning;
			String query2 = tempDate[5]+"-"+monthToNumber(tempDate[1])+"-"+tempDate[2]+" "+ finalTimeMorning;
			String query3 = tempDate[5]+"-"+monthToNumber(tempDate[1])+"-"+tempDate[2]+" "+ initialTimeAfternoon;
			String query4 = tempDate[5]+"-"+monthToNumber(tempDate[1])+"-"+tempDate[2]+" "+ finalTimeAfternoon;

					
			Level1Quote newQuote = new Level1Quote();
			//newQuote = getRecord.getLevel1Quote(con, newQuote, "GOOGL", date, date1);
			newQuote = getRecord.getLevel1Quote(con, newQuote, "AAPL", query1, query2);
			
			if (newQuote.getId() == 0) {
				// no data for the day
				log.info("No initial data for the day " + date);
				continue;
			}
			initialDayIndex.add(newQuote.getId());
			System.out.println(newQuote.getId());
			newQuote = getRecord.getLevel1Quote(con, newQuote, "QQQ", query3, query4);
			if (newQuote.getId() == 0) {
				// no final data for the day, I guess remove the data from initial index as well and continue to next day
				log.info("No final data for the day " + date);
				initialDayIndex.remove(initialDayIndex.size()-1);
				continue;
			}
			finalDayIndex.add(newQuote.getId());
			
			
		}
		
		
        if (initialDayIndex.size() == 0) {
        	log.info("There are no days to run the simulation on............................");
        	System.exit(0);
        }
        
        
       // final GraphSeries demo = new GraphSeries("DisplayStatistics");
      //  demo.pack();
       // RefineryUtilities.centerFrameOnScreen(demo);
       // demo.setVisible(true);
        
        int day_count = 0;
        int zero_count = 0;
        // The following arrays store all the mins and maxs for the entire simulation run so we can display them at the end.
        List<Double> min_array = new ArrayList<Double>();
		List<Double> max_array = new ArrayList<Double>();
		
		String symbol = "FB";
		for (int j =0; j < initialDayIndex.size(); j++ )  {
			
			List<Double> prices = new ArrayList<Double>();
			List<Double> mu = new ArrayList<Double>();
			List<Double> diffs = new ArrayList<Double>();
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			DescriptiveStatistics diff_stats = new DescriptiveStatistics();
			Level1Quote prevQuote=null;
			double mean;  
			day_count += 1;
		    morningOpenIndex = initialDayIndex.get(j);
		    closeOpeningIndex = finalDayIndex.get(j);
		    String tradeDay= "Day"+(j+1);
		    List<Level1Quote> l = new ArrayList<Level1Quote>();

		    l=getRecord.getLevel1QuoteList(con, symbol, morningOpenIndex, closeOpeningIndex);
			  
		    int i =0;			  

		    //Iterate on all quotes
		    for (Level1Quote newQuote : l) {
		    	i++;
		    	if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
		    		continue;
		    	}
		    	
		    	prices.add(newQuote.getLast());
		    	mean = calculateMean(newQuote, prevQuote, stats);
		    	prevQuote = newQuote;
		    	diff_stats.addValue(newQuote.getLast() - mean);
		    	//demo.updateDatasets(newQuote.getLast() -mean, 0, 0, i);
		    	//demo.updateDatasets(newQuote.getLast(), mean, newQuote.getVolume(), i);
		    	//demo.updateDatasets(newQuote.getChange(), 0, 0);
		    	/*
		    	try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                */
		    }
		    log.info("Symbol is " + symbol);
		    log.info("Descriptive stats of the difference vector ");
		    log.info(" mean is " + diff_stats.getMean());
		    log.info("std dev is " + diff_stats.getStandardDeviation());
		    log.info(" Min and Max are " + diff_stats.getMin() + " " + diff_stats.getMax());
		    // The following arrays 
		    min_array.add(diff_stats.getMin());
		    max_array.add(diff_stats.getMax());
		    
		    int min_index=0;
		    int max_index=0;
		    List<Integer> zero_index = new ArrayList<Integer>();
		    
		    // The following loops finds the index for the min and max of the diff_stats vector
		    for ( int k = 0; k <= diff_stats.getValues().length -1; k++ ) {
		    	
		    	if ( diff_stats.getElement(k) == diff_stats.getMin()) {
		    		min_index = k;
		    	}
		    	if ( diff_stats.getElement(k) == diff_stats.getMax()) {
		    		max_index = k;
		    	}
		    }
		    
		    // The following  is the  algorithm to find the index of the root of the diff_stats contained between the min and max 
		    // whichever happens first. The idea is that it will give the time on when the diff_stats is 0 after either a min or a max 
		    // has been reached.
		    for ( int k = 0; k <= diff_stats.getValues().length -1; k++ ) {
		    	if ( (diff_stats.getElement(k) >= -0.005 ) && (diff_stats.getElement(k) <= +0.005 ) ) {
		    		if ( min_index > max_index) {
		    			// if the min happens after the max then the this is the root that we are looking for if it is higher  
		    			// than max_index and lower than min_index
		    			if ((k < min_index) && (k > max_index)) {
		    				zero_index.add(k);
		    				break;
		    				
		    			}
		    		} else {
		    			// if the min happens before the max then the this is the root that we are looking for if it is higher  
		    			// than min_index and lower than max_index
		    			if ((k > min_index) && (k < max_index)) {
		    			    zero_index.add(k);
		    			    break;
		    			}
		    		}
		    		
		    	}
		    }
		    
		    if ( (min_index == 0) && (max_index == 0) )
		    	continue;
		    
            log.info( " Min and Max Indexes are at " + min_index + " " + max_index);
            log.info(" Quote dates are " + l.get(min_index).getCurrentDateTime() + " " + l.get(max_index).getCurrentDateTime());
            log.info("Min and Max prices are " + l.get(min_index).getLast() + " " + l.get(max_index).getLast());
            log.info("Difference between Min and Max is : " + (l.get(max_index).getLast()-l.get(min_index).getLast()));
            log.info("Log of the variation between Min and Max  is " + (Math.log(l.get(max_index).getLast()/l.get(min_index).getLast())));
            
            
            
            for (Integer zero_pos : zero_index) {
            	log.info( "zero positin is " + zero_pos);
            	log.info(l.get(zero_pos).getCurrentDateTime());
            	log.info(l.get(zero_pos).getLast());
            	log.info(" " );
            	log.info( " ------------------------");
            	log.info(" Next Day .......");
            	zero_count +=1;
            }
            
            /*
            final DisplayStatisticsDoublePlot demo = new DisplayStatisticsDoublePlot("DisplayStatistics");
            demo.pack();
            RefineryUtilities.centerFrameOnScreen(demo);
            demo.setVisible(true);
            demo.updateDatasets(diff_stats.getValues(), 0, 0);
            */
			if (j == initialDayIndex.size() - 1) {
				// this is the end so analyze all trades
				log.info(" Done with the analyis");
				log.info("Probability of zero counts " + (double)zero_count/(double)day_count);
				log.info("Min Array");
				for (Double a : min_array) {
					log.info(a);
				}
				log.info("Max Array");
				for (Double a : max_array) {
					log.info(a);
				}
			} 
	
	 }
	 getRecord.closeConnection();
        
        
        
	}
	
	public static double  calculateMean(Level1Quote newQuote, Level1Quote prevQuote, DescriptiveStatistics stats) {
		
		// we do not want to add to sample space quotes that do not have difference in volumes. The reason is that 
		// there have been no trades between these two quotes so from a mean calculation stand point we do not want to include them.
		
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
	
	
	public static String monthToNumber(String month) {
		String monthNumber="";
		
		if (month.contentEquals("Jan")) {
			monthNumber = "01";
		} else if ((month.contentEquals("Feb"))){
			monthNumber = "02";
		} else if ((month.contentEquals("Mar"))){
			monthNumber = "03";
		} else if ((month.contentEquals("Apr"))){
			monthNumber = "04";
		} else if ((month.contentEquals("May"))){
			monthNumber = "05";
		} else if ((month.contentEquals("Jun"))){
			monthNumber = "06";
		} else if ((month.contentEquals("Jul"))){
			monthNumber = "07";
		} else if ((month.contentEquals("Aug"))){
			monthNumber = "08";
		} else if ((month.contentEquals("Sep"))){
			monthNumber = "09";
		} else if ((month.contentEquals("Oct"))){
			monthNumber = "10";
		} else if ((month.contentEquals("Nov"))){
			monthNumber = "11";
		}  else if ((month.contentEquals("Dec"))){
			monthNumber = "12";
		} else {
			return month;
		}
				
		return monthNumber;
	}
	
	public static String getDB(String initialDate) {
		String db = "";
		int month;
		
		String[] idateArray = initialDate.split("-");
		month = Integer.parseInt(idateArray[1]);
		if (idateArray[0].contentEquals("2016")) {			
			
			if (month <= 4 ) {
				db = "traderlight2016-1";
			} else if (month <= 8 ){
				db = "traderlight2016-2";
			} else {
				db = "traderlight2016-3";
			}
			
		} else if (idateArray[0].contentEquals("2015")) {
			
			if (month <= 6 ) {
				db = "traderlight2015";
			} else {
				db = "traderlight2015-2";
			}
		} else if (idateArray[0].contentEquals("2017")) {
			if (month <= 4 ) {
				db = "traderlight2017-1";
			} else {
				db = "traderlight2017-2";
			}
						
		} else {
			db = "traderlight2014";
		}	
		return db;
	}
	
	
	public static List<Date> getDaysBetweenDates(Date startdate, Date enddate)
	{
	    List<Date> dates = new ArrayList<Date>();
	    Calendar calendar = new GregorianCalendar();
	    calendar.setTime(startdate);
	    
	    Calendar endCalendar = new GregorianCalendar();
	    endCalendar.setTime(enddate);
	    endCalendar.add(Calendar.DATE, 1);
	    enddate=endCalendar.getTime();

	    while (calendar.getTime().before(enddate))
	    {
	        Date result = calendar.getTime();
	        dates.add(result);
	        calendar.add(Calendar.DATE, 1);
	    }
	    return dates;
	}
	
	
	
}
