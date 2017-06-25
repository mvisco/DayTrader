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

import java.text.DecimalFormat;
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
	
	//Stock Objects
	private List<StockPosition> positions;
	private List<StockPosition> historyPositions;
	
	//Option Objects
	protected Map<String,List<OptionPosition>> optionPositions;
	protected Map<Integer, List<OptionPosition> > dayTrades;
	
	private double tradeCost = 0;
	private double optionTradeCost = 0;
	private double currentTradeCost=0D;
	private final Lock updateLock=new ReentrantLock();
	private String broker;
	private double lifeGain=0;
	int max_number_of_positions;
	private int positionOpen =  0 ; // number of positions open simultaneously
	boolean mock;
	double totalGain;
	boolean tradeOption = false;
	private String optionSymbolInTheMoney = "";
	private String optionSymbolOutoftheMoney = "";
	
	
	public AccountMgr(int maxNumberOfPositions, boolean mock, boolean tradeOption) {
		
		this.cash = 0;
		positions = new ArrayList<StockPosition>();	
		historyPositions = new ArrayList<StockPosition>();
		optionPositions = new HashMap<String, List<OptionPosition>>();
		dayTrades = new HashMap<Integer, List<OptionPosition>>();
		this.max_number_of_positions = maxNumberOfPositions;
		this.mock = mock;
		this.tradeOption = tradeOption;		
	}
	
	public void buy_or_sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy) {
		this.max_number_of_positions  = 10000;
		if (this.tradeOption == true) {
			// call the option related method
			option_buy_or_sell(buy_sell, open_close_update, quote, lot, strategy);
			return;
		}
				
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
	
	public void sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy, int leg) {
		this.max_number_of_positions  = 10000;
		sellOptionLeg(buy_sell, open_close_update, quote, lot, strategy, leg);
		
	}
	
	public void sellOptionLeg(String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy, int leg) {
		
		// right now we sell only the last option
    	Integer quantity;
    	double  optionPrice;

    	//log.info("Entering sell method for symbol " + symbol);
    	//log.info("Date is " + quote.getCurrentDateTime());
    	double Sprice = quote.getLast();
    	double Xprice=0;
    	
    	OptionCostProvider optionQuote = new OptionCostProvider();

    	double sigma = strategy.getImpVol();
    	
    	if (!optionPositions.containsKey(quote.getSymbol())) {
    		log.info( " Something is asking to sell positions that we do not have ");
    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
    		return;
    	}
    	
    	// get list of options to sell
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());
    	OptionPosition option = listOfOption.get(listOfOption.size() -1);
    	

    	Xprice = getExercisePricefromSymbol(option.getSymbol());
    	quantity = option.getQuantity();
    	double expirationTime = getExpirationTime(quote, option.getSymbol());
    	if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
    		Sprice = quote.getBid();
    		optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime,sigma);
    		option.setPriceSold(optionPrice);
    		log.info("Option symbol " + option.getSymbol());
    		log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
    		log.info("delta of the option is " + optionQuote.getCallDelta(Sprice, Xprice, expirationTime, sigma));

    	} else {
    		Sprice = quote.getAsk();
    		optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
    		option.setPriceSold(optionPrice);
    		log.info("Option symbol " + option.getSymbol());
    		log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
    		log.info("delta of the option is " + optionQuote.getPutDelta(Sprice, Xprice, expirationTime, sigma));
    	}
    	double totalOptionPrice=optionPrice*option.getQuantity();

    	double singleTradeCost= (optionTradeCost+0.15*(option.getQuantity()/100));
    	option.updateCost(singleTradeCost);
    	updateCash(totalOptionPrice, false, singleTradeCost);
    	List<OptionPosition> listOfOption1 = new ArrayList<OptionPosition>();
    	listOfOption1.add(option);
    	// get the list of trades to the dayTrades structure
    	int i = 0;
    	while (true) {
    		i++;
    		if (!dayTrades.containsKey(i)) {
    			dayTrades.put(i, listOfOption1);
    			break;
    		}
    	}
    	
    	// clear the current position  for this symbol because they have been closed
    	//optionPositions.remove(quote.getSymbol());
    	listOfOption.remove(listOfOption.size() -1);
    	// reduce positions
    	positionOpen--;
    	optionReturnOrder(true, quote.getSymbol(), " ", buy_sell , strategy );
		return;
		
	}
	
	public void option_buy_or_sell (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy) {
		
		/*
		 * Options are different than stocks when the strategy issue an order. The following table applies for option:
		 * buy_sell = buy open_close_update = open then we need to buy a lot of calls
		 * buy_sell = buy open_close_update = update  then we need to buy another lot of calls
		 * buy_sell = buy open_close_update = close then it means that we have puts that we need to sell
		 * buy_sell = sell open_close_update = open then we need to buy a lot of puts
		 * buy_sell = sell open_close_update = update  then we need to buy another lot of puts
		 * buy_sell = sell open_close_update = close  then it means that we have calls that we have to sell
		 */
		
		log.info("Entering option_buy_or_sell method" + " buy_sell is " + buy_sell + " positionOpen is " + positionOpen);
		if ( open_close_update.contentEquals("open")  && (positionOpen >= max_number_of_positions) ) {
			// return failure to the strategy
			log.info("Not allowing opening a new position for symbol " + quote.getSymbol());
			// increase position open it will be decreased in returnOrderParameters when we return failure
			//positionOpen++;
			optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
			return;
		}
		
		if (open_close_update.contentEquals("open")) {
			positionOpen++;
		}
		boolean call_put = true;		
		if (open_close_update.contentEquals("open") || open_close_update.contentEquals("update") ) {
			if (buy_sell.contentEquals("buy")) {
				// buy calls
				buyOption(quote, lot, call_put, strategy, false, buy_sell);
			} else {
				// buy puts
				call_put = false;
				buyOption(quote, lot, call_put, strategy, false, buy_sell);
			}
		} else {
			// close case sell everything we got
			sellOption(quote, quote.getSymbol(), strategy, buy_sell);
		}	
	}
	
	
	
    public void buyOption (Level1Quote quote, int lot, boolean call_put, Strategy strategy, boolean inTheMoney, String buy_sell) {	
		
		String optionS="";
		double optionPrice;
		double Xprice;
		double Sprice;
		double T1;
		
		
		Sprice = quote.getLast();
		optionS=optionSymbolWeekly(quote,call_put, inTheMoney);
		//optionS=getOption(quote,call_put, inTheMoney);
		Xprice = getExercisePricefromSymbol(optionS);
		//T1 = getExpirationTime(quote);
		T1 = getExpirationTime(quote, optionS);
		OptionCostProvider ocp = new OptionCostProvider();
		double sigma = strategy.getImpVol();
		
		if (call_put) {
			Sprice = quote.getBid();
			optionPrice = ocp.getCallCost(Sprice,Xprice,T1,sigma);
			log.info("Option symbol " + optionS);
			log.info("Buy price is " + optionPrice + " Quantity is " + lot);
			log.info("Time to epiration is " + T1);
			log.info("delta of the option is " + ocp.getCallDelta(Sprice, Xprice, T1, sigma));
			
		} else {
			Sprice = quote.getBid();
			optionPrice = ocp.getPutCost(Sprice,Xprice,T1,sigma);
			log.info("Option symbol " + optionS);
			log.info("Buy price is " + optionPrice);
			log.info("Buy price is " + optionPrice + " Quantity is " + lot);
			log.info("Time to epiration is " + T1);
			log.info("delta of the option is " +  ocp.getPutDelta(Sprice, Xprice, T1, sigma));
			
		}
		// When buying simulate bid-ask spread .....
		
		if (optionPrice<3.0) {
			optionPrice=optionPrice+0.01;
		} else {
			optionPrice=optionPrice+0.1;
		}
		
		double totalOptionPrice=optionPrice*lot;	    
	    double singleTradeCost= (optionTradeCost+0.15*(lot/100)); 
	    updateCash(totalOptionPrice, true, singleTradeCost);
	    OptionPosition option = new OptionPosition(optionS, optionPrice, lot, singleTradeCost);
		 
		// add to positions
	    if (optionPositions.containsKey(quote.getSymbol())) {
	    	// symbol already exists in the map
	    	optionPositions.get(quote.getSymbol()).add(option);
	    } else {
	    	List<OptionPosition> listOption = new ArrayList<OptionPosition>();
	    	listOption.add(option);
	    	optionPositions.put(quote.getSymbol(), listOption);
	    }
    
		optionReturnOrder(true, quote.getSymbol(), " ", buy_sell , strategy );
		return;
	 }


    public void sellOption (Level1Quote quote, String symbol, Strategy strategy, String buy_sell) {	
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
    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
    		return;
    	}
    	
    	// get list of options to sell
    	List<OptionPosition> listOfOption;
    	listOfOption = optionPositions.get(quote.getSymbol());
    	
    	for (OptionPosition option : listOfOption) {

    	    Xprice = getExercisePricefromSymbol(option.getSymbol());
    	    quantity = option.getQuantity();
    	    double expirationTime = getExpirationTime(quote, option.getSymbol());
    	    if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
    	    	 Sprice = quote.getBid();
    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime,sigma);
    		     option.setPriceSold(optionPrice);
    		     log.info("Option symbol " + option.getSymbol());
    		     log.info("Time to epiration is " + expirationTime);
    		     log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
    		     log.info("delta of the option is " + optionQuote.getCallDelta(Sprice, Xprice, expirationTime, sigma));

    	    } else {
    	    	Sprice = quote.getAsk();
    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
    		    option.setPriceSold(optionPrice);
    		    log.info("Option symbol " + option.getSymbol());
    		    log.info("Time to epiration is " + expirationTime);
    		    log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
    		    log.info("delta of the option is " + optionQuote.getPutDelta(Sprice, Xprice, expirationTime, sigma));
    	    }
    	    double totalOptionPrice=optionPrice*option.getQuantity();
    	    
    	    double singleTradeCost= (optionTradeCost+0.15*(option.getQuantity()/100));
    	    option.updateCost(singleTradeCost);
    	    updateCash(totalOptionPrice, false, singleTradeCost);
    	}
    	
    	// get the list of trades to the dayTrades structure
    	int i = 0;
    	while (true) {
    		i++;
    		if (!dayTrades.containsKey(i)) {
    			dayTrades.put(i, listOfOption);
    			break;
    		}
    	}
    	
    	// clear the current position  for this symbol because they have been closed
    	optionPositions.remove(quote.getSymbol());
    	// reduce positions
    	positionOpen--;
    	optionReturnOrder(true, quote.getSymbol(), " ", buy_sell , strategy );
		return;
    	
    }

    
    public void optionReturnOrder(Boolean success, String symbol, String price, String buy_sell, Strategy strategy) {
    	
    	if (!success) {	  
  		  // if no success on filling the order tell the strategy to adjust and return
  		  strategy.strategyCallback(false, symbol, buy_sell, "0");	  
  		  return;
  	    }
    	strategy.strategyCallback(true, symbol, buy_sell, price);
    	
    	
    }
	
	@SuppressWarnings("deprecation")
	public String optionSymbolWeekly(Level1Quote quote, boolean buy, boolean inTheMoney) {
		
		/**
		  * These are the rules we follow to determine the option symbol:
		  * 
		  * 1) if price of the underlying is "close" to a strike price we take the first in the money strike for both put and calls.
		  * For price "close to strike" we mean that if we have 555.99 or 35.59 they will be treated as 555 and 35.50 and therefore for calls we will take 
		  * 552.5 and 35 while for puts we will take 557.5 and 36. Consider that if we have 35.60 we will take 36 for the puts but 35.5 for the calls....
		  * So the key difference between the two  cases if the first decimal digit.
		  * 
		  * 2) for other underlying prices we will take the first in the money calls or puts. For example for 35.20 we will take 35 for calls and 35.5 
		  * for puts. For 552 we will take  550 for calls strike and 552.5 for puts strike.
		  * 
		  * 3) If today is Friday or Thursday we will trade next week options to diminish the effect of time decay.
		  * 
		  * 4) if this the 3rd week of the month (or if next week is the the third week of the month if today is Friday or Thursday) we will trade the monthly option 
		  *    this only means  that the expiration day in the symbol is the Saturday of the week and not the Friday
		 **/
		
		String expiration;
		String callOrPut = "";
		String optionSymbol=""; // in the money option symbol
		double price;
		String symbol;
		int optionPrice; // this is the difference between the asset price and the remainder of modulo 5. 
    	int outOfTheMoneyOptionPrice; // first out of the money price
    	int inTheMoneyOptionPrice; // first in the money price
    	double doubleoutOfTheMoneyOptionPrice=0;
    	double doubleinTheMoneyOptionPrice=0;
		
		price = quote.getLast();
		if (buy) {
			callOrPut = "C";
			
		} else {
			callOrPut = "P";
			
		}
		
		symbol = quote.getSymbol();
		
		Date date = quote.getCurrentDateTime();
		int day = date.getDate();
		int month = date.getMonth();
		int year = date.getYear()+1900;
		
		//log.info("Date is " + date);
		//log.info(year + " " + month + " " + day);
		
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(year, month, day);
		
		Calendar first = (Calendar) rightNow.clone();
	    first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));
	    
	    // and add 5 days to get to Friday of the week
	    first.add(Calendar.DAY_OF_YEAR, 5);
	    
		//if ( (date.getDay() == 5) || (date.getDay() == 4) ) {
			
			// Thursday and Friday trade next week options.
			first.add(Calendar.DAY_OF_YEAR, 7);		    			
		//}
		
		int week = first.get(Calendar.WEEK_OF_MONTH);
		/*
		if (week == 3) {
			
		    	//log.info("this is the third week of the month " + week);
		    	// in this case the expiration is Saturday because we are trading  a monthly option and not a weekly option
		    	first.add(Calendar.DAY_OF_YEAR, 1);		    	
		}
		*/
				
	    log.info("Option Expiration time that we are using is:  " + first.getTime());
	    
	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
	    expiration = df.format(first.getTime());
	    
	    
	    if (price >= 150) {
	    		    	
	    	int k = (int) (price%5);	    	
	    	optionPrice = (int) price - k;
	    	//log.info("optionPrice is " + optionPrice);
	    	//log.info("k is " + k);
	    	
	     	if ( callOrPut == "P") {
	     		
	     	    // for puts  optionPrice is lower or equal to current underlying price (because it is  price-k1) 	     			     		
	     		
	     		if (k != 0) {
	     			
	     			// k should only be 1,2,3 or 4 
	     			
	     			if ( (k == 1) || (k ==2) ) {
	     				// this is the case of 531,532  or 536,537 etc...
	     				doubleinTheMoneyOptionPrice = optionPrice+2.5;
	    	     		doubleoutOfTheMoneyOptionPrice = optionPrice;	     				
	     			    
	     			} else if ( (k==3) || (k == 4) ) {
	     			    // this is the case of 533,534  or 538,539 etc...
	     				doubleoutOfTheMoneyOptionPrice=optionPrice+2.5;
	     			    doubleinTheMoneyOptionPrice=optionPrice+5;
	     			} else {
	     				// this should never happen
	     				log.info("Got into a situation where k is different that what expected for put. Price is: " + price);
	     				// not sure what to do here. Let's do this .....
	     				doubleinTheMoneyOptionPrice = optionPrice+2.5;
	    	     		doubleoutOfTheMoneyOptionPrice = optionPrice;
	     			}
	     			
	     			
	     		} else {
	     			// this is the case of 530 or 535
	     			doubleinTheMoneyOptionPrice=optionPrice+2.5;
	     			doubleoutOfTheMoneyOptionPrice = optionPrice;
	     		}     		
	     			
	     	} else {
	     		
	     	    // for calls optionPrice will  be the in the money price (being lower than asset price) except for the case when there is no remainder 
	     		// in this case we get the first one below. Consider that if we have something like 595.99 it will have a 0 remainder so the first 
	     		// call in the money strike price will be 592.50	     		
	     		if (k != 0) {
	     			
	     			// k should only be 1,2,3 or 4 
	     			if ( (k == 1) || (k ==2) ) {
	     				// this is the case of 531,532  or 536,537 etc...
	     				
	     				doubleoutOfTheMoneyOptionPrice=optionPrice+2.5;
	     			    doubleinTheMoneyOptionPrice=optionPrice;
	     			    
	     			} else if ( (k==3) || (k == 4) ) {
	     			    // this is the case of 533,534  or 538,539 etc...
	     				doubleoutOfTheMoneyOptionPrice=optionPrice+5;
	     			    doubleinTheMoneyOptionPrice=optionPrice+2.5;
	     			} else {
	     				// this should never happen
	     				log.info("Got into a situation where k is different that what expected for call. Price is: " + price);
	     				// not sure what to do here. Let's do this .....
	     				doubleoutOfTheMoneyOptionPrice=optionPrice+2.5;
	     			    doubleinTheMoneyOptionPrice=optionPrice;
	     			}
	     			
	     			
	     		} else {
	     			// this is the case of 530 or 535
	     			doubleinTheMoneyOptionPrice=optionPrice-2.5;
	     			doubleoutOfTheMoneyOptionPrice = optionPrice;
	     		}
	     	}
	     		     	
	    	optionSymbol=symbol+":"+expiration+":"+doubleinTheMoneyOptionPrice+":"+callOrPut;	    	
	    	this.optionSymbolInTheMoney=optionSymbol;
	    	this.optionSymbolOutoftheMoney = symbol+":"+expiration+":"+doubleoutOfTheMoneyOptionPrice+":"+callOrPut;	    	
	    	
	    } else {
	    	
	    	price = price *10;
	    	int k1 = (int) (price%5);
	    	optionPrice = (int) price - k1;
	    	//log.info("optionPrice is " + optionPrice);
	    	//log.info("k1 is " + k1);
	    	
	     	if ( callOrPut == "P") {
	     		
	     		// for puts because optionPrice is lower or equal to current underlying price (because it is  price-k1) it will be always the 
	     		// out of the money option while priceOption+5 will always be the in the money option strike.		     		
	     		outOfTheMoneyOptionPrice=optionPrice;
	     		inTheMoneyOptionPrice = optionPrice+5;	   	     			
	     	} else {
	     		
	     		// for calls optionPrice will  be the in the money price (being lower than asset price) except for the case when there is no remainder 
	     		// in this case we get the first one below. Consider that if we have something like 39.59 it will have a 0 remainder so the first 
	     		// call in the money will be 39     		
	     		if (k1 != 0) {
	     			outOfTheMoneyOptionPrice=optionPrice+5;
	     			inTheMoneyOptionPrice  = optionPrice;
	     			
	     		} else {
	     			outOfTheMoneyOptionPrice=optionPrice;
	     			inTheMoneyOptionPrice  = optionPrice-5;
	     			
	     		}
	     	}
	     	
	     	doubleinTheMoneyOptionPrice = (double) inTheMoneyOptionPrice/10D; // in the money price
	     	doubleoutOfTheMoneyOptionPrice = (double) outOfTheMoneyOptionPrice/10D; // out of the money price	
	    	
	    	// for cases like 39.0 the use of following code should transform it in 39 otherwise AMTD complains. 
	    	// Also cases like 39.5 should still be represented as such.
	    	DecimalFormat format = new DecimalFormat();	    	
	        format.setDecimalSeparatorAlwaysShown(false);
	    	
	    	optionSymbol=symbol+":"+expiration+":"+format.format(doubleinTheMoneyOptionPrice)+":"+callOrPut;
	    	this.optionSymbolInTheMoney=optionSymbol;
	    	this.optionSymbolOutoftheMoney = symbol+":"+expiration+":"+format.format(doubleoutOfTheMoneyOptionPrice)+":"+callOrPut;
	    }
	    				
		//log.info("Weekly symbol option IN the money is " + this.optionSymbolInTheMoney);
		//log.info("Weekly symbol option OUT of the money is " + this.optionSymbolOutoftheMoney);
		
		if (!inTheMoney) {
			
			log.info("Weekly Option Symbol returned is" + optionSymbolOutoftheMoney);
			return optionSymbolOutoftheMoney;
		}
		log.info("Weekly Option Symbol returned is" + optionSymbol);
		return optionSymbol;
		
	}
	
	@SuppressWarnings("deprecation")
	public double getExpirationTime(Level1Quote quote) {
		Date date=quote.getCurrentDateTime();
		
		int daysToExpiration;
		int hours = date.getHours();
		int day = date.getDay();
		//log.info("Date is " + date);
		//log.info(year + " " + month + " " + day);
		//log.info("day is " + date.getDay());
		// if trading next week option it should be 12 -day if trading same week 5-day.
     //  if ( (date.getDay() == 5) || (date.getDay() == 4) ) {
			
			// Thursday and Friday trade next week options
			// if trading next week option it should be 12 -day if trading same week 5-day..			
			 daysToExpiration = 12-day;	    			
		//} else {
			// daysToExpiration = 5-day;
			// daysToExpiration = 12-day;
		//}
       
       //log.info("days to expiration is  " + daysToExpiration);
       
		
		// fraction of day depends on the hours. Weekly option expire on Friday at the close of market so 
		// assume the following if 7 add 7/7 if 8 add 6/7 ....... if 13 add 1/7 to day
		// The above  is not right all the time however it is  right on Friday the last day of the week
		// so for simulation does not really matter that is not right the other days of the week because we do not 
		// hold position for days.
		
		double fractionOfDay;
		
		if (hours == 7) {
			fractionOfDay = 1.0;
		} else if (hours == 8) {
			fractionOfDay = 6.0/7.0;
		} else if (hours == 9) {
			fractionOfDay = 5.0/7.0;
		} else if (hours == 10) {
			fractionOfDay = 4.0/7.0;
		} else if (hours == 11) {
			fractionOfDay = 3.0/7.0;
		} else if (hours == 12) {
			fractionOfDay = 2.0/7.0;
		} else if (hours ==13) {
			fractionOfDay = 1.0/7.0;
		} else {
			fractionOfDay = 0.01;
		}
		
		double T1 = ((double)daysToExpiration +fractionOfDay)/365.0;
		//log.info( "T1 is " + T1);
		return T1;
	}
	
	public double getExercisePrice(Level1Quote quote, boolean buy, boolean inTheMoney) {
		

		String callOrPut = "";
		double price;
	
		if (buy) {
			callOrPut = "C";
		} else {
			callOrPut = "P";
		}
		
		price = quote.getLast();
	    
	    if (price >= 100) {
	    	int k = (int) (price%5);
	    	
	    	int optionPrice; // in the money price
	    	int optionPrice1; // first out of the money price
	    	optionPrice = (int) price - k;
	    	
	        if (callOrPut == "P") {
	     		optionPrice+=5;
	     	}
	        
	     	if (k==0) {
	     		optionPrice=(int)price;
	     	}
	    	
	     	if ( callOrPut == "P") {
	     		// out of the money calculation
	     		if ( k!= 0 ) {
	     			optionPrice1=optionPrice-5;
	     		} else {
	     			optionPrice1=optionPrice;
	     		}
	     			
	     	} else {
	     		//out of the money calculation
	     		if (k != 0) {
	     			optionPrice1=optionPrice+5;
	     		} else {
	     			optionPrice1=optionPrice;
	     		}
	     	}
	     	
	     	if (!inTheMoney) {
	     		
	     		return optionPrice1;
	     	}
	     	return optionPrice;    	
	    	
	    } else {
	    	price = price *10;
	    	int k1 = (int) (price%5);
	    	int optionPrice; 
	    	int optionPrice3; 
	    	optionPrice = (int) price - k1;
	    	
	    	if (callOrPut == "P") {
	     		optionPrice+=5;
	     	}
	     	if (k1==0) {
	     		optionPrice=(int)price;
	     	}
	     	
	     	if ( callOrPut == "P") {
	     		if ( k1!= 0 ) {
	     			optionPrice3=optionPrice-5;
	     		} else {
	     			optionPrice3=optionPrice;
	     		}
	     			
	     	} else {
	     		if (k1 != 0) {
	     			optionPrice3=optionPrice+5;
	     		} else {
	     			optionPrice3=optionPrice;
	     		}
	     	}
	    	double optionPrice1 = (double) optionPrice/10D; // in the money price
	    	double optionPrice4 = (double) optionPrice3/10D; // out of the money price
	    		    	
	    	if (!inTheMoney) {
	    		return optionPrice4;
	    	}
	    	return optionPrice1;
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
				  if (buy_sell.contentEquals("buy")) {
				      stock.updatePosition(lot_int, stockPrice);
				  } else {
					  
					  String s = strategy.getClass().getSimpleName();
					  if (s.contentEquals("MeanReversionStrategyNeq4")) {
						  stock.updatePosition(lot_int, stockPrice);
					  } else {
						  // This is used for delta adjustment when we sell a portion of the stocks
						  StockPosition deltaStock = new StockPosition(stock.getTypeofPosition(), stock.getSymbol(), lot_int, stock.getPriceBought());
						  deltaStock.closePosition(stockPrice);
						  historyPositions.add(deltaStock);
						  stock.decreaseQuantity(lot_int);
					  }
				  }
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
	 
	
	public void analyzeTrades(boolean display) {
		
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
	
	if ( this.tradeOption) {
		analyzeOptionTrades();
		return;
	}
		
	if (display){
		log.info("-------------------------------");
		log.info("Analyzing Trades.........");
		log.info("Analyzing Trades.........");
		log.info("Analyzing Trades.........");
		log.info("-------------------------------");
	}
	
	
		Iterator<StockPosition> iterator = this.historyPositions.iterator();
		
		
		while (iterator.hasNext()){
					
			    trade =  (StockPosition)iterator.next();
			    symbol=trade.getSymbol();
				quantity=trade.getQuantity();	
				priceBought = trade.getPriceBought(); 
				soldPrice=trade.getPriceSold();
				gain=(soldPrice-priceBought)*(double)trade.getQuantity();				
				numberOfTrades+=1;
			if (display) {
				log.info("These are the parameters of the trade: " + symbol + " " + quantity + " " 
				         + priceBought + " " + soldPrice + " " + gain);
			}
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
		this.totalGain = totalGain;
	if (display){
		log.info("Number of Total Trades: " + numberOfTrades + " " + " Number of Winning Trades: " + numberOfWinningTrades + 
				  " Number of Loosing Trades: " + numberofLoosingTrades ) ;
		log.info("Gain from Trades: " + currentGain + " Loss from Trades: " + currentLoss + " Total Gain/Loss: " + totalGain);
		log.info("Total Transaction Costs are :" + this.currentTradeCost);	 // totalCost should be the same of currentTadeCost	
	}	
	//analyzeOptionTrades();
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
		this.totalGain = totalGain;
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
			this.optionTradeCost = 3.5;
					
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
    	return 0;
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
    	/*
    	for (StockPosition stock : positions) {
			  if (stock.getSymbol().contentEquals(quote.getSymbol())) {
				  // assumes that we are long otherwise quantity should be negative for delta calculation
				  quantity = stock.getQuantity();
				  stock_value = quantity * quote.getBid() - stock.getPriceBought()*quantity ;
				  
			  }
		}
    	*/
		return( totalOptionPrice-pricePaid+stock_value);
	
		
	}
	
	public double getLifeGain() {
		return this.lifeGain;
	}
	
	public void updateMaxNumberOfPosition(int max_number) {
		this.max_number_of_positions=max_number;
	}
	
	public List<StockPosition> getStockPosition() {
		return historyPositions;
	}

	public void resetAcctMgr() {
		this.cash = 0;
		positions = new ArrayList<StockPosition>();	
	    historyPositions = new ArrayList<StockPosition>();
		optionPositions = new HashMap<String, List<OptionPosition>>();
		dayTrades = new HashMap<Integer, List<OptionPosition>>();
		currentTradeCost=0D;
		cash=0D;
		
		
	}
	public double getTotalGain() {
		return this.totalGain;
	}
	
	public boolean getTradeOption() {
		return this.tradeOption;
	}
	
	public Map<Integer, List<OptionPosition>> getDayTrades() {
		return this.dayTrades;
	}
	
	 public void sellOptionOneLeg (Level1Quote quote, String symbol, Strategy strategy, String buy_sell) {	
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
		    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
		    		return;
		    	}
		    	
		    	// get last option 
		    	List<OptionPosition> listOfOption;
		    	listOfOption = optionPositions.get(quote.getSymbol());
		    	
		    	OptionPosition option = listOfOption.get(listOfOption.size() -1);
		    	Xprice = getExercisePricefromSymbol(option.getSymbol());
	    	    quantity = option.getQuantity();
	    	     double expirationTime = getExpirationTime(quote, option.getSymbol());
	    	    if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
	    	    	 Sprice = quote.getBid();
	    	    	
	    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime,sigma);
	    		     option.setPriceSold(optionPrice);
	    		     
	    		     log.info("Option symbol " + option.getSymbol());
	    		     log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
	    		     log.info("delta of the option is " + optionQuote.getCallDelta(Sprice, Xprice, expirationTime, sigma));

	    	    } else {
	    	    	Sprice = quote.getAsk();
	    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
	    		    option.setPriceSold(optionPrice);
	    		    log.info("Option symbol " + option.getSymbol());
	    		    log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
	    		    log.info("delta of the option is " + optionQuote.getPutDelta(Sprice, Xprice, expirationTime, sigma));
	    	    }
	    	    double totalOptionPrice=optionPrice*option.getQuantity();
	    	    
	    	    double singleTradeCost= (optionTradeCost+0.15*(option.getQuantity()/100));
	    	    option.updateCost(singleTradeCost);
	    	    updateCash(totalOptionPrice, false, singleTradeCost);
	    	    
	    	    List<OptionPosition> listOfOption1 = new ArrayList<OptionPosition>();
	    	    listOfOption1.add(option);
		    	// get the list of trades to the dayTrades structure
		    	int i = 0;
		    	while (true) {
		    		i++;
		    		if (!dayTrades.containsKey(i)) {
		    			dayTrades.put(i, listOfOption1);
		    			break;
		    		}
		    	}
		    	
		    	
		    	listOfOption.remove(listOfOption.size() -1);
		    	optionReturnOrderOneLeg(true, quote.getSymbol(), " ", buy_sell , strategy );
				return;
		    	
		    }

	 public void sellDeltaAdjustment (Level1Quote quote, String symbol, Strategy strategy, String buy_sell, int quantity, String call_or_put) {	
			// we have to sell quantity associated with symbol

		    	double  optionPrice;

		    	log.info("Entering sellDeltaAdjustment method for symbol " + symbol);
		    	log.info("Date is " + quote.getCurrentDateTime());
		    	double delta = calculatePortfolioDelta(quote,strategy);
		    	log.info("Portfolio Delta is " + delta);
		    	double Sprice = quote.getLast();
		    	double Xprice=0;
		    	//double expirationTime = getExpirationTime(quote);
		    	OptionCostProvider optionQuote = new OptionCostProvider();

		    	double sigma = strategy.getImpVol();
		    	
		    	if (!optionPositions.containsKey(quote.getSymbol())) {
		    		log.info( " Something is asking to sell positions that we do not have ");
		    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
		    		return;
		    	}
		    	
		    	// get the option 
		    	OptionPosition option= null;
		    	for (OptionPosition o : optionPositions.get(quote.getSymbol())) {
		    		
		    		Xprice = getExercisePricefromSymbol(o.getSymbol());
		    		
		    		if (getCallOrPutfromSymbol(o.getSymbol()).equals(call_or_put))  {
		    			option = o;
		    			//break;
		    		}
		    	}	
		    	
		    	if (option == null) {
		    		log.info("No option found in Account Mgr");
		    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
		    		return;
		    	}
		    	
		    	Xprice = getExercisePricefromSymbol(option.getSymbol());
		    	double expirationTime = getExpirationTime(quote, option.getSymbol());
	    	    if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
	    	    	 Sprice = quote.getBid();
	    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime,sigma);
	    		     //option.setPriceSold(optionPrice);
	    		     log.info("Option symbol " + option.getSymbol());
	    		     log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
	    		     log.info("Time to epiration is " + expirationTime);
	    		     log.info("delta of the option is " + optionQuote.getCallDelta(Sprice, Xprice, expirationTime, sigma));

	    	    } else {
	    	    	Sprice = quote.getAsk();
	    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
	    		    //option.setPriceSold(optionPrice);
	    		    log.info("Option symbol " + option.getSymbol());
	    		    log.info("Sell price is " + optionPrice + " Quntity is " + quantity);
	    		    log.info("Time to epiration is " + expirationTime);
	    		    log.info("delta of the option is " + optionQuote.getPutDelta(Sprice, Xprice, expirationTime, sigma));
	    	    }
	    	    double totalOptionPrice=optionPrice*quantity;
	    	    
	    	    double singleTradeCost= (optionTradeCost+0.15*(quantity/100));
	    	   // option.updateCost(singleTradeCost);
	    	    option.updateQuantity(-quantity);
	    	    updateCash(totalOptionPrice, false, singleTradeCost);
	    	    	    	    
	    	    List<OptionPosition> listOfOption1 = new ArrayList<OptionPosition>();
	    	    // create an option trade
	    	    OptionPosition newOption = new OptionPosition(option.getSymbol(), option.getPriceBought(), quantity, singleTradeCost );
	    	    newOption.setPriceSold(optionPrice);
	    	    listOfOption1.add(newOption);
		    	// get the list of trades to the dayTrades structure
		    	int i = 0;
		    	while (true) {
		    		i++;
		    		if (!dayTrades.containsKey(i)) {
		    			dayTrades.put(i, listOfOption1);
		    			break;
		    		}
		    	}
		    	if (option.getQuantity() == 0) {
	    	    	// remove from positions
		    		log.info("Removing option from positions " + option.getSymbol());
	    	    	optionPositions.get(quote.getSymbol()).remove(option);
	    	    }
		    	
		    	optionReturnOrderOneLeg(true, quote.getSymbol(), " ", buy_sell , strategy );
				return;
		    	
		    }
	 
	 
	 public void optionReturnOrderOneLeg(Boolean success, String symbol, String price, String buy_sell, Strategy strategy) {
	    	
	    	if (!success) {	  
	  		  // if no success on filling the order tell the strategy to adjust and return
	  		  strategy.strategyCallback(false, symbol, buy_sell, "0");	  
	  		  return;
	  	    }
	    	strategy.strategyCallback(true, symbol, buy_sell, price);
	    	
	    	
	    }
	 
		public double calculatePortfolioDelta(Level1Quote quote, Strategy strategy) {
			
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
			int quantity = 0;
			for (StockPosition stock : positions) {
				  if (stock.getSymbol().contentEquals(quote.getSymbol())) {
					  // assumes that we are long otherwise quantity should be negative for delta calculation
					  quantity = stock.getQuantity();
				  }
			}
			return delta+quantity;
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
        
        
        public void buyDeltaAdjustment (Level1Quote quote, String symbol, Strategy strategy, String buy_sell, int quantity, String call_or_put) {
        	
	    	double  optionPrice;

	    	log.info("Entering buyDeltaAdjustment method for symbol " + symbol);
	    	log.info("Date is " + quote.getCurrentDateTime());
	    	log.info("Portfolio Delta is " + calculatePortfolioDelta(quote,strategy));
	    	double Sprice = quote.getLast();
	    	double Xprice=0;
	    	double expirationTime;
	    	OptionCostProvider optionQuote = new OptionCostProvider();

	    	double sigma = strategy.getImpVol();
	    	
	    	if (!optionPositions.containsKey(quote.getSymbol())) {
	    		log.info( " Something is asking to sell positions that we do not have ");
	    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
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
	    		optionReturnOrder(false, quote.getSymbol(), " ", buy_sell, strategy );
	    		return;
	    	}
	    	
	    	Xprice = getExercisePricefromSymbol(option.getSymbol());
	    	expirationTime = getExpirationTime(quote, option.getSymbol());
    	    if (getCallOrPutfromSymbol(option.getSymbol()).equals("C")) {
    	    	 Sprice = quote.getBid();
    		     optionPrice = optionQuote.getCallCost(Sprice, Xprice, expirationTime,sigma);
    		     //option.setPriceSold(optionPrice);
    		     log.info("Option symbol " + option.getSymbol());
    		     log.info("Buy price is " + optionPrice + " Quantity is " + quantity);
    		     log.info("delta of the option is " + optionQuote.getCallDelta(Sprice, Xprice, expirationTime, sigma));

    	    } else {
    	    	Sprice = quote.getAsk();
    		    optionPrice = optionQuote.getPutCost(Sprice, Xprice, expirationTime,sigma);
    		    //option.setPriceSold(optionPrice);
    		    log.info("Option symbol " + option.getSymbol());
    		    log.info("Sell price is " + optionPrice + " Quantity is " + quantity);
    		    log.info("delta of the option is " + optionQuote.getPutDelta(Sprice, Xprice, expirationTime, sigma));
    	    }
    	    double totalOptionPrice=optionPrice*quantity;
    	    
    	    double singleTradeCost= (optionTradeCost+0.15*(quantity/100));
    	   // option.updateCost(singleTradeCost);
    	    // update price bought
    	    option.priceBought = (option.priceBought*option.getQuantity() + optionPrice*quantity)/(option.getQuantity() + quantity);   	   
    	    option.updateQuantity(quantity);
    	    updateCash(totalOptionPrice, true, singleTradeCost);
    	    optionReturnOrderOneLeg(true, quote.getSymbol(), " ", buy_sell , strategy );
        	
        }
        
        public void buyPutOption (Level1Quote quote, int lot, boolean call_put, Strategy strategy, boolean inTheMoney, String buy_sell) {	
    		
    		String optionSymbol="";
    		double optionPrice;
    		double Xprice;
    		double Sprice;
    		double T1;
    		  		
    		Sprice = quote.getLast();
    		optionSymbol=getOption(quote,call_put, inTheMoney);
    		Xprice = getExercisePricefromSymbol(optionSymbol);
    		T1 = getExpirationTime(quote);	
    		T1 = getExpirationTime(quote, optionSymbol);
    		OptionCostProvider ocp = new OptionCostProvider();
    		double sigma = strategy.getImpVol();
    		
    		if (call_put) {
    			Sprice = quote.getAsk();
    			optionPrice = ocp.getCallCost(Sprice,Xprice,T1,sigma);
    			log.info("Option symbol " + optionSymbol);
    			log.info("Buy price is " + optionPrice + " Quantity is " + lot);
    			log.info("delta of the option is " + ocp.getCallDelta(Sprice, Xprice, T1, sigma));
    			
    		} else {
    			Sprice = quote.getBid();
    			optionPrice = ocp.getPutCost(Sprice,Xprice,T1,sigma);
    			log.info("Option symbol " + optionSymbol);
    			log.info("Buy price is " + optionPrice);
    			log.info("Buy price is " + optionPrice + " Quantity is " + lot);
    			log.info("delta of the option is " +  ocp.getPutDelta(Sprice, Xprice, T1, sigma));
    			
    		}
    		// When buying simulate bid-ask spread .....
    		
    		if (optionPrice<3.0) {
    			optionPrice=optionPrice+0.01;
    		} else {
    			optionPrice=optionPrice+0.1;
    		}
    		
    		double totalOptionPrice=optionPrice*lot;	    
    	    double singleTradeCost= (optionTradeCost+0.15*(lot/100)); 
    	    updateCash(totalOptionPrice, true, singleTradeCost);
    	    OptionPosition option = new OptionPosition(optionSymbol, optionPrice, lot, singleTradeCost);
    		 
    		// add to positions
    	    if (optionPositions.containsKey(quote.getSymbol())) {
    	    	// symbol already exists in the map
    	    	optionPositions.get(quote.getSymbol()).add(option);
    	    } else {
    	    	List<OptionPosition> listOption = new ArrayList<OptionPosition>();
    	    	listOption.add(option);
    	    	optionPositions.put(quote.getSymbol(), listOption);
    	    }
        
    		optionReturnOrder(true, quote.getSymbol(), " ", buy_sell , strategy );
    		return;
    	 }
        
    	
    	public String getOption(Level1Quote quote, boolean buy, boolean inTheMoney) {
    		
    		
    		String expiration;
    		String callOrPut = "";
    		String optionSymbol=""; // in the money option symbol
    		double price;
    		String symbol;
    		   		
    		price = quote.getLast();
    		
    		if (buy) {
    			callOrPut = "C";
    			//price = quote.getLast()+(0.02*quote.getLast());
    			//price = quote.getLast();
    			price = quote.getBid() + 3;
    		} else {
    			callOrPut = "P";
    			//price = quote.getLast()-(0.02*quote.getLast());
    			price = quote.getBid() - 3;
    			//price = quote.getLast();
    			
    		}
    		
    		int intPrice = (int) Math.round(price);
    		symbol = quote.getSymbol();
    		
    		Date date = quote.getCurrentDateTime();
    		Calendar rightNow = Calendar.getInstance();
    		rightNow.setTime(date);
    		   		
    		Calendar first = (Calendar) rightNow.clone();
    	    first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));
    	    
    	    // and add 5 days to get to Friday of the week
    	    first.add(Calendar.DAY_OF_YEAR, 5);
    	    // Add one month
    	    first.add(Calendar.MONTH, 1);
    				
    	    log.info("Option Expiration time that we are using is:  " + first.getTime());
    	    
    	    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
    	    expiration = df.format(first.getTime());
    	    
    	    optionSymbol=symbol+":"+expiration+":"+intPrice+":"+callOrPut;
    	    
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
    	
    	public void buy_or_sell_delta (String buy_sell, String open_close_update, Level1Quote quote, int lot, Strategy strategy) {
    		
    		log.info("Entering buy_or_sell_delta method" + " buy_sell is " + buy_sell + " positionOpen is " + positionOpen);
    		log.info("Date is " + quote.getCurrentDateTime());
	    	log.info("Portfolio Delta is " + calculatePortfolioDelta(quote,strategy));
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
}
