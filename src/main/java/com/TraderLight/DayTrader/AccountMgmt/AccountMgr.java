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

package com.TraderLight.DayTrader.AccountMgmt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.Strategy.Strategy;

/**
 *  This class has many functions: 1) is the entry point to place orders from all stock strategies; 2) keeps track of all the positions we open and close
 *  through the trading day; 3) keeps track of the cash account; 4) implements risk management
 *  
 * 
 * @author Mario Visco
 *
 */

public class AccountMgr {
	
	public static final Logger log = Logging.getLogger(true);
	
	// current cash situation 
	private double cash ;
	private final double maxCapital = -25000.0;
	
	private List<StockPosition> positions;
	private List<StockPosition> historyPositions;
	
	private double tradeCost = 0;
	private double currentTradeCost=0D;
	private final Lock updateLock=new ReentrantLock();
	private String broker;
	private double lifeGain=0;
	int max_number_of_positions;
	private int positionOpen =  0 ; // number of positions open simultaneously
	boolean mock;
	
	
	public AccountMgr(int maxNumberOfPositions, boolean mock) {
		
		this.cash = 0;
		positions = new ArrayList<StockPosition>();	
		historyPositions = new ArrayList<StockPosition>();
		this.max_number_of_positions = maxNumberOfPositions;
		this.mock = mock;
		
		
	}
	
