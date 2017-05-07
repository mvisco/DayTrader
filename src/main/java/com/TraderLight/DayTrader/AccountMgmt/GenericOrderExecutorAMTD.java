package com.TraderLight.DayTrader.AccountMgmt;

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



import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;



import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.TradeMonster.CancelOrderTM;
import com.TraderLight.DayTrader.TradeMonster.CreateStockOrderTM;
import com.TraderLight.DayTrader.TradeMonster.RequestOrderStatusTM;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.Strategy.Strategy;
import com.TraderLight.DayTrader.Ameritrade.GetQuoteAmeritrade;
import com.TraderLight.DayTrader.Ameritrade.CancelOrder;
import com.TraderLight.DayTrader.Ameritrade.CreateOptionOrder;
import com.TraderLight.DayTrader.Ameritrade.OrderStatus;


/**
 *  This class manages a single order. It gets called from  AccountMgr and executes in its own thread.
 *  When the order processing is terminated, the thread terminates and an asynchronous call to AccountMgr is performed to let it know 
 *  if the order was placed successfully or not.
 * 
 * @author Mario Visco
 *
 */


public class GenericOrderExecutorAMTD implements Runnable {
	
	public static final Logger log = Logging.getLogger(true);
	public String symbol;
	public String optionSymbol; // this is the AccountMgr option symbol
	public String optionSymbolTM; // this is the option symbol as requested by TM
	public String optionSymbolAMTD; // this is the option symbol as requested by AMTD
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
	
	public GenericOrderExecutorAMTD(String buy_sell, String open_close_update, String symbol, String price, int lot, AccountMgr account, 
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
	
	public GenericOrderExecutorAMTD(String buy_sell, String open_close_update, String optionSymbol, String symbol, String price, int lot, AccountMgr account, 
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
		// first we need to convert the option symbol in the symbol used by AMTD
		// for example we need to convert AAPL:20130419:395:C in AAPL_041913C395
		// second we need to get a quote 
		// third we need to place the order
		log.info("Thread ID is: " + Thread.currentThread().getId());
		String[] symbolSplit = optionSymbol.split(":");

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

		optionSymbolAMTD = symbolSplit[0]+"_"+expiration+symbolSplit[3]+symbolSplit[2]; 
		String iv = "";		
		
		// Get the quote now
		 GetQuoteAmeritrade optionQuote = new GetQuoteAmeritrade();
		
		try { 
			
			optionPrice = optionQuote.getQuotes(optionSymbolAMTD,buy,closeTransaction,optionBidAskSpread);
			iv = optionQuote.getIv();
			/*
			if (this.buy_sell.contentEquals("buy") ){
				optionPrice = optionQuote.getAsk();
			} else {
				optionPrice = optionQuote.getBid();
			}
			*/
		} catch (Exception e) {
			log.info("Something went wrong with getting quote for symbol " + optionSymbolTM);
			e.printStackTrace();
			// What's to do here ??  let's just  return failure to the account manager 	
			account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy, iv, stockPrice );
			return;
		}
		
		if (String.valueOf(optionPrice).isEmpty()) {
			// We get here if something went wrong with getting the quote
			account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy, iv, stockPrice );
			return;
		}
		log.info("Option Price is " + optionPrice);	
		if (mock) {
			// do not place order just simulate success			
			account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
			return;
		}
	
		String price_type="limit";
		
