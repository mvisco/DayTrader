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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.TradeMonster.CancelOrderTM;
import com.TraderLight.DayTrader.TradeMonster.CreateStockOrderTM;
import com.TraderLight.DayTrader.TradeMonster.GetQuoteTM;
import com.TraderLight.DayTrader.TradeMonster.RequestOrderStatusTM;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.Strategy.Strategy;


/**
 *  This class manages a single order. It gets called from  AccountMgr and executes in its own thread.
 *  When the order processing is terminated, the thread terminates abd an asynchronous call to AccountMgr is performed to let it know 
 *  if the order was placed successfully or not.
 * 
 * @author Mario Visco
 *
 */


public class GenericOrderExecutorTM implements Runnable {
	
	public static final Logger log = Logging.getLogger(true);
	public String symbol;
	public String optionSymbol; // this is the AccountMgr option symbol
	public String optionSymbolTM; // this is the option symbol as requested by TM
	public String optionPrice;
	public String lot;
	public String quantity;
	AccountMgr account;
	public double bid;
	public double ask;
	public boolean buy;
	double optionBidAskSpread;
	Strategy strategy;
	int i=0;
	int leg;
	boolean closeTransaction; // if true we sell at the bid and buy at the ask otherwise we will try to close transaction at better price
	boolean spread_trading=false;
	String shortOptionSymbol="";
	String longOptionSymbol= "";
	String shortOptionSymbolTM="";
	String longOptionSymbolTM= "";
	boolean open_close=false;
	double shortStrike=0;
	double longStrike = 0;
	boolean call_put=false;
	boolean mock = true;
	boolean trade_option = false;
	String spread_price = "";
	String underlying_price = "";
	boolean stock_trading = false;
	String buy_sell = "";
	String open_close_update = "";
	String stockPrice = "";
	String sourceApp;
	
	public GenericOrderExecutorTM(String buy_sell, String open_close_update, String symbol, String price, int lot, AccountMgr account, 
			Strategy strategy) {
		//  constructor for stocks
		this.stock_trading=true;
		this.buy_sell = buy_sell;
		this.open_close_update = open_close_update;
		this.symbol = symbol;
		this.stockPrice = price;
		this.quantity= Integer.toString(lot); // We will use this parameter to return the number of options (for ex. 100, 200 etc) as a string in callbacks
		this.lot=Integer.toString(lot);
		this.account = account;
		this.strategy=strategy;	
		
	}
	
	public GenericOrderExecutorTM(String buy_sell, String open_close_update, String optionSymbol, String symbol, String price, int lot, AccountMgr account, 
			Strategy strategy, boolean mock, boolean trade_option) {
		//  constructor for options
		this.stock_trading=false;
		this.buy_sell = buy_sell;
		this.open_close_update = open_close_update;
		this.optionSymbol = optionSymbol;
		this.symbol = symbol;
		this.stockPrice = price;
		this.quantity= Integer.toString(lot); // We will use this parameter to return the number of options (for ex. 100, 200 etc) as a string in callbacks
		this.lot=Integer.toString(lot);
		this.account = account;
		this.strategy=strategy;	
		this.mock = mock;
		this.trade_option = true;
	}

	@Override
	public void run() {
				
		log.info("Entering Order Executor TM");
		log.info("Thread ID is: " + Thread.currentThread().getId());
		
		if (!trade_option) {
		   stockTrade();
		} else {
			optionTrade();
		}
		return;	
	}
	
	public void stockTrade() {
		//Trading stock method
		
		CreateStockOrderTM order = new CreateStockOrderTM();
		boolean open_close;
		if (this.open_close_update.contains("open") || this.open_close_update.contains("update") ){
			open_close=true;
		} else {
			open_close=false;
		}
		
		if (this.buy_sell.contentEquals("buy") ){
			buy=true;
		} else {
			buy=false;
		}
		
		String ord_type;
		// just operate at market for all orders
		ord_type="market";
		/*
		if (open_close) {
			ord_type="limit";
		} else {
			ord_type="market";
		}
		*/
		try {
			order.createOrder(buy,lot,symbol,stockPrice,ord_type,open_close);
		} catch (Exception e) {
			log.info("Order could not be created");			
			e.printStackTrace();
			// return failure so the strategy will go back to previous state
			account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
			return;
		}
		String order_id = order.getOrderID();
		
		log.info("order_id is " + order_id);
		
		
		if (order_id.contentEquals("")) {
			// Something went wrong we got an empty string. So return failure to account manager
			log.info("order_id is an empty string");
			account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
			return;
			
		}
		
		checkStockOrderCompletion(order_id,1);
		
	}
	
