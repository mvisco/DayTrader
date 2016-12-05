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

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.AccountMgmt.StockPosition;
import com.TraderLight.DayTrader.GeneticAlgos.GeneticSimulation;
import com.TraderLight.DayTrader.MarketDataProvider.GetFromStorageJDBC;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.Strategy.ManualStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategyNeq2;
import com.TraderLight.DayTrader.Strategy.Strategy;
import com.TraderLight.DayTrader.Strategy.TrendStrategy;

public class MainSimulationAlgo {
	
	public static final Logger log = Logging.getLogger(true);
	public static Level1Quote prevQuote;
	public static double totalTradeCost=0;
	private static String broker=""; // The only brokers supported right now is TM
	public static int maxNumberOfPositions;
	public static double capital_allocated;
	public static int spreadTrading = 0;  // Default value is that we do not trade spread
	public static Map<String,List<StockPosition>> allTrades=new HashMap<String,List<StockPosition>>();
	public static int capital_available;
	public static Map<Date, Double> stats_trade = new HashMap<Date,Double>();
	
	

	public static void main(String[] args) {
		
		
		List<Integer> initialDayIndex = new ArrayList<Integer>();
		List<Integer> finalDayIndex = new ArrayList<Integer>();
		int morningOpenIndex;
		int closeOpeningIndex;
		int length = args.length;
		
		if (length <= 0) {
			//we need broker in simulation to assess trading costs
            log.info(" Broker is missing in command line. Brokers supported is TM");
            log.info("Quitting.....");
            System.exit(0);
        }
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
		String initialDate = "2014-07-1";
		String finalDate = "2014-07-3";
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
		String initialTimeMorning = "07:29:55";
		String finalTimeMorning = "07:30:00";
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
			newQuote = getRecord.getLevel1Quote(con, newQuote, "AAPL", query3, query4);
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
        SystemConfig.populateSystemConfig("config.properties");
        SystemConfig sysconfig = SystemConfig.getSystemConfig();
        
        capital_available=sysconfig.capital;
        maxNumberOfPositions=sysconfig.maxNumberOfPositions;
        
        Stock.populateStock("stock-2014-2.xml");
        List<Stock> listOfStocks = Stock.getListOfStocks();
				
		AccountMgr account= new AccountMgr(sysconfig.maxNumberOfPositions, sysconfig.mock, true);
		
        // populate broker and inform AccountMgr
        broker = args[0];
        //broker = "OH";
        account.updateBroker(broker);
        log.info("Broker is " + broker);
		
		NewVolumeAverages v;
		v = NewVolumeAverages.readVolumeAverages();
		
		String symbols = "";
		List<Integer> defaultVolume = new ArrayList<Integer>();
		
		// set up default volume vector in case we are trading a symbol for which we do not have 
		// the information. Just use 0 for all minutes
		for (int i = 0; i < 389; i++) {
			defaultVolume.add(0);
		}
		
		for (Stock stock : listOfStocks ) {
			
			// set  average volume for each stock				
			if (v.symbolAverageVolume.get(stock.symbol) != null) {		
			    stock.setVolumeVector(v.symbolAverageVolume.get(stock.symbol));
			} else {
				stock.setVolumeVector(defaultVolume);
			}
			// Instantiate strategy
			
			if (stock.getStrategyID() == 0) {
				Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
					stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), stock.getVolumeVector());
				stock.setStrategy(strategy);				
				
			} else if ( stock.getStrategyID() == 1) {
				Strategy strategy = new MeanReversionStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), stock.getVolumeVector());
				stock.setStrategy(strategy);
					
		   } else if ( stock.getStrategyID() == 2) {
			   Strategy strategy = new TrendStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   } else if ( stock.getStrategyID() == 3) {
				Strategy strategy = new MeanReversionStrategyNeq2(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), stock.getVolumeVector());
				stock.setStrategy(strategy);
		   
		   } else {
			   log.info("Stategy ID not supported, assigning manual as default");
			   Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   }
			symbols += stock.getSymbol()+",";
		}
		
		//capital_available = 4*sysconfig.capital;
		//overwrite lot size based on capital available and max positions
		double single_stock_capital = (double)4*capital_available/(double) sysconfig.maxNumberOfPositions;
		log.info("single stock capital is " + single_stock_capital);;
		
		 for (Stock stock : listOfStocks ) {

             String symb = stock.getSymbol();
             if (stock.getTradeable()) {

                     // Get a quote to have a market price of the stock
                     Level1Quote quote = new Level1Quote();
                     int i = initialDayIndex.get(0) + 10000;
                     quote = getRecord.getLevel1Quote(i,con,quote,symb,100);
                     double price_bid = quote.getBid();
                     log.info("price is " + price_bid);
                     int lot_size = (int) (single_stock_capital/(2.0*price_bid));
                     log.info("symb is " + symb + " lot size is " + lot_size);
                     // update lot_size in the strategy
                     stock.updateLot(lot_size);
             }
         }
		
		
		Map<String,Stock> mapOfStocks = new HashMap<String, Stock>(Stock.listToMap(Stock.getListOfStocks()));
		
		for (int j =0; j < initialDayIndex.size(); j++ )  {
			  
	        morningOpenIndex = initialDayIndex.get(j);
			closeOpeningIndex = finalDayIndex.get(j);
			 String tradeDay= "Day"+(j+1);
			 Level1Quote lastQuote = new Level1Quote();
			 List<Level1Quote> l = new ArrayList<Level1Quote>();
			 
			 if (j!=0) { 
				  				  
				  l=getRecord.getLevel1QuoteList(con, "ALL", morningOpenIndex, closeOpeningIndex);
				  
				  int i =0;			  
				  
				  //Iterate on all quotes
				  for (Level1Quote newQuote : l) {
				      i++;
					  if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
							continue;
					  }						
					  if (i >= (l.size() - 50)) {
					      // Close all positions we are in the last 50 quotes
						  lastQuote = newQuote;
						  if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
						      continue;
						   }
						   if (mapOfStocks.containsKey(newQuote.getSymbol()) && (mapOfStocks.get(newQuote.getSymbol()).getTradeable()==true)) {
								mapOfStocks.get(newQuote.getSymbol()).getStrategy().closePositions(newQuote);
								
						   }
						   continue;
						}
						
						// send quote to strategy
						if (mapOfStocks.containsKey(newQuote.getSymbol())) {
							mapOfStocks.get(newQuote.getSymbol()).getStrategy().stateTransition(newQuote);
										
						}
						
				  }
			  }

