package com.TraderLight.DayTrader.StockTrader;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.AccountMgmt.StockPosition;
import com.TraderLight.DayTrader.MarketDataProvider.GetFromStorageJDBC;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.Strategy.ManualStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategyNeq2;
import com.TraderLight.DayTrader.Strategy.Strategy;
import com.TraderLight.DayTrader.Strategy.TrendStrategy;

public class MainSimulation {
	
	public static final Logger log = Logging.getLogger(true);
	public static Level1Quote prevQuote;
	public static double totalTradeCost=0;
	private static String broker=""; // The only brokers supported right now is TM
	public static int maxNumberOfPositions;
	public static double capital_allocated;
	public static int spreadTrading = 0;  // Default value is that we do not trade spread
	public static Map<String,List<StockPosition>> allTrades=new HashMap<String,List<StockPosition>>();
	
	

	public static void main(String[] args) {
		
		
		List<Integer> initialDayIndex = new ArrayList<Integer>();
		List<Integer> finalDayIndex = new ArrayList<Integer>();
		int morningOpenIndex;
		int closeOpeningIndex;
		int length = args.length;
		
		
		initialDayIndex.add(71885709);    // Mon Nov 03 00:00:00 MST 2014
		initialDayIndex.add(72526956);    // Wed Nov 05 00:00:00 MST 2014
		initialDayIndex.add(73164096);    // Thu Nov 06 00:00:00 MST 2014
		initialDayIndex.add(73801605);    // Fri Nov 07 00:00:00 MST 2014
		initialDayIndex.add(74432114);    // Mon Nov 10 00:00:00 MST 2014
		initialDayIndex.add(75072318);    // Tue Nov 11 00:00:00 MST 2014
		initialDayIndex.add(75698675);    // Wed Nov 12 00:00:00 MST 2014
		initialDayIndex.add(76329501);    // Thu Nov 13 00:00:00 MST 2014
		initialDayIndex.add(76967953);    // Fri Nov 14 00:00:00 MST 2014
		initialDayIndex.add(77594228);    // Mon Nov 17 00:00:00 MST 2014
		initialDayIndex.add(78231819);    // Tue Nov 18 00:00:00 MST 2014
		initialDayIndex.add(78868508);    // Wed Nov 19 00:00:00 MST 2014
		initialDayIndex.add(79517251);    // Thu Nov 20 00:00:00 MST 2014
		initialDayIndex.add(80160090);    // Fri Nov 21 00:00:00 MST 2014
		initialDayIndex.add(80808177);    // Mon Nov 24 00:00:00 MST 2014
		initialDayIndex.add(81445112);    // Tue Nov 25 00:00:00 MST 2014
		initialDayIndex.add(82084507);    // Wed Nov 26 00:00:00 MST 2014
		
		/*		
		initialDayIndex.add(82724353);    // Tue Dec 02 00:00:00 MST 2014
		initialDayIndex.add(83359033);    // Wed Dec 03 00:00:00 MST 2014
		initialDayIndex.add(83995968);    // Thu Dec 04 00:00:00 MST 2014
		initialDayIndex.add(84634051);    // Fri Dec 05 00:00:00 MST 2014
		initialDayIndex.add(85268034);    // Mon Dec 08 00:00:00 MST 2014
		initialDayIndex.add(85907017);    // Tue Dec 09 00:00:00 MST 2014
		initialDayIndex.add(86547316);    // Wed Dec 10 00:00:00 MST 2014
		initialDayIndex.add(87179823);    // Thu Dec 11 00:00:00 MST 2014
		initialDayIndex.add(87812248);    // Fri Dec 12 00:00:00 MST 2014
		initialDayIndex.add(88452673);    // Mon Dec 15 00:00:00 MST 2014
		initialDayIndex.add(89088098);    // Tue Dec 16 00:00:00 MST 2014
		initialDayIndex.add(89714197);    // Wed Dec 17 00:00:00 MST 2014
		initialDayIndex.add(90356708);    // Thu Dec 18 00:00:00 MST 2014
		initialDayIndex.add(90994996);    // Fri Dec 19 00:00:00 MST 2014
		*/
		
		finalDayIndex.add(72526956);    // Mon Nov 03 00:00:00 MST 2014
		finalDayIndex.add(73164096);    // Wed Nov 05 00:00:00 MST 2014
		finalDayIndex.add(73801605);    // Thu Nov 06 00:00:00 MST 2014
		finalDayIndex.add(74432114);    // Fri Nov 07 00:00:00 MST 2014
		finalDayIndex.add(75072318);    // Mon Nov 10 00:00:00 MST 2014
		finalDayIndex.add(75698675);    // Tue Nov 11 00:00:00 MST 2014
		finalDayIndex.add(76329501);    // Wed Nov 12 00:00:00 MST 2014
		finalDayIndex.add(76967953);    // Thu Nov 13 00:00:00 MST 2014
		finalDayIndex.add(77594228);    // Fri Nov 14 00:00:00 MST 2014
		finalDayIndex.add(78231819);    // Mon Nov 17 00:00:00 MST 2014
		finalDayIndex.add(78868508);    // Tue Nov 18 00:00:00 MST 2014
		finalDayIndex.add(79517251);    // Wed Nov 19 00:00:00 MST 2014
		finalDayIndex.add(80160090);    // Thu Nov 20 00:00:00 MST 2014
		finalDayIndex.add(80808177);    // Fri Nov 21 00:00:00 MST 2014
		finalDayIndex.add(81445112);    // Mon Nov 24 00:00:00 MST 2014
		finalDayIndex.add(82084507);    // Tue Nov 25 00:00:00 MST 2014
		finalDayIndex.add(82724353);    // Wed Nov 26 00:00:00 MST 2014
		
		/*
		finalDayIndex.add(83359033);    // Tue Dec 02 00:00:00 MST 2014
		finalDayIndex.add(83995968);    // Wed Dec 03 00:00:00 MST 2014
		finalDayIndex.add(84634051);    // Thu Dec 04 00:00:00 MST 2014
		finalDayIndex.add(85268034);    // Fri Dec 05 00:00:00 MST 2014
		finalDayIndex.add(85907017);    // Mon Dec 08 00:00:00 MST 2014
		finalDayIndex.add(86547316);    // Tue Dec 09 00:00:00 MST 2014
		finalDayIndex.add(87179823);    // Wed Dec 10 00:00:00 MST 2014
		finalDayIndex.add(87812248);    // Thu Dec 11 00:00:00 MST 2014
		finalDayIndex.add(88452673);    // Fri Dec 12 00:00:00 MST 2014
		finalDayIndex.add(89088098);    // Mon Dec 15 00:00:00 MST 2014
		finalDayIndex.add(89714197);    // Tue Dec 16 00:00:00 MST 2014
		finalDayIndex.add(90356708);    // Wed Dec 17 00:00:00 MST 2014
		finalDayIndex.add(90994996);    // Thu Dec 18 00:00:00 MST 2014
		finalDayIndex.add(91621927);    // Fri Dec 19 00:00:00 MST 2014
		*/
		
		
        if (length <= 0) {
            log.info(" Broker is missing in command line. Brokers supported is TM");
            log.info("Quitting.....");
            System.exit(0);
        }
        
        SystemConfig.populateSystemConfig("config.properties");
        SystemConfig sysconfig = SystemConfig.getSystemConfig();
        
        Stock.populateStock("stock.xml");
        List<Stock> listOfStocks = Stock.getListOfStocks();
				
		AccountMgr account= new AccountMgr(sysconfig.maxNumberOfPositions, sysconfig.mock);
		
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
			// Right now we are using the same strategy for all symbols, it may be better to have the choice of strategy for each symbol
			
			if (stock.getStrategyID() == 0) {
				Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
					stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
				stock.setStrategy(strategy);				
				
			} else if ( stock.getStrategyID() == 1) {
				Strategy strategy = new MeanReversionStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
				stock.setStrategy(strategy);
					
		   } else if ( stock.getStrategyID() == 2) {
			   Strategy strategy = new TrendStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   } else if ( stock.getStrategyID() == 3) {
				Strategy strategy = new MeanReversionStrategyNeq2(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
				stock.setStrategy(strategy);
		   
		   } else {
			   log.info("Stategy ID not supported, assigning manual as default");
			   Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   }
			symbols += stock.getSymbol()+",";
		}
		
        Date getDate1= null;
		GetFromStorageJDBC getRecord = new GetFromStorageJDBC();		
		// get connection
		Connection con = getRecord.establishConnection(); 
		if (con == null) {
			log.error("Cannot create a connection to the DB");
			return;
		}
		int n;
		 for (int j =0; j < initialDayIndex.size(); j++ )  { 
			  
			    morningOpenIndex = initialDayIndex.get(j);
			    closeOpeningIndex = finalDayIndex.get(j);
			    String tradeDay= "Day"+(j+1);
			    n=1;
			    		    
			    for (int i = morningOpenIndex;i < closeOpeningIndex  ;i++ ) {
					
					
					// Use jdbc driver
					Level1Quote newQuote = new Level1Quote();
					newQuote = getRecord.getLevel1Quote(i, con, newQuote);	
					
					if (i >= (closeOpeningIndex - 1500)) {
						// Close all positions we are in the last 20 quotes
						for (Stock stock : listOfStocks) {
							String symbol = newQuote.getSymbol();
							if (stock.getSymbol().contentEquals(symbol)) {
								stock.strategy.closePositions(newQuote);
								getDate1 =  newQuote.getCurrentDateTime();
								break;
							}
						}
						continue;
					}
					if (newQuote != null) {
						 // send quote to strategy							
						for (Stock stock : listOfStocks) {
							if (stock.getSymbol().contentEquals(newQuote.getSymbol())) {
								stock.getStrategy().stateTransition(newQuote);
								break;
							}
						}
						
				    } else {
						log.error("Quotes returned from Market Data Provider is a null String Array");
					}					
			    }	
			    account.analyzeTrades();
				allTrades.put(tradeDay, account.getStockPosition());
				
				
				totalTradeCost+=account.getTradeCost();
				account.resetAcctMgr();
				
				//moveStrategiesToInitialState();
				
				log.info(" Done with the day");
				
				for (Stock stock : listOfStocks) {
					stock.strategy.setStateToS0();
					stock.strategy.clearMean();
				}

				if (j == initialDayIndex.size() - 1) {
					// this is the end so analyze all trades
					analyzeAllTrades();
				}
				n++;
				
				
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
		log.info("Return on capital " + (totalGain-totalTradeCost)/(capital_allocated/4.0));
		log.info("Capital allocated " + (capital_allocated/4.0) + " Max number of positions " + maxNumberOfPositions );
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info("Total for the simulation run *****************************END ");
		log.info(" " );
		log.info(" " );
		
				
	}

}

    