	public void checkStockOrderCompletion(String order_id, int wait_time_minutes) {
		// we place stock order at market so they should be filled right away
		// wait for 10 seconds before checking the order status
		try {
			Thread.currentThread();
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			log.info("Something went wrong in setting the 10 sec sleep");
			//keep going 
			e1.printStackTrace();
		}
		RequestOrderStatusTM order_status = new RequestOrderStatusTM();
		String status = "";
		try {
			status = order_status.requestOrderStatus(order_id);
		} catch (Exception e) {
			log.info("Cannot request status on order_id " + order_id);
			//keep going
			e.printStackTrace();
		}
		
		if (status.contentEquals("Filled")) {
			//order has been filled. return Success
			
			account.returnOrderParameters(true, this.symbol, order_status.getAverageFilledPrice(), order_status.getFilledQuantity(), this.buy_sell, this.strategy, this.open_close_update);
			return;
			
		}
		
		int wait_milliseconds = 60000*wait_time_minutes;
		int sleepTime = 60000;
		int iterationLoop = wait_milliseconds/sleepTime;  
		
		for (int i = 1; i<= iterationLoop; i++ ) {
		
		    try {
			    Thread.sleep(sleepTime);
			
		    } catch (InterruptedException e1) {
			    log.info("Something went wrong in setting the 60 seconds sleep");
			    e1.printStackTrace();
		    }
		
		    try {
			    status = order_status.requestOrderStatus(order_id);
		    } catch (Exception e) {
			    log.info("Cannot request status on order_id " + order_id);
			    e.printStackTrace();
		    }
		
		    if (status.contentEquals("Filled" )) {
			   //order has been filled. return Success 
		    	account.returnOrderParameters(true, this.symbol, order_status.getAverageFilledPrice(), order_status.getFilledQuantity(), this.buy_sell, this.strategy, this.open_close_update);
			   return;
			
		    }
		    
		    if (i==iterationLoop) {
				//  This is the last time we check for order status. It is possible that the order has been partially filled
		    			    	
		    	if (status.contentEquals("Open" )) {
		    	    log.info("Filled Quantity is " + order_status.getFilledQuantity());
		    	    
		    	    if (!order_status.getFilledQuantity().contentEquals("0")) {
		    	       // int partialQuantity = Integer.parseInt((order_status.getFilledQuantity()))*100;		    	    
					   // String pQuantity = Integer.toString(partialQuantity);
					
					    // The order has been partially filled so cancel it and return the filled quantity back to Account Manager
					    // if close transaction is not set
					    
					        CancelOrderTM cancel = new CancelOrderTM();
					        try {
						       cancel.cancelOrder(order_id);
					        } catch (Exception e) {
						       //cannot cancel the order, exit program there are problems. If we do not exit we may have loops of posting orders
						       // because the strategy will continue to ask to buy or sell when it goes into the previous state
						      log.info("Cannot cancel the order......check manually to see what happened");
						      log.info("Exiting program..........");
						      e.printStackTrace();
						      System.exit(0);
					        }
					        // Wait for 10 seconds and make sure that the order is cancelled
					       try {
						    Thread.sleep(10000);
						
					       } catch (InterruptedException e1) {
						       log.info("Something went wrong in setting the 10 seconds sleep");
						        e1.printStackTrace();
					       }
					      try {
						       status = order_status.requestOrderStatus(order_id);
						       log.info("Status is " + status);
					       } catch (Exception e) {
						      log.info("Cannot request status on order_id " + order_id);
						      e.printStackTrace();
					       }
					       // There may be a race condition for which the order has been filled anyway even if we tried to cancel if so return success
					   
					       if (status.contentEquals("Filled" )) {				
					    	   account.returnOrderParameters(true, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
							   return;
					        }
					       account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
						   return;
		    	    }
		    	    // no partial fill so cancel the order
		    	    CancelOrderTM cancel = new CancelOrderTM();
			        try {
				       cancel.cancelOrder(order_id);
			        } catch (Exception e) {
				       //cannot cancel the order, exit program there are problems. If we do not exit we may have loops of posting orders
				       // because the strategy will continue to ask to buy or sell when it goes into the previous state
				      log.info("Cannot cancel the order......check manually to see what happened");
				      log.info("Exiting program..........");
				      e.printStackTrace();
				      System.exit(0);
			        }
			        // Wait for 10 seconds and make sure that the order is cancelled
			       try {
				    Thread.sleep(10000);
				
			       } catch (InterruptedException e1) {
				       log.info("Something went wrong in setting the 10 seconds sleep");
				        e1.printStackTrace();
			       }
			      try {
				       status = order_status.requestOrderStatus(order_id);
				       log.info("Status is " + status);
			       } catch (Exception e) {
				      log.info("Cannot request status on order_id " + order_id);
				      e.printStackTrace();
			       }
			       // There may be a race condition for which the order has been filled anyway even if we tried to cancel if so return success
			   
			       if (status.contentEquals("Filled" )) {				
			    	   account.returnOrderParameters(true, this.symbol, order_status.getAverageFilledPrice(), order_status.getFilledQuantity(), this.buy_sell, this.strategy, this.open_close_update);
					   return;
			        }
			        account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
					return;
					
		    	} else if (status.contentEquals("Queued" ) )  {
		    		// if we are here just cancel this order, because the order has been queued and it will never be filled
		    		 CancelOrderTM cancel = new CancelOrderTM();
				      try {
					       cancel.cancelOrder(order_id);
				      } catch (Exception e) {
					       //cannot cancel the order, exit program there are problems. If we do not exit we may have loops of posting orders
					       // because the strategy will continue to ask to buy or sell when it goes into the previous state
					      log.info("Cannot cancel the order......check manually to see what happened");
					      log.info("Exiting program..........");
					      e.printStackTrace();
					      System.exit(0);
				     }
		    		 account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
		    		 return;
		    	} else if (status.contentEquals("Rejected" ) ) {
		    		// Nothing to cancel here just return failure
		    		account.returnOrderParameters(false, this.symbol, this.stockPrice, this.quantity, this.buy_sell, this.strategy, this.open_close_update);
		    		
		    	}
		    	
		    } 
		}
		
		
	}	
	
	public void optionTrade() {
		
		log.info("Entering optionTrade()");
		optionSymbolTM = convertOptionSymbol(optionSymbol);
		
		// Get the quote now
        GetQuoteTM optionQuote = new GetQuoteTM();
		
		try { 
			optionQuote.getQuotes(optionSymbolTM,buy,closeTransaction);
			
			if (this.buy_sell.contentEquals("buy") ){
				optionPrice = optionQuote.getAsk();
			} else {
				optionPrice = optionQuote.getBid();
			}
			
		} catch (Exception e) {
			log.info("Something went wrong with getting quote for symbol " + optionSymbolTM);
			e.printStackTrace();
			// What's to do here ??  let's just  return failure to the account manager 	
			account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy,"0" );
			return;
		}
		
		if (String.valueOf(optionPrice).isEmpty()) {
			// We get here if something went wrong with getting the quote
			account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy,"0" );
			return;
		}
		log.info("Option Price is " + optionPrice);	
		if (mock) {
			// do not place order just simulate success			
			account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy,"0");
			return;
		}
	
