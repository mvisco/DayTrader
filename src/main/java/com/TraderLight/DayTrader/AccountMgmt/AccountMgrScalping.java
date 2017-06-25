package com.TraderLight.DayTrader.AccountMgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.Strategy.Strategy;

public class AccountMgrScalping extends AccountMgr{
	

	public static final Logger log = Logging.getLogger(true);
	
    public AccountMgrScalping(int maxNumberOfPositions, boolean mock, boolean tradeOption) {
		
		super(maxNumberOfPositions, mock, tradeOption);
	}
    
	public void buy_or_sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy) {
		
		boolean call_put = true;		
		if (open_close_update.contentEquals("open") ) {
			if (buy_sell.contentEquals("buy")) {
				// buy calls
				buyOption(quote, lot, call_put, strategy, false, buy_sell);
			} else {
				// buy puts
				call_put = false;
				buyOption(quote, lot, call_put, strategy, false, buy_sell);
			}
			
		} else if (open_close_update.contentEquals("update")) {
			// This is where the delta adjustment happens. 
			calculatePortfolioDelta(quote);
			
			
		} else {
			// close case sell everything we got
			sellOption(quote, quote.getSymbol(), strategy, buy_sell);
		}	
	}
	
	public double calculatePortfolioDelta(Level1Quote quote) {
		
		String optionSymbol="";
		double Xprice;
		double Sprice;
		double T1;
		double sigma = 0.1;
		double delta = 0.0;

		if (optionPositions.containsKey(quote.getSymbol())) {
			
	    	for (OptionPosition option : optionPositions.get(quote.getSymbol())) {
	    		Sprice = quote.getBid();
	    		optionSymbol=option.getSymbol();
	    		Xprice = getExercisePricefromSymbol(optionSymbol);
	    		T1 = getExpirationTime(quote);		
	    		OptionCostProvider ocp = new OptionCostProvider();
	    		if (getCallOrPutfromSymbol(option.getSymbol()).equals("C"))  {
	    			delta += ocp.getCallDelta(Sprice, Xprice, T1, sigma);
	    		} else {
	    			delta += ocp.getPutDelta(Sprice, Xprice, T1, sigma);
	    		}
	    		
	    	}	    	
		}		
		return delta;
	}
	
	
}
