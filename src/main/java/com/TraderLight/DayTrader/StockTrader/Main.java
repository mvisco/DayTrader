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
package com.TraderLight.DayTrader.StockTrader;

import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.GetFromStorageJDBC;
import com.TraderLight.DayTrader.MarketDataProvider.MarketDataProvider;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.Strategy.GammaScalping;
import com.TraderLight.DayTrader.Strategy.ManualStrategy;
import com.TraderLight.DayTrader.Strategy.MeanReversionStrategy;
import com.TraderLight.DayTrader.Strategy.Strategy;
import com.TraderLight.DayTrader.Strategy.TrendStrategy;
import com.TraderLight.DayTrader.TradeMonster.GetStockQuoteTM;
import com.TraderLight.DayTrader.TradeMonster.LoginTradeMonster;
import com.TraderLight.DayTrader.TradeMonster.TMSessionControl;
import com.TraderLight.DayTrader.Ameritrade.LoginAmeritrade;
import com.TraderLight.DayTrader.Ameritrade.KeepAliveAmeritrade;

/**
 *  This is the main class.
 * 
 * @author Mario Visco
 *
 */

public class Main {
	
	public static final Logger log = Logging.getLogger(true);
	public static Level1Quote prevQuote;
	public static double totalTradeCost=0;
	private static String broker=""; 
	public static int maxNumberOfPositions;
	public static int spreadTrading = 0;  // Default value is that we do not trade spread
	
