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

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.StockTrader.Stock;
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
	
	// stock objects
	private List<StockPosition> positions;
	private List<StockPosition> historyPositions;
	//option objects
	private Map<String, List<OptionPosition>> optionPositions = new HashMap<String, List<OptionPosition>>();
	private Map<Integer, List<OptionPosition>> dayTrades = new HashMap<Integer, List<OptionPosition>>();		
	private List<Stock> listOfStocks;	
	private double tradeCost = 0;
	private double currentTradeCost=0D;
	private final Lock updateLock=new ReentrantLock();
	private String broker;
	private double lifeGain=0;
	int max_number_of_positions;
	private int positionOpen =  0 ; // number of positions open simultaneously
	boolean mock;

	private double optionTradeCost;

	private double pricePerContract;
	
	
	public AccountMgr(int maxNumberOfPositions, boolean mock, List<Stock> listOfStocks) {
		
		this.cash = 0;
		positions = new ArrayList<StockPosition>();	
		historyPositions = new ArrayList<StockPosition>();
		this.max_number_of_positions = maxNumberOfPositions;
		this.mock = mock;
		this.listOfStocks = listOfStocks;		
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
	
    public void updateCash(double totalPrice, boolean buy_sell, double tradeCost) {
		
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
		analyzeOptionTrades();
	}
	
	private void analyzeOptionTrades() {
		
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
		log.info("Analyzing Option Trades.........");
		log.info("Analyzing Option Trades.........");
		log.info("Analyzing Option Trades.........");
		log.info("-------------------------------");
		
		for (List<OptionPosition> l : dayTrades.values()) {
			
			for (OptionPosition option : l ) {
				
				symbol=option.getSymbol();
				quantity=option.getQuantity();	
				priceBought = option.getPriceBought(); 
				soldPrice=option.getPriceSold();
				gain=(soldPrice-priceBought)*(double)option.getQuantity();				
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
			this.optionTradeCost = 3.5;
			this.pricePerContract = 0.75;
								
		} else if ( (broker.contentEquals("OH")) || (broker.contentEquals("TM"))) {
			
			// set up trade costs
			this.tradeCost=4.2;
			this.optionTradeCost = 3.5;
			this.pricePerContract = 0.15;
					
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
	
	public void option_buy_or_sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, String call_or_put, 
			Strategy strategy) {
		
		log.info("Entering option_buy_or_sell method" + " buy_sell is " + buy_sell + " positionOpen is " + positionOpen);
		
		// if we are trying to open a new position allow it only if  the number of positions open is less than MAX
		if ( open_close_update.contentEquals("open")  && (positionOpen >= max_number_of_positions) ) {
			// return failure to the strategy
			log.info("Not allowing opening a new position for symbol " + quote.getSymbol());
			// increase position open it will be decreased in returnOrderParameters when we return failure
			positionOpen++;
			optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy );
			return;
		}
		
		if (open_close_update.contentEquals("open")) {
			
			log.info("Increasing position open" + (positionOpen+1));
			positionOpen++;
		}
		
		
		boolean call_put = true;
		
		if (call_or_put.contentEquals("P")) {
			call_put = false;
		} 
				
		if (buy_sell.contentEquals("buy")) {			
			buyOption(quote, lot, call_put, strategy, buy_sell,  open_close_update);
			
		} else {			
			sellOption(quote, lot, call_put, strategy,buy_sell,  open_close_update );
		}	

	}
	
    public void buyOption (Level1Quote quote, int lot, boolean call_put, Strategy strategy, String buy_sell, String open_close_update) {	
		
		String optionS="";
		String priceS;		
		double percent_variation = 0.02; // how much out of the money is the strike of th options we buy given current price
		int expiration_from_now = 1; // weeks from current week
				
		priceS = String.valueOf(quote.getLast());
		optionS=getOption(quote,call_put, percent_variation, expiration_from_now);
		if (broker.contentEquals("AMTD")) {
			// TODO
			;
		} else if (broker.contentEquals("TM")) {
			
			GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, optionS, 
					quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
			Thread thread = new Thread(order);
			thread.start();	
			
		} else {
			log.info("Broker not supported in Account Mgr");
		}
		return;
		
	 }
    
	 public void deltaAdjustment (String buy_sell, String open_close_update, Level1Quote quote, int lot, String call_or_put, 
				Strategy strategy) {
		 
		// we have to sell/buy quantity associated with symbol
		 log.info("Entering sellDeltaAdjustment method for symbol " + quote.getSymbol());
		 log.info("Date is " + quote.getCurrentDateTime());
		 log.info("Portfolio Delta is " + calculatePortfolioDelta(quote,strategy));
		 double Sprice = quote.getLast();
		 double Xprice=0;
		 //double expirationTime = getExpirationTime(quote);
		 OptionCostProvider optionQuote = new OptionCostProvider();

		 double sigma = strategy.getImpVol();

		 if (!optionPositions.containsKey(quote.getSymbol())) {
			 log.info( " Something is asking to sell positions that we do not have ");
			 optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy );
			 return;
		 }

		 // get the option 
		 OptionPosition option= null;
		 for (OptionPosition o : optionPositions.get(quote.getSymbol())) {

			 Xprice = getExercisePricefromSymbol(o.getSymbol());

			 if (getCallOrPutfromSymbol(o.getSymbol()).equals(call_or_put))  {
				 option = o;
				 break;
			 }
		 }	

		 if (option == null) {
			 log.info("No option found in Account Mgr");
			 optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy );
			 return;
		 }
		 String priceS = String.valueOf(quote.getLast());
		 GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, option.getSymbol(), 
				 quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
		 Thread thread = new Thread(order);
		 thread.start();
		    	
	}


    public void sellOption (Level1Quote quote, int lot, boolean call_put, Strategy strategy, String buy_sell, String open_close_update) {	
	// we have to sell all options associated with symbol

    	Integer quantity;
    	double  optionPrice;

    	//log.info("Entering sell method for symbol " + symbol);
    	//log.info("Date is " + quote.getCurrentDateTime());
    	double Sprice = quote.getLast();
    	double Xprice=0;
    	//double expirationTime = getExpirationTime(quote);
    	
    	OptionCostProvider optionQuote = new OptionCostProvider();

    	double sigma = strategy.getImpVol();
    	
    	if (!optionPositions.containsKey(quote.getSymbol())) {
    		log.info( " Something is asking to sell positions that we do not have ");
    		optionReturnOrder(false,"", quote.getSymbol(), " ", "", buy_sell, "", strategy );
    		return;
    	}
    	
    	// get list of options to sell
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());
    	
    	for (OptionPosition option : listOfOption) {

    	    Xprice = getExercisePricefromSymbol(option.getSymbol());
    	    quantity = option.getQuantity();
    	    String priceS = String.valueOf(quote.getLast());
   		    GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, option.getSymbol(), 
   				 quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
   		    Thread thread = new Thread(order);
   		    thread.start();
    	}
    	
		return;
    	
    }

    
    public void optionReturnOrder(boolean success, String optionSymbol, String symbol, String price, String buy_sell, String lot,
    		String open_close_update, Strategy strategy) {
    	
        double optionPrice;
    	int lot_int;
    		
        updateLock.lock();
        log.info("Entering  optionReturnOrder");
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
    	  
    	  optionPrice=Double.parseDouble(price);
    	  // this lot is the filled quantity it does not necessarily match with the lot in the strategy although it should be the same 
    	  // if we place orders at market.
    	  lot_int = Integer.parseInt(lot);
    	  
    	  double totalPrice=optionPrice*lot_int;
   	  
    	  if (open_close_update.contentEquals("open")) {
    		  
    		  //  allocate option position object   		 
    		 double singleTradeCost= (optionTradeCost+pricePerContract*(lot_int/100)); 
    		 OptionPosition option = new OptionPosition(optionSymbol, optionPrice, lot_int, singleTradeCost);
    		 updateCash(totalPrice, true, singleTradeCost);
    		 // add to positions
    		 if (optionPositions.containsKey(symbol)) {
    			 // symbol already exists in the map
    			 optionPositions.get(symbol).add(option);
    		 } else {
    			 List<OptionPosition> listOption = new ArrayList<OptionPosition>();
    			 listOption.add(option);
    			 optionPositions.put(symbol, listOption);
    		 }

    		 
    	  } else if (open_close_update.contentEquals("update")) {
    		  
  	    	if (!optionPositions.containsKey(symbol)) {
	    		log.info( " Something is asking to update positions that we do not have ");
	    		strategy.strategyCallback(false, symbol, buy_sell, "0");
	    		return;
	    	}
  	    	
	    	// get the option 
	    	OptionPosition option= null;
	    	for (OptionPosition o : optionPositions.get(symbol)) {	       		 
	    		if (o.getSymbol().equals(optionSymbol))  {
	    			option = o;
	    			break;
	    		}
	    	}	
	    	
	    	if (option == null) {
	    		log.info("No option found in Account Mgr");
	    		strategy.strategyCallback(false, symbol, buy_sell, "0");
	    		return;
	    	} 
	    	  	    
	    	double singleTradeCost= (optionTradeCost+pricePerContract*(lot_int/100));
	    	
	    	if (buy_sell.contentEquals("buy")) {
    	        option.priceBought = (option.priceBought*option.getQuantity() + optionPrice*lot_int)/(option.getQuantity() + lot_int);   	   
    	        option.updateQuantity(lot_int);
    	        updateCash(Double.parseDouble(price)*lot_int, true, singleTradeCost);
    	        strategy.strategyCallback(true, symbol, buy_sell, "0");
	    	} else {
	    		// We sold some options so we need to create a trade in the daytrades structure
	    		 OptionPosition option1 = new OptionPosition(optionSymbol, option.priceBought, lot_int, singleTradeCost);
	    		 option1.setPriceSold(Double.parseDouble(price));
	    		 List<OptionPosition> listOfOption;
	    	     listOfOption = new ArrayList<OptionPosition>();
	    	     int i = 0;
	    	     while (true) {
	    	    	 i++;
	    	    	 if (!dayTrades.containsKey(i)) {
	    	    		 dayTrades.put(i, listOfOption);
	    	    		 break;
	    	    	 }
	    	     }
	    	     updateCash(Double.parseDouble(price)*lot_int, false, singleTradeCost);
	    	     option.updateQuantity(-lot_int);
	    	}
            
    	  } else if (open_close_update.contentEquals("close")) {
    	    	List<OptionPosition> listOfOption;
    	    	listOfOption = optionPositions.get(symbol);
    	    	List<OptionPosition> listOfOption1 = new ArrayList<OptionPosition>();
    	    	for (OptionPosition option : listOfOption) {
    	    		listOfOption1.add(option);
    	    	}
    	    	for (OptionPosition option : listOfOption1) {
    	    		if (option.getSymbol().equals(optionSymbol) ) {
    	    			option.setPriceSold(optionPrice);
    	    			double singleTradeCost= (optionTradeCost+pricePerContract*(lot_int/100));
    	        	    option.updateCost(singleTradeCost);
    	        	    updateCash(totalPrice, false, singleTradeCost);
    	            	
    	            	// clear the current position  for this option from the list
    	            	listOfOption.remove(option);
    	            	if (listOfOption.size() == 0) {
    	            		// clear the map if there is nothing left
    	            	    optionPositions.remove(symbol);
    	            	    // reduce positions
    	            	    positionOpen--;
    	            	}
    	            	
    	            	break;
    	    		}
    	    		
    		  }
    	    	// get the list of trades to the dayTrades structure
            	int i = 0;
            	while (true) {
            		i++;
            		if (!dayTrades.containsKey(i)) {
            			dayTrades.put(i, listOfOption1);
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
	
  
	public String getOption(Level1Quote quote, boolean buy, double strike_percent_variation, int expiration_in_weeks_from_now) {
		
		
		String expiration;
		String callOrPut = "";
		String optionSymbol=""; 
		double price;
		String symbol;
		   		
		price = quote.getLast();
		double infPrice = price - strike_percent_variation*price;
		double supPrice = price + strike_percent_variation*price;
		
		
		Stock stock = null;
		for (Stock s : listOfStocks) {
			if (s.getSymbol().contentEquals(quote.getSymbol())) {
				stock = s;
				break;
			}
		}
		
		// get the strike for the option. The idea is that for calls we get the the lowest strike that is >= supPrice
		// while for put we get the highest strike that is >= infPrice
		BigDecimal strike= new BigDecimal("0");
		
		if (buy) {
			callOrPut = "C";
			for (BigDecimal bd : stock.getStrikes()) {
				if (bd.doubleValue() >= supPrice) {
					strike = bd;
					break;
				}
			}
		} else {
			callOrPut = "P";
			for (BigDecimal bd : stock.getStrikes()) {
				if (bd.doubleValue() >= infPrice) {
					strike = bd;
					break;
				}
			}
		}
		
		symbol = quote.getSymbol();
		
		Date date = quote.getCurrentDateTime();
		Calendar rightNow = Calendar.getInstance();
		rightNow.setTime(date);		   		
		Calendar first = (Calendar) rightNow.clone();
	    first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));	    
	    // and add 5 days to get to Friday of the week
	    first.add(Calendar.DAY_OF_YEAR, 5);
	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
	    expiration = df.format(first.getTime());
	    
	    for (int i = 0; i < stock.getExpirations().size(); i++) {
	    	if (stock.getExpirations().get(i).contentEquals(expiration)) {
	    		if (i < stock.getExpirations().size() - expiration_in_weeks_from_now) {
	    			// get the expiration time by bumping the index based on the passed parameter 
	    			expiration = stock.getExpirations().get(i+expiration_in_weeks_from_now);
	    			break;
	    		} else {
	    			//get the current one because we could  go out of the array memory if we bump the index
	    			expiration = stock.getExpirations().get(i);
	    			break;
	    		}
	    	}
	    }
	    	
	    optionSymbol=symbol+":"+expiration+":"+strike.toString()+":"+callOrPut;	    
	    return optionSymbol;
	     		
	}
	
	

	public double getExpirationTime(Level1Quote quote, String optionSymbol) {
		
		Date currentDate=quote.getCurrentDateTime();
		Date expirationDate = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");    		
		try {
			 expirationDate = sdf.parse(getExpirationFromSymbol(optionSymbol));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        Calendar now = Calendar.getInstance();
		now.setTime(currentDate);
		Calendar expiration = Calendar.getInstance();
		expiration.setTime(expirationDate);
        return (daysBetween(expiration, now)/365.0);
	}
	
	public  int daysBetween(Calendar day1, Calendar day2){
	    Calendar dayOne = (Calendar) day1.clone(),
	            dayTwo = (Calendar) day2.clone();

	    if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
	        return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
	    } else {
	        if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
	            //swap them
	            Calendar temp = dayOne;
	            dayOne = dayTwo;
	            dayTwo = temp;
	        }
	        int extraDays = 0;

	        int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

	        while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
	            dayOne.add(Calendar.YEAR, -1);
	            // getActualMaximum() important for leap years
	            extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
	        }

	        return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays ;
	    }
	}
	    
	public double getExercisePricefromSymbol(String optionSymbol) {
		
		String[] symbolSplit = optionSymbol.split(":");
		
		return(Double.parseDouble(symbolSplit[2]));
		
		
	}
	
	public String getCallOrPutfromSymbol(String optionSymbol) {
		
		String[] symbolSplit = optionSymbol.split(":");
		return symbolSplit[3];
		
	} 
	
	public String getExpirationFromSymbol(String optionSymbol) {
		String[] symbolSplit = optionSymbol.split(":");
		return symbolSplit[1];
	}
	
	public double calculatePortfolioDelta(Level1Quote quote, Strategy strategy) {
		
		// TODO This has to change because the calculation should be done on market values and not theoretical values.
		
		String optionSymbol="";
		double Xprice;
		double Sprice;
		double T1;		
		double delta = 0.0;
		double sigma = strategy.getImpVol();
		if (optionPositions.containsKey(quote.getSymbol())) {
			
	    	for (OptionPosition option : optionPositions.get(quote.getSymbol())) {
	    		Sprice = quote.getBid();
	    		optionSymbol=option.getSymbol();
	    		Xprice = getExercisePricefromSymbol(optionSymbol);
	    		//T1 = getExpirationTime(quote);	
	    		T1 = getExpirationTime(quote, option.getSymbol());
	    		OptionCostProvider ocp = new OptionCostProvider();
	    		if (getCallOrPutfromSymbol(option.getSymbol()).equals("C"))  {
	    			delta += ocp.getCallDelta(Sprice, Xprice, T1, sigma)*option.getQuantity();
	    		} else {
	    			delta += ocp.getPutDelta(Sprice, Xprice, T1, sigma)*option.getQuantity();
	    		}
	    		
	    	}	    	
		}
		return delta;
	}
	
	
	
    public int getQuantity(Level1Quote quote, String optionType) {
		
		int quantity =0;
					
		if (optionPositions.containsKey(quote.getSymbol())) {
			
	    	for (OptionPosition option : optionPositions.get(quote.getSymbol())) {
	    		
	    		if (getCallOrPutfromSymbol(option.getSymbol()).equals(optionType))  {
	    			quantity = option.getQuantity();
	    		}
	    		
	    	}	    	
		}
					
		return quantity;
	}
    
	public double getPortfolioValue(Level1Quote quote, Strategy strategy) {
		double Xprice;
		int quantity;
		double Sprice;
		double optionPrice;
		double totalOptionPrice=0;
		double pricePaid = 0;
		double stock_value=0;
		
	   	if (!optionPositions.containsKey(quote.getSymbol())) {
   		
    		return 0;
    	}
	   
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());
    	//double expirationTime = getExpirationTime(quote);
    	OptionCostProvider optionQuote = new OptionCostProvider();

    	double sigma = strategy.getImpVol();
    	
    	for (OptionPosition option : listOfOption) {

    	    Xprice = getExercisePricefromSymbol(option.getSymbol());
    	    quantity = option.getQuantity();
    	    double expirationTime = getExpirationTime(quote, option.getSymbol());
    	    if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
    	    	 Sprice = quote.getBid();
    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime, sigma);
    		     

    	    } else {
    	    	Sprice = quote.getAsk();
    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
    		    
    		    
    	    }
    	    totalOptionPrice += optionPrice*option.getQuantity();  
    	    pricePaid += option.getPriceBought()*option.getQuantity();
    	}
    	
    	for (StockPosition stock : positions) {
			  if (stock.getSymbol().contentEquals(quote.getSymbol())) {
				  // assumes that we are long otherwise quantity should be negative for delta calculation
				  quantity = stock.getQuantity();
				  stock_value = quantity * quote.getBid() - stock.getPriceBought()*quantity ;
				  
			  }
		}
    	
		return( totalOptionPrice-pricePaid+stock_value);
	
		
	}
	

}