	public void buy_or_sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy) {
				
		log.info("Entering buy_or_sell method" + " buy_sell is " + buy_sell + " positionOpen is " + positionOpen);
		
		// if we are trying to open a new position (leg = 1) allow it only if  the number of positions open is less than MAX
		if ( open_close_update.contentEquals("open")  && (positionOpen >= max_number_of_positions) ) {
			// return failure to the strategy
			log.info("Not allowing opening a new position for symbol " + quote.getSymbol());
			// increase position open it will be decreased in returnOrderParameters when we return failure
			positionOpen++;
			returnOrderParameters(false, quote.getSymbol(), " ", String.valueOf(lot), buy_sell, strategy, open_close_update);
			return;
		}
		
		if (open_close_update.contentEquals("open")) {
			
			log.info("Increasing position open" + (positionOpen+1));
			positionOpen++;
		}
		
		double price;
		
		if (buy_sell.contentEquals("buy")) {
			price = quote.getAsk();
		} else {
			price = quote.getBid();
		}
		
		String price_string = String.valueOf(price);
		
		if (mock) {
			// we are simulating do not send orders to the brokers
			returnOrderParameters(true, quote.getSymbol() , price_string, Integer.toString(lot), buy_sell, strategy, open_close_update);
			return;
		}
					
	    if (broker.contentEquals("TM")) {
			// TODO when closing an order we should get the filled quantity form the open position in the account manager list instead 
	    	// of relying on the lot coming from the strategy. If we operate at market the two things should coincide otherwise they may not.
			GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, quote.getSymbol(), price_string, lot, this, strategy);			
			Thread thread = new Thread(order);
			thread.start();	
			
		} else {
			log.info("Broker not supported in Account Mgr");
		}
		return;		
	}
	

	public void updateCash(double totalPrice, boolean buy_sell) {
		
		// if buy_sell is true we are buying so cash out else we are selling so cash in
		// assume that transaction cost  is tradeCost for transaction 
		
		if (buy_sell) {
			this.cash = this.cash -totalPrice - (tradeCost);		
		} else {
			this.cash = this.cash + totalPrice - (tradeCost);			
		}	
		this.currentTradeCost += tradeCost;
		log.info("Current Cash position is:            " + this.cash);
		return;
	}

	
	public void returnOrderParameters(Boolean success, String symbol, String price, String lot, String buy_sell, Strategy strategy, String open_close_update) {
		
    double stockPrice;
	int lot_int;
		
    updateLock.lock();
    log.info("Entering  returnOrderParameters");
	try {
	  log.info("Thread ID is: " + Thread.currentThread().getId()); 	  
	  if (!success) {
		  
		  if (open_close_update.contentEquals("open")) {
			  // we need to decrease the max position counter because we increased it in the buy method and the buy was not successful
			  log.info("Decreasing  position open because the buy was not succesfull " + (positionOpen-1));
			  positionOpen-=1;			  
		  }
		  // if no success on filling the order tell the strategy to adjust and return
		  strategy.strategyCallback(false, symbol, buy_sell, "0");	  
		  return;
	  }
	  
	  stockPrice=Double.parseDouble(price);
	  // this lot is the filled quantity it does not necessarily match with the lot in the strategy although it should be the same 
	  // if we place orders at market.
	  lot_int = Integer.parseInt(lot);
	  
	  double totalPrice=stockPrice*lot_int;
	  boolean long_short;
	  
	  if (buy_sell.contentEquals("buy")) {
		  updateCash(totalPrice, true);
	  } else {
		  updateCash(totalPrice, false);
	  }
	  
	  if (open_close_update.contentEquals("open")) {
		  //  allocate stock position object
		 if (buy_sell.contentEquals("buy")) {
			 long_short=true;
		 } else {
			 long_short = false;
		 }			 
		  StockPosition stock = new StockPosition(long_short, symbol, lot_int, stockPrice );
		  positions.add(stock);  
	  } else if (open_close_update.contentEquals("update")) {
		  
		  for (StockPosition stock : positions) {
			  if (stock.getSymbol().contentEquals(symbol)) {
				  // update stock position object
				  stock.updatePosition(lot_int, stockPrice);
				  break;			  
			  }
		  }
	  } else if (open_close_update.contentEquals("close")) {
		  for (StockPosition stock : positions) {
			  if (stock.getSymbol().contentEquals(symbol)) {
				  // close stock position object
				  stock.closePosition(stockPrice);
				  positionOpen-=1;
				  historyPositions.add(stock);
				  positions.remove(stock);
				  break;			  
			  }
		  }
	  }
	  
      // update strategy
      strategy.strategyCallback(true, symbol, buy_sell, price);
       
   } finally {
	   updateLock.unlock();
   }
  }
	 
	
	public void analyzeTrades() {
		
		StockPosition trade;
		int numberOfTrades=0;
		int numberOfWinningTrades =0;
		int numberofLoosingTrades=0;
		double currentGain=0D;
		double currentLoss=0D;
		double totalGain=0;
		String symbol;
		int quantity;
		double priceBought;
		double soldPrice;
		double gain;
		
		
		
		log.info("-------------------------------");
		log.info("Analyzing Trades.........");
		log.info("Analyzing Trades.........");
		log.info("Analyzing Trades.........");
		log.info("-------------------------------");
		
		Iterator<StockPosition> iterator = this.historyPositions.iterator();
		
		
		while (iterator.hasNext()){
					
			    trade =  (StockPosition)iterator.next();
			    symbol=trade.getSymbol();
				quantity=trade.getQuantity();	
				priceBought = trade.getPriceBought(); 
				soldPrice=trade.getPriceSold();
				gain=(soldPrice-priceBought)*(double)trade.getQuantity();				
				numberOfTrades+=1;
				log.info("These are the parameters of the trade: " + symbol + " " + quantity + " " 
				         + priceBought + " " + soldPrice + " " + gain);
			    if (gain>0) {
					numberOfWinningTrades = numberOfWinningTrades + 1;
					currentGain+=gain;
				} else if (gain<0) {
					numberofLoosingTrades = numberofLoosingTrades +1;
					currentLoss=currentLoss+gain;
				}
			    gain=priceBought=soldPrice=0;
			    quantity=0;
		}
		
		
		totalGain = currentGain+currentLoss;
		log.info("Number of Total Trades: " + numberOfTrades + " " + " Number of Winning Trades: " + numberOfWinningTrades + 
				  " Number of Loosing Trades: " + numberofLoosingTrades ) ;
		log.info("Gain from Trades: " + currentGain + " Loss from Trades: " + currentLoss + " Total Gain/Loss: " + totalGain);
		log.info("Total Transaction Costs are :" + this.currentTradeCost);	 // totalCost should be the same of currentTadeCost	
		
	}
	
	
	public double getTradeCost() {
		return currentTradeCost;
	}
	
	public void updateBroker(String broker) {
		this.broker=broker;
		if (broker.contentEquals("AMTD") ) {
			
			// set up trade costs
			this.tradeCost = 9.99;
								
		} else if ( (broker.contentEquals("OH")) || (broker.contentEquals("TM"))) {
			
			// set up trade costs
			this.tradeCost=4.2;
					
		} else {
			
			log.info("Broker not supported");
		}
		return;
	}
	 
    public boolean checkIfWeHaveCapital() {
    	if (this.cash <= this.maxCapital) {
    		return false;
    	}
    	return true;
    }
	
	public double getPortfolioValue(String symbol, Strategy strategy) {
		
		
		return (0);
		
	}
	
	public double getLifeGain() {
		return this.lifeGain;
	}
	
	public void updateMaxNumberOfPosition(int max_number) {
		this.max_number_of_positions=max_number;
	}
	
	public List<StockPosition> getStockPosition() {
		return positions;
	}

	public void resetAcctMgr() {
		// TODO Auto-generated method stub
		
	}

}
