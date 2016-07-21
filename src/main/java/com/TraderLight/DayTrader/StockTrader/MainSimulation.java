package com.TraderLight.DayTrader.StockTrader;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.Strategy.ManualStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategy;
import com.TraderLight.DayTrader.Strategy.Strategy;
import com.TraderLight.DayTrader.Strategy.TrendStrategy;

public class MainSimulation {
	
	public static final Logger log = Logging.getLogger(true);
	public static Level1Quote prevQuote;
	public static double totalTradeCost=0;
	private static String broker=""; // The only brokers supported right now is TM
	public static int maxNumberOfPositions;
	public static int spreadTrading = 0;  // Default value is that we do not trade spread

	public static void main(String[] args) {
		
		int length = args.length;
		
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
		   } else {
			   log.info("Stategy ID not supported, assigning manual as default");
			   Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   }
			symbols += stock.getSymbol()+",";
		}
		
		
		

	}

}