/*
			    for (int i = morningOpenIndex;i < closeOpeningIndex  ;i++ ) {
										
					// Use jdbc driver
					//Level1Quote newQuote = new Level1Quote();
					newQuote = getRecord.getLevel1Quote(i, con, newQuote);	
					
					if (i >= (closeOpeningIndex - 1500)) {
						// Close all positions we are in the last 20 quotes
						if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
							continue;
						}
						if (mapOfStocks.containsKey(newQuote.getSymbol()) && (mapOfStocks.get(newQuote.getSymbol()).getTradeable()==true)) {
							mapOfStocks.get(newQuote.getSymbol()).getStrategy().closePositions(newQuote);
							
						}
						continue;
					}
					if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
						continue;
					}
										
				    // send quote to strategy
					if (mapOfStocks.containsKey(newQuote.getSymbol())) {
						mapOfStocks.get(newQuote.getSymbol()).getStrategy().stateTransition(newQuote);
						
					}				    				
			    }
			  }  
*/  
			 if (j!=0) {	 
			    account.analyzeTrades(true);
				allTrades.put(tradeDay, account.getStockPosition());
				createTradingStats(lastQuote.getCurrentDateTime(),account.getStockPosition());
			 }	
				
				totalTradeCost+=account.getTradeCost();
				account.resetAcctMgr();
				
				//moveStrategiesToInitialState();
				
				log.info(" Done with the day " + lastQuote.getCurrentDateTime());
				
				
				for (Stock stock : listOfStocks) {
					stock.strategy.setStateToS0();
					stock.strategy.clearMean();
				}
				
				/*
				
				// recalculate change profit and loss for next day
				tempDate = newQuote.getCurrentDateTime().toString().split(" ");
				// temp[0] should contain the day
				String query1 = tempDate[0]+ " " + initialTimeAfternoon;
				String query2 = tempDate[0]+ " " + finalTimeAfternoon;
				for (Stock s : listOfStocks) {
					
					//newQuote = getRecord.getLevel1Quote(con, newQuote, "GOOGL", date, date1);
					newQuote = getRecord.getLevel1Quote(con, newQuote, s.getSymbol(), query1, query2);
					double high = newQuote.getHigh();
					double low = newQuote.getLow();
					double change = (high-low)/2.0;
					// update strategy with the new values
					s.strategy.updateChangeProfitLoss(change, change/2.0, change);					
				}
				*/
				
				// apply genetic algos for next day
				if (j != initialDayIndex.size() - 1) {
				    for (Stock s : listOfStocks) {
					    if (s.getTradeable()==true) {
					        GeneticSimulation gSimulation = new GeneticSimulation(s.getSymbol(), account, mapOfStocks, morningOpenIndex, 
				    		     closeOpeningIndex, getRecord, 
				    		     listOfStocks, con, s.getChange());
				            gSimulation.execute();
					    }
				    }
				}
				for (Stock stock : listOfStocks) {
					stock.strategy.setStateToS0();
					stock.strategy.clearMean();
					stock.strategy.updateDisplay(true);
				}

				if (j == initialDayIndex.size() - 1) {
					// this is the end so analyze all trades
					analyzeAllTrades();
				}
		        l.clear();
		 }
		 getRecord.closeConnection();
	}
	
	
	public static void analyzeAllTrades() {
		
		StockPosition trade;
		int numberOfTrades=0;
		int numberOfWinningTrades =0;
		int numberofLoosingTrades=0;
		double currentGain=0D;
		double currentLoss=0D;
		double totalGain;
		
		log.info("                   ");
		
		
		if (!allTrades.isEmpty()) {
			
			//Iterator<Entry<String, LinkedList<TradeObject>>> it = allTrades.entrySet().iterator();
			
			Iterator<Entry<String, List<StockPosition>>> it = allTrades.entrySet().iterator();
			
			 while (it.hasNext()) {
				 
			        Map.Entry<String, List<StockPosition>> pairs = (Entry<String, List<StockPosition>>)it.next();			        
			        Iterator<StockPosition> iterator = pairs.getValue().iterator();	
			        
			        while (iterator.hasNext()){
						trade = (StockPosition)iterator.next();
						numberOfTrades=numberOfTrades+1;
						if (trade.getGain()>0) {
							numberOfWinningTrades = numberOfWinningTrades + 1;
							currentGain=currentGain+trade.getGain();
						} else if (trade.getGain()<0) {
							numberofLoosingTrades = numberofLoosingTrades +1;
							currentLoss=currentLoss+trade.getGain();
						}
						
					}
			        
			 }			
			
		}
		

		totalGain = currentGain+currentLoss;
		log.info("Total for the simulation run *****************************START ");
		log.info("Total for the simulation run *****************************START ");
		log.info("Total for the simulation run *****************************START ");
		log.info("Number of Trades is: " + numberOfTrades);
		log.info("Number of Winning Trades is: " + numberOfWinningTrades+ " Percentage is: " + ((double)numberOfWinningTrades/numberOfTrades));
		log.info("Number of Loosing Trades is " + numberofLoosingTrades+ " Percentage is: " + ((double)numberofLoosingTrades/numberOfTrades) );
		log.info("Gain from the trades is " + currentGain);
		log.info("Loss from the trades is " + currentLoss);		
		log.info("Total Gain is: " + totalGain);
		log.info("Trade Cost is : " + totalTradeCost);
		log.info("Gain minus trade cost is: " + (totalGain - totalTradeCost) );
		log.info("Return on capital " + (totalGain-totalTradeCost)/(double)capital_available);
		log.info("Capital allocated " + (capital_available) + " Max number of positions " + maxNumberOfPositions );
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info(" " );
		log.info(" " );
		analyze_trading_stats();
				
	}
	
	public static String getDB(String initialDate) {
		String db = "";
		int month;
		
		String[] idateArray = initialDate.split("-");
		
		if (idateArray[0].contentEquals("2016")) {			
			month = Integer.parseInt(idateArray[1]);
			if (month <= 4 ) {
				db = "traderlight2016-1";
			} else {
				db = "traderlight2016-2";
			}
			
		} else if (idateArray[0].contentEquals("2015")) {
			month = Integer.parseInt(idateArray[1]);
			if (month <= 6 ) {
				db = "traderlight2015";
			} else {
				db = "traderlight2015-2";
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
	
    public static void createTradingStats(Date date, List<StockPosition> a) {

        double gain = 0.0;

        for (StockPosition s : a) {
        	// not considering trading cost right now
                gain = gain + s.getGain();
        }

        stats_trade.put(date, gain);

}

public static void analyze_trading_stats() {

       // double max_loss = Double.MAX_VALUE;
      //  double max_gain = Double.MIN_VALUE;
        double sharpe_ratio=0;
      //  Date loss_date = null;
      //  Date gain_date = null;
        DescriptiveStatistics m = new DescriptiveStatistics();
        DescriptiveStatistics min_max = new DescriptiveStatistics();


        for ( Entry<Date,Double> a : stats_trade.entrySet()) {

                log.info("Date is " + a.getKey() + " Gain for the day is " + a.getValue());
                
               
                double allocated_capital = capital_available;
                m.addValue(a.getValue()/allocated_capital);
                min_max.addValue(a.getValue());
                

        }
        sharpe_ratio = m.getMean()/m.getStandardDeviation();

        log.info("Mean of the returns " + m.getMean());
        log.info("Std Dev of the return " + m.getStandardDeviation());
        log.info(" Maximum Day Loss " + min_max.getMin());
        log.info("Maximum Day Gain " + min_max.getMax());
        log.info("Sharpe ratio is " + sharpe_ratio);


   }
    
}

    