		CreateOptionOrder order = new CreateOptionOrder();		
		try {
			order.createOrder(buy,lot,optionSymbolAMTD,optionPrice,price_type);
		} catch (Exception e) {
			log.info("Order could not be created");			
			e.printStackTrace();
			// return failure so the strategy will go back to previous state
			account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy, iv, stockPrice );
			return;
		}
		String order_id = order.getOrderID();
		
		log.info("order_id is " + order_id);
		
		/*
		if (order_id.contentEquals("")) {
			// Something went wrong we got an empty string. So return failure to account manager
			log.info("order_id is an empty string");
			account.returnOrderParameters(false, this.symbol, this.optionSymbol, this.optionPrice, this.quantity, this.buy, this.i, this.strategy);
			return;
			
		}
		*/
		
		// wait for 20 seconds before checking the order status
		try {
			Thread.currentThread();
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			log.info("Something went wrong in setting the 10 sec sleep");
			//keep going 
			e1.printStackTrace();
		}
		
		// check order status
		
		OrderStatus order_status = new OrderStatus();
		String status = "";
		try {
			status = order_status.requestStatus(order_id);
		} catch (Exception e) {
			log.info("Cannot request status on order_id " + order_id);
			//keep going
			e.printStackTrace();
		}
		
		if (status.contentEquals("Filled")) {
			//order has been filled. return Success 
			account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
			return;
			
		} 
		
		// if we are here it means that the order has not been filled yet so the question is what to do next ?
		// let's wait  a total of 5 minutes checking every 50 seconds to see if the order has been filled. 
		// We are waiting to see if the market comes to us....... 
		
		int fiveMinutes = 60000*5;
		int sleepTime = 50000;
		int iterationLoop = fiveMinutes/sleepTime;  //  this is 6
		
		for (int i = 1; i<= iterationLoop; i++ ) {
		
		    try {
			    Thread.sleep(sleepTime);
			
		    } catch (InterruptedException e1) {
			    log.info("Something went wrong in setting the 10 seconds sleep");
			    e1.printStackTrace();
		    }
		
		    try {
			    status = order_status.requestStatus(order_id);
		    } catch (Exception e) {
			    log.info("Cannot request status on order_id " + order_id);
			    e.printStackTrace();
		    }
		
		    if (status.contentEquals("Filled" )) {
			   //order has been filled. return Success 
		    	account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
			   return;
			
		    }
		    
		    // We have to deal with partial fills....
		    
		    if (i==iterationLoop) {
				//  This is the last time we check for order status. It is possible that the order has been partially filled
		    	int q = 0;
		    	 if (status.contentEquals("Open" )) {
		    		 try {
						 q = order_status.checkPartialFill(order_id);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    		if (q > 0 ) {
		    			// order has been partially filled, so the following is the processing for partial fills
		    			String pQuantity = Integer.toString(q);
		    			 if (!closeTransaction){
		    				 
		    				 CancelOrder cancel = new CancelOrder();
		    				   try {
		    					   cancel.requestOrderCancel(order_id);
		    				   } catch (Exception e) {
		    					//cannot cancel the order, exit program there are problems. If we do not exit we may have loops of posting orders
		    					// because the strategy will continue to ask to buy or sell when it goes into the previous state
		    					log.info("Cannot cancel the order......check manually to see what happened");
		    					log.info("Exiting program..........");
		    					e.printStackTrace();
		    					System.exit(0);
		    				   }
		    				   
		    				   try {
								    Thread.sleep(10000);
								
							       } catch (InterruptedException e1) {
								       log.info("Something went wrong in setting the 10 seconds sleep");
								        e1.printStackTrace();
							       }
		    				   try {
		    					    status = order_status.requestStatus(order_id);
		    				    } catch (Exception e) {
		    					    log.info("Cannot request status on order_id " + order_id);
		    					    e.printStackTrace();
		    				    }
		    				
		    				   // race condition we tried to cancel but the order has been filled
		    				    if (status.contentEquals("Filled" )) {
		    					   //order has been filled. return Success 
		    				    	account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
		    					   return;
		    					
		    				    }
		    				    
		    				    // order has been cancelled so return the partial fill to acct mgr
		    				    account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, pQuantity, open_close_update, strategy, iv, stockPrice);
		    				    return;
		    				   
				         } else {
				    	     // close transaction case 
				    	     // just set the new quantity to place an order at the market for the remaining quantity; the code below will take care of it.
				    	     this.lot=Integer.toString( ((Integer.parseInt(quantity) - Integer.parseInt(pQuantity))/100) );				    					    	
				    	
				         }
		    			 
		    		}
		    		
		    	 }
		    	
		    	
		    	
		    }
		    
		}
			   
			   
			   
			   
		if (!closeTransaction){
			
			   //cancel order and return failure
			   CancelOrder cancel = new CancelOrder();
			   try {
				   cancel.requestOrderCancel(order_id);
			   } catch (Exception e) {
				//cannot cancel the order, exit program there are problems. If we do not exit we may have loops of posting orders
				// because the strategy will continue to ask to buy or sell when it goes into the previous state
				log.info("Cannot cancel the order......check manually to see what happened");
				log.info("Exiting program..........");
				e.printStackTrace();
				System.exit(0);
			   }
			   // Wait for 20 seconds and make sure that the order is cancelled
			   try {
				Thread.sleep(10000);
				
			   } catch (InterruptedException e1) {
				   log.info("Something went wrong in setting the 10 seconds sleep");
				    e1.printStackTrace();
			   }
			   try {
				   status = order_status.requestStatus(order_id);
				   log.info("Status is " + status);
			   } catch (Exception e) {
				log.info("Cannot request status on order_id " + order_id);
				e.printStackTrace();
			   }
			   // There may be a race condition for which the order has been filled anyway even if we tried to cancel if so return success
			   // otherwise the method  will return failure on its own 
			   if (status.contentEquals("Filled" )) {				
				   account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
				  return;				
			   }
									
		} else {
			// cancel the order and place a market order
			   
			   CancelOrder cancel = new CancelOrder();
			   try {
				   cancel.requestOrderCancel(order_id);
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
				   status = order_status.requestStatus(order_id);
				   log.info("Status is " + status);
			   } catch (Exception e) {
				log.info("Cannot request status on order_id " + order_id);
				e.printStackTrace();
			   }
			   // There may be a race condition for which the order has been filled anyway even if we tried to cancel if so return success
			   // otherwise the method  will return failure on its own 
			   if (status.contentEquals("Filled" )) {				
				   account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
				  return;				
			   }
			   
			   // assume that order has been canceled if we get here and place a market order
				price_type="market";
				
				CreateOptionOrder order1 = new CreateOptionOrder();		
				try {
					order1.createOrder(buy,lot,optionSymbolAMTD,optionPrice,price_type);
				} catch (Exception e) {
					log.info("Order could not be created");			
					e.printStackTrace();
					// return failure so the strategy will go back to previous state
					account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy, iv, stockPrice );
					return;
				}
				String order_id1 = order1.getOrderID();
				
				log.info("order_id is " + order_id1);
				
				/*
				if (order_id.contentEquals("")) {
					// Something went wrong we got an empty string. So return failure to account manager
					log.info("order_id is an empty string");
					account.returnOrderParameters(false, this.symbol, this.optionSymbol, this.optionPrice, this.quantity, this.buy, this.i, this.strategy);
					return;
					
				}
				*/
				
				// we have a market order do not cancel we just have to wait until is filled
				while (true) {
					
					try {
						 Thread.sleep(20000);
								
						} catch (InterruptedException e1) {
							log.info("Something went wrong in setting the 20 seconds sleep");
							e1.printStackTrace();
						}
					try {
						   status = order_status.requestStatus(order_id1);
						   log.info("Status is " + status);
					   } catch (Exception e) {
						log.info("Cannot request status on order_id " + order_id1);
						e.printStackTrace();
					   }
					   // There may be a race condition for which the order has been filled anyway even if we tried to cancel if so return success
					   // otherwise the method  will return failure on its own 
					   if (status.contentEquals("Filled" )) {				
						   account.optionReturnOrder(true, optionSymbol, symbol, optionPrice, buy_sell, lot, open_close_update, strategy, iv, stockPrice);
						  return;				
					   }
				}	
			
		}
		// if we get here return failure 
		account.optionReturnOrder(false,"", "", " ", "", buy_sell, "", strategy, iv, stockPrice );
		return;
				
	}
		

	
	
}