	public static void main(String[] args) {
			
		Level1Quote newQuote = null;
		String[] quotes;
		long timeSentLogin=0;
		long timeReadConfig=0;
		boolean loginSent=false;
			
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
        for (Stock stock : listOfStocks) {
        	stock.createStrikes();
        	stock.createExpirations();
        }
				
		AccountMgr account= new AccountMgr(sysconfig.maxNumberOfPositions, sysconfig.mock, listOfStocks);
		
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
					stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(),stock.getVolumeVector());
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
			   Strategy strategy = new GammaScalping(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), 
						stock.getVolumeVector(), stock);
			   stock.setStrategy(strategy);
			   
		   } else {
			   log.info("Stategy ID not supported, assigning manual as default");
			   Strategy strategy = new ManualStrategy(stock.getSymbol(), stock.getLot(), stock.getChange(),
						stock.getTradeable(), account, stock.getLoss(), stock.getProfit(), stock.getImpVol(), 
						stock.getVolumeVector());
			   stock.setStrategy(strategy);
			   
		   }
			symbols += stock.getSymbol()+",";
			// initialize positions in Account manager if we have any.....
			account.initializePositions(stock.getSymbol(), stock.getStrategy());
		}
        
		MarketDataProvider.setParameters(sysconfig.qtURL, symbols);
		
		
		GetFromStorageJDBC getRecord = new GetFromStorageJDBC();
		Connection con = null;
		
		if (sysconfig.useDB) {
			// create connection to the DB
			String db = "traderlight2016-3";					
			// get connection
			con = getRecord.establishConnection(db); 
			if (con == null) {
				System.out.println("db connection error" + " " +db);
				System.exit(0);
			}
		}
		
		while (true) {
			
			// We are using time zone MST for this so the market opens at 7:30 and closes at 14:00
			Calendar rightNow = Calendar.getInstance();
			TimeZone mst = TimeZone.getTimeZone("America/Denver");
		    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		    sdf.setTimeZone(mst);
		    String[] time = sdf.format(rightNow.getTime()).split(":");       
			int min = Integer.parseInt(time[1]);
			int hour =Integer.parseInt(time[0]);
			
			if (System.currentTimeMillis() > (timeReadConfig+1000) ) {
				
				// read stocks configuration every 1 second
				// sysconfig does not change so no need to update that
				timeReadConfig = System.currentTimeMillis();
				Stock.updateStock("stock.xml");				
				for (Stock stock : listOfStocks ) {
					stock.strategy.updateStrategyParameters(stock.change, stock.profit, stock.loss, stock.lot, stock.tradeable, 
							stock.closePosition, stock.openLongPosition, stock.openShortPosition, stock.openLongPositionWithPrice, stock.openShortPositionWithPrice, 
							stock.priceForLongPosition, stock.priceForShortPosition);
				}
			}
						
			if (!loginSent) {
				
				if (broker.contentEquals("AMTD")) {
				    LoginAmeritrade login = new LoginAmeritrade(sysconfig.TDLogin, sysconfig.TDPassword, 
							sysconfig.TDAuthURL, sysconfig.TDSourceApp);
				    try {
					    login.login();
					   //TODO make login boolean to make sure that we logged in.. look at the result to be OK in login response
				    } catch (IOException e) {
					     log.info("login to AMTD failed for some reason... just quit ");
					     e.printStackTrace();
					     System.exit(0);
				    }
				    loginSent=true;
				} else if (broker.contentEquals("TM")  ) {		    	

					// Login into TM
					LoginTradeMonster loginTM = new LoginTradeMonster(sysconfig.OHLogin, sysconfig.OHPassword, 
							sysconfig.OHAuthURL, sysconfig.OHSourceApp);

					try {
						loginTM.login();
						loginSent=true;	
						timeSentLogin = System.currentTimeMillis();
						log.info("timeSentLogin is " + timeSentLogin);
						log.info("logged in TM ,  session id   is "+ TMSessionControl.getSessionid());
					} catch (Exception e) {
						//Not sure what is the best thing to do here....just exit
						log.info("Cannot login into OH");
						e.printStackTrace();
						System.exit(0);
					}

				} else {
					log.info("Wrong broker received that is not supported ..... quitting");
					System.exit(0);		
				}				 
			} 
			
			if ((loginSent) && (broker.contentEquals("AMTD"))) {
				
				if (System.currentTimeMillis() > (timeSentLogin+(60000*30)) ) {
					// send keep alive every 30 minutes
					KeepAliveAmeritrade keepAlive = new KeepAliveAmeritrade(sysconfig.TDAuthURL);
					try {
						boolean rc = keepAlive.keepAlive();
						if (!rc) {
							// login again
							LoginAmeritrade login = new LoginAmeritrade(sysconfig.TDLogin, sysconfig.TDPassword, 
									sysconfig.TDAuthURL, sysconfig.TDSourceApp);
						    try {
							    login.login();
							   //TODO make login boolean to make sure that we logged in.. look at the result to be OK in login response
						    } catch (IOException e) {
							     log.info("login to AMTD failed for some reason... we'll try again later ");
							     e.printStackTrace();							     
						    }
						    loginSent=true;
						}
					} catch (IOException e) {
						// what should we do here ?
						log.info("Something went wrong with sending keep Alive");
						e.printStackTrace();
					}
					timeSentLogin = System.currentTimeMillis();
					log.info("timeSentLogin is " + timeSentLogin);
					
				}
				
			}			
			if ( (loginSent) && (broker.contentEquals("TM")) ) {
				// for TM we do not have keep alive just request a quote every 10 minutes
				if (System.currentTimeMillis() > (timeSentLogin+(60000*10)) ) {
					timeSentLogin = System.currentTimeMillis();
					GetStockQuoteTM stockQuote = new GetStockQuoteTM();
					String bid = "";
					try {
						 bid =stockQuote.getStockQuote("AAPL");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (bid.isEmpty()) {
						// something is not right, I have seen that sometime Option House (TM) looses the connection so let's login again
						LoginTradeMonster loginTM = new LoginTradeMonster(sysconfig.OHLogin, sysconfig.OHPassword, 
								                                          sysconfig.OHAuthURL, sysconfig.OHSourceApp);						
						try {
							loginTM.login();
						} catch (Exception e) {
							log.info("Lost session and cannot login again into OH");
							e.printStackTrace();
						}
					}
				}
				
				
			}
									
			if ( ((hour > 7) && (hour < 14)) || ((hour == 7) && (min >= 29)) || ((hour == 14) && (min <= 1)) ) {
				
				// start getting quotes at 7:29 and end at 14:01
				if (!sysconfig.useDB) { 
				    quotes = MarketDataProvider.SEMarketDataProvider();
				    if (quotes != null) {
					 // send quote to strategy
					    for (String quote : quotes) {
						    newQuote = new Level1Quote(quote);
						    for (Stock stock : listOfStocks) {
							    if (stock.getSymbol().contentEquals(newQuote.getSymbol())) {
								    stock.getStrategy().stateTransition(newQuote);
								    break;
							    }
						    }
					     }
			        } else {
					log.error("Quotes returned from Market Data Provider is a null String Array");
				    }
				
				    try {
				    // wait 1 sec  before getting the new set of quotes from market data provider
				        Thread.sleep(1000);
				    } catch (InterruptedException e) {
					    log.error("got exception from  sleep number 1");
					    e.printStackTrace();
				    }
				} else {
					// get latest quotes from DB
					List<Level1Quote> l = new ArrayList<Level1Quote>();
					
				    // we are getting the last 50 quotes
				    l=getRecord.getLevel1QuoteList(con);
				    
				    // iterate on all quotes 
				    for (Level1Quote quote : l) {
				    	if ( (quote == null) || quote.getSymbol().contentEquals("") ) {
							continue;
					    }
				    	for (Stock stock : listOfStocks) {
						    if (stock.getSymbol().contentEquals(quote.getSymbol())) {
							    stock.getStrategy().stateTransition(quote);
							    break;
						    }
					    }
				    	
				    }
				    
				    try {
					    // wait 1 sec  before getting the new set of quotes from market data provider
					        Thread.sleep(1000);
					    } catch (InterruptedException e) {
						    log.error("got exception from  sleep number 1");
						    e.printStackTrace();
					    }
				}
				if ( ((hour==13) && (min>=58)) ) {
					//close all positions
					log.info("-------------------------------");
					log.info("Closing all positions.........");
					log.info("Closing all positions.........");
					log.info("Closing all positions.........");
					log.info("-------------------------------");
					for (Stock stock : listOfStocks) {
						//TODO Close all positions
						try {
				    		Thread.sleep(1000);
				    		stock.strategy.closePositions(stock.getSymbol());
							} catch (InterruptedException e) {
								log.error("got exception from  sleep when waiting for orders to close");
								e.printStackTrace();
							}
					}

					//Wait for 1 minute to wait for all  orders to close and then analyze trades
					try {
			    		Thread.sleep(60000);
						} catch (InterruptedException e) {
							log.error("got exception from  sleep when waiting for orders to close");
							e.printStackTrace();
						}					
					account.analyzeTrades();
					break;
				}
				
		    } else if ( hour >= 14) {
		    	// Day is over break the loop and exit program
		    	break;
		    } else {
		    	// we started  before 7:29 am so keep going but do not make any action
		    	try {
		    		log.info("market day did not start yet ....keep running");
		    		Thread.sleep(1000);
					} catch (InterruptedException e) {
						log.error("got exception from  sleep number 2");
						e.printStackTrace();
					}				
		    }
	    }
		
		log.info("Market Day is over........ exiting:");
		
		if (broker.contentEquals("TM") && (!sysconfig.mock)) {			
			LoginTradeMonster loginTM = new LoginTradeMonster(sysconfig.OHLogin, sysconfig.OHPassword, 
                    sysconfig.OHAuthURL, sysconfig.OHSourceApp);
			try {
				loginTM.logout();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	    System.exit(0);
	}	
}
