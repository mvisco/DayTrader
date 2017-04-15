/*
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
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

import com.TraderLight.DayTrader.Ameritrade.GetQuoteAmeritrade;
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
			optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy,"0" );
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
		double percent_variation = 0.01; // how much out of the money is the strike of th options we buy given current price
		int expiration_from_now = 0; // weeks from current week
				
		priceS = String.valueOf(quote.getLast());
		optionS=getOption(quote,call_put, percent_variation, expiration_from_now);
		if (broker.contentEquals("AMTD")) {
			GenericOrderExecutorAMTD order = new GenericOrderExecutorAMTD(buy_sell, open_close_update, optionS, 
					quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
			Thread thread = new Thread(order);
			thread.start();
			
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
			 optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy, "0" );
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
			 optionReturnOrder(false,"", quote.getSymbol(), " ", String.valueOf(lot), buy_sell, open_close_update, strategy,"0" );
			 return;
		 }
		 String priceS = String.valueOf(quote.getLast());
			if (broker.contentEquals("AMTD")) {
				GenericOrderExecutorAMTD order = new GenericOrderExecutorAMTD(buy_sell, open_close_update, option.getSymbol(), 
						quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
				Thread thread = new Thread(order);
				thread.start();
				
			} else if (broker.contentEquals("TM")) {
				
				GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, option.getSymbol(), 
						quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
				Thread thread = new Thread(order);
				thread.start();	
				
			} else {
				log.info("Broker not supported in Account Mgr");
			}
		    	
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
    		optionReturnOrder(false,"", quote.getSymbol(), " ", "", buy_sell, "", strategy,"0" );
    		return;
    	}
    	
    	// get list of options to sell
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());
    	
    	for (OptionPosition option : listOfOption) {

    	    Xprice = getExercisePricefromSymbol(option.getSymbol());
    	    quantity = option.getQuantity();
    	    String priceS = String.valueOf(quote.getLast());
    		if (broker.contentEquals("AMTD")) {
    			GenericOrderExecutorAMTD order = new GenericOrderExecutorAMTD(buy_sell, open_close_update, option.getSymbol(), 
    					quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
    			Thread thread = new Thread(order);
    			thread.start();
    			
    		} else if (broker.contentEquals("TM")) {
    			
    			GenericOrderExecutorTM order = new GenericOrderExecutorTM(buy_sell, open_close_update, option.getSymbol(), 
    					quote.getSymbol(), priceS, lot, this, strategy, mock, true);				
    			Thread thread = new Thread(order);
    			thread.start();	
    			
    		} else {
    			log.info("Broker not supported in Account Mgr");
    		}
    	    
    	}
    	
		return;
    	
    }

    
    public void optionReturnOrder(boolean success, String optionSymbol, String symbol, String price, String buy_sell, String lot,
    		String open_close_update, Strategy strategy, String iv) {
    	
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
    			//TEMP CODE leave the strategy in STEMP for failure for debug......
    			//strategy.strategyCallback(false, symbol, buy_sell, "0");	  
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
    			OptionPosition option = new OptionPosition(optionSymbol, optionPrice, lot_int, singleTradeCost, Double.parseDouble(iv));
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
    			// store in DB
    			option.StoreInDB();
    			option.establishConnection("jdbc:mysql://localhost/Positions");
    			option.storeOptionPosition(option);
    			option.closeConnection();


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
    				OptionPosition option1 = new OptionPosition(optionSymbol, option.priceBought, lot_int, singleTradeCost, option.impVol);
    				option1.setPriceSold(Double.parseDouble(price));
    				List<OptionPosition> listOfOption;
    				listOfOption = new ArrayList<OptionPosition>();
    				listOfOption.add(option1);
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
    				// Store the day trade in the DB
    				option1.StoreInDB();
        			option1.establishConnection("jdbc:mysql://localhost/DayTrades");
        			option1.storeOptionPosition(option1, Calendar.getInstance().getTime());
        			option1.closeConnection();
    				
    			}
    			// update record in DB
    			option.StoreInDB();
    			option.establishConnection("jdbc:mysql://localhost/Positions");
    			option.updateOptionPosition(option);
    			option.closeConnection();

    		} else if (open_close_update.contentEquals("close")) {
    			log.info("Closing position for symbol " + symbol);
    			List<OptionPosition> listOfOption;
    			listOfOption = optionPositions.get(symbol);
    			List<OptionPosition> listOfOption1 = new ArrayList<OptionPosition>();
    			OptionPosition optionToClose=null;
    			for (OptionPosition option : listOfOption) {
    				log.info("adding option to temp list " + option.getSymbol());
    				listOfOption1.add(option);

    			}
    			for (OptionPosition option : listOfOption1) {
    				if (option.getSymbol().equals(optionSymbol) ) {
    					option.setPriceSold(optionPrice);
    					double singleTradeCost= (optionTradeCost+pricePerContract*(lot_int/100));
    					option.updateCost(singleTradeCost);
    					updateCash(totalPrice, false, singleTradeCost);
    					optionToClose = option;

    					// clear the current position  for this option from the official list
    					log.info("Removing option from the list " + option.getSymbol());
    					listOfOption.remove(option);
    					// remove from DB
    					option.StoreInDB();
    					option.establishConnection("jdbc:mysql://localhost/Positions");
    					option.deleteOptionPosition(option);
    					option.closeConnection();
    					if (listOfOption.size() == 0) {
    						// clear the map if there is nothing left
    						optionPositions.remove(symbol);
    						// reduce positions
    						positionOpen--;
    					}
    				}

    			}

    			// Clear the temp list that we used above to avoid a concurrency error and add the option we just closed
    			listOfOption1.clear();

    			if ( optionToClose != null) {
    				listOfOption1.add(optionToClose);
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
		//TEMP CODE START
		strike_percent_variation=0;
		//TEMP CODE END
		
		   		
		price = quote.getBid();		
		Stock stock = null;
		for (Stock s : listOfStocks) {
			if (s.getSymbol().contentEquals(quote.getSymbol())) {
				stock = s;
				break;
			}
		}
		//double infPrice = price - strike_percent_variation*price;
		//double supPrice = price + strike_percent_variation*price;
		
		int price_int = (int)(price*10);
		double infPrice = (price_int/10.0) - stock.getStrike_increment();
		double supPrice = (price_int/10.0) + stock.getStrike_increment();
		log.info("infPrice is " + infPrice);		
		log.info("supPrice is " + supPrice );
		
		
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
			for (int i = (stock.getStrikes().size() -1); i >=0; i--) {
				BigDecimal bd = stock.getStrikes().get(i);
				if (bd.doubleValue() <= infPrice) {
					strike = bd;
					break;
				}
			}
			
		}
		
		BigDecimal new_strike;
		new_strike = strike.stripTrailingZeros();
		
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
	    	
	    // TODO TEMP CODE START OVERWRITE EXPIRATION
	    expiration = "20170413";
	    // TEMP CODE END
	    optionSymbol=symbol+":"+expiration+":"+new_strike.toPlainString()+":"+callOrPut;	    
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
			Sprice = quote.getBid();
			OptionCostProvider ocp = new OptionCostProvider();
	    	for (OptionPosition option : optionPositions.get(quote.getSymbol())) {	    		
	    		optionSymbol=option.getSymbol();
	    		Xprice = getExercisePricefromSymbol(optionSymbol);
	    		//T1 = getExpirationTime(quote);	
	    		T1 = getExpirationTime(quote, option.getSymbol());
	    		//log.info(Sprice + " " + Xprice + " " +T1 + " "+ optionSymbol + " "+ option.impVol);
	    		
	    		if (getCallOrPutfromSymbol(option.getSymbol()).equals("C"))  {
	    			delta += ocp.getCallDelta(Sprice, Xprice, T1, option.impVol)*option.getQuantity();
	    			//log.info("Call delta is " + ocp.getCallDelta(Sprice, Xprice, T1, option.impVol) + " " + optionSymbol);
	    		} else {
	    			delta += ocp.getPutDelta(Sprice, Xprice, T1, option.impVol)*option.getQuantity();
	    			//log.info("Put delta is " + ocp.getPutDelta(Sprice, Xprice, T1, option.impVol) + " " + optionSymbol);
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
    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime, option.impVol);
    		     
    		     

    	    } else {
    	    	Sprice = quote.getAsk();
    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,option.impVol);
    		    
    		    
    	    }
    	    log.info("Theoretical option price for " + option.getSymbol() + " is " + optionPrice);
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
	
	public double getPortfolioValue(Level1Quote quote, Strategy strategy, boolean market) {
				
		int quantity;		
		String optionPrice;
		double totalOptionPrice=0;
		double pricePaid = 0;
		double stock_value=0;
		
	   	if (!optionPositions.containsKey(quote.getSymbol())) {
   		
    		return 0;
    	}
	   
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());

    	double sigma = strategy.getImpVol();
    	
    	for (OptionPosition option : listOfOption) {
    		
    		String[] symbolSplit = option.getSymbol().split(":");

    		// Date used by Account Mgr is in the form of 20130419 while AMTD needs 041913		
    		DateFormat df = new SimpleDateFormat("yyyyMMdd"); 
    		Date oldDate=new Date();
    		try {
    			oldDate = df.parse(symbolSplit[1]);
    		} catch (ParseException e2) {
    			log.info("Got problems in parsing the date");
    			e2.printStackTrace();
    		}
    		// we now have a date in the format of yyyyMMdd
    		// we need to convert it in MMddyy
    		DateFormat df1 = new SimpleDateFormat("MMddyy");
    		String expiration = df1.format(oldDate);

    		String optionSymbolAMTD = symbolSplit[0]+"_"+expiration+symbolSplit[3]+symbolSplit[2];
    		
    		String iv = "";		
    		
    		// Get the quote now
    		 GetQuoteAmeritrade optionQuote = new GetQuoteAmeritrade();
    		
    		try { 
    			
    			optionPrice = optionQuote.getQuotes(optionSymbolAMTD,false,false,0);
    			iv = optionQuote.getIv();
    			optionPrice = optionQuote.getBid(); 
    			
    			
    		} catch (Exception e) {
    			log.info("Something went wrong with getting quote for symbol " + optionSymbolAMTD);
    			e.printStackTrace();
    			optionPrice="0";
    			
    		}
    		
    		if (String.valueOf(optionPrice).isEmpty()) {
    			// We get here if something went wrong with getting the quote
    			optionPrice = "0";
    		}

    	   
    	    totalOptionPrice += Double.valueOf(optionPrice)*option.getQuantity();  
    	    pricePaid += option.getPriceBought()*option.getQuantity();
    	}
    	
    	for (StockPosition stock : positions) {
			  if (stock.getSymbol().contentEquals(quote.getSymbol())) {
				  // assumes that we are long otherwise quantity should be negative for delta calculation
				  quantity = stock.getQuantity();
				  stock_value = quantity * quote.getBid() - stock.getPriceBought()*quantity ;
				  
			  }
		}
    	
    	//log the theoretical value of the option
    	//getPortfolioValue(quote, strategy);
    	
		return( totalOptionPrice-pricePaid+stock_value);
	
		
	}
	
	public void initializePositions(String symbol, Strategy strategy) {
		
		// This method gets called at startup time to get existing positions that we may have from previous day
		Connection con=null;
		Statement stmt;
		ResultSet rs;
		
		//Register the JDBC driver for MySQL.
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		String url = "jdbc:mysql://localhost/Positions";
		try {
			con = DriverManager.getConnection(url,"root", "password");
			stmt = con.createStatement();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			 
	
		String selectOption = "SELECT * FROM position where SYMBOL='"+symbol+"'";
		int id;
		
		try {
			Statement statement = con.createStatement();
			rs = statement.executeQuery(selectOption);
			while (rs.next()) {
				
				//String stock_symbol = rs.getString("SYMBOL");
				String option_symbol = rs.getString("OPTION_SYMBOL");
				int q = rs.getInt("QUANTITY");
				double pb = rs.getDouble("PRICE_BOUGHT");
				//double ps = rs.getDouble("PRICE_SOLD");
				double iv = rs.getDouble("IMP_VOL");
				double tc = rs.getDouble("TRANSACTION_COST");
				OptionPosition option = new OptionPosition(option_symbol, pb, q, tc, iv);
    			// add to positions
    			if (optionPositions.containsKey(symbol)) {
    				// stock symbol already exists in the map
    				optionPositions.get(symbol).add(option);
    			} else {
    				List<OptionPosition> listOption = new ArrayList<OptionPosition>();
    				listOption.add(option);
    				optionPositions.put(symbol, listOption);
    			}
    			// move strategy to the right state 
    			// let's move it directly to S0 for GammaScalping it should work because we should have two positions
    			strategy.setStateToS0();
			   
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