/*
        String price_type="";
		price_type="limit";
		CreateOptionOrderTM order = new CreateOptionOrderTM();		
		try {
			order.createOrder(buy,lot,optionSymbolTM,optionPrice,price_type);
		} catch (Exception e) {
			log.info("Order could not be created");			
			e.printStackTrace();
			// return failure so the strategy will go back to previous state
			account.returnOrderParameters(false, this.symbol, this.optionSymbol, this.optionPrice, this.quantity, this.buy, this.i, this.strategy, this.leg);
			return;
		}
		String order_id = order.getOrderID();
		
		log.info("order_id is " + order_id);
		
		
		if (order_id.contentEquals("")) {
			// Something went wrong we got an empty string. So return failure to account manager
			log.info("order_id is an empty string");
			account.returnOrderParameters(false, this.symbol, this.optionSymbol, this.optionPrice, this.quantity, this.buy, this.i, this.strategy, this.leg);
			return;
			
		}
		
		checkOrderCompletion(order_id,5);
*/		
	}
		
	public String convertOptionSymbol(String acct_mgr_option) {
		
		Map<String, String> month_for_call = new HashMap<String,String>();
		Map<String, String> month_for_put = new HashMap<String,String>();
		
		month_for_call.put("01", "A");
		month_for_call.put("02", "B");
		month_for_call.put("03", "C");
		month_for_call.put("04", "D");
		month_for_call.put("05", "E");
		month_for_call.put("06", "F");
		month_for_call.put("07", "G");
		month_for_call.put("08", "H");
		month_for_call.put("09", "I");
		month_for_call.put("10", "J");
		month_for_call.put("11", "K");
		month_for_call.put("12", "L");
		
		month_for_put.put("01", "M");
		month_for_put.put("02", "N");
		month_for_put.put("03", "O");
		month_for_put.put("04", "P");
		month_for_put.put("05", "Q");
		month_for_put.put("06", "R");
		month_for_put.put("07", "S");
		month_for_put.put("08", "T");
		month_for_put.put("09", "U");
		month_for_put.put("10", "V");
		month_for_put.put("11", "W");
		month_for_put.put("12", "X");
		
		
		
		String[] symbolSplit = acct_mgr_option.split(":");
		
		// Get the month from the date
		String month = symbolSplit[1].substring(4, 6);
		//log.info("month " + month);
		
		if (symbolSplit[3].contentEquals("C")) {
			month = month_for_call.get(month);
		} else {
			month = month_for_put.get(month);
		}
		
		String day = symbolSplit[1].substring(6, 8);
		String year = symbolSplit[1].substring(2, 4);
		
		double price = Double.parseDouble(symbolSplit[2]);
		
		boolean add_zero_upfront=false;
		
		if (price < 100) {
			add_zero_upfront = true;
		}
		
		if (price >= 1000) {
			price = price *100;
		} else {
		    price = price *1000;
		}
		Long priceL = Math.round(price);
		String priceS = Long.toString(priceL);
						
		if (add_zero_upfront) {
			StringBuilder str = new StringBuilder(priceS).insert(0, "0");
			priceS = str.toString();
		}
		
		
		String optionTM=symbolSplit[0]+month+day+year+"C"+priceS;		
		log.info("optionTM is " + optionTM);
		return optionTM;
		
	}	
	
	
}


