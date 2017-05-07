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

package com.TraderLight.DayTrader.Strategy;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.StockTrader.Stock;

public class MeanReversionNEq2 extends Strategy{
	
		
	enum States {S0, S1, S2, S3, S4, STemp, SDelta };
	States StrategyState ;
	// We use currentState as a place to store the state from which we are coming when we go to STemp
	// StrategyState will be STemp in that case.
	States currentState;
	States desiredState;
	double possiblePrice;
	public static final Logger log = Logging.getLogger(true);	
	AccountMgr account;
	double price;	
	DescriptiveStatistics stats;
	Level1Quote lastQuote; 
	int volumePortion=1;
	boolean openLongPositionWithPrice;
	boolean openShortPositionWithPrice;
	double shortPositionPrice;
	double longPositionPrice;
	int count;
	Stock stock;
	boolean soldPercentage = false;
	
	private final String OPEN = "open";
	private final String CLOSE = "close";
	private final String UPDATE = "update";
	private final String BUY = "buy";
	private final String SELL = "sell";
	
	
	public MeanReversionNEq2(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, double impVol, List<Integer> v, Stock stock) {
		
		super(symbol, symbol_lot, change, isTradeable, account, loss, profit, impVol, v);
		this.StrategyState = States.S0;
		this.currentState = States.S0;
		this.desiredState = States.S0;
		this.price = 0;
		this.possiblePrice=0;	
		this.lastQuote=null;
		this.openLongPositionWithPrice = false;
		this.openShortPositionWithPrice = false;
		this.shortPositionPrice = Double.MAX_VALUE;
		this.longPositionPrice = Double.MIN_VALUE;
		this.account = account;
		this.count = 0;
		this.stock = stock;
	}
	
	@Override
	public void stateTransition(Level1Quote quote) {

	   double mean;
	   double currentPrice;
	   double currentBid;
	   double currentAsk;
	   int minutes;
	   int hours;
	   int currentVolume;
	   int position;
	   int avgVolume;
	   int deltaVolume;
	   double change_from_previous_close;
	
	   	Date getDate =  quote.getCurrentDateTime();
	   	TimeZone mst = TimeZone.getTimeZone("America/Denver");
	    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    sdf.setTimeZone(mst);
	    String[] time = sdf.format(getDate).split(":");       
		minutes = Integer.parseInt(time[1]);
		hours =  Integer.parseInt(time[0]);
						
		if ( (hours == 7) && (minutes < 30) ) {
			
			// Start to calculate the mean right at the open i.e. at 7:30 and not before
			//log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
			return;			
		}
		
       if ( (hours >= 14) ) {
           // this takes care of the fact that sometime we may have some quotes from the previous day in the stream		
			return;			
		}
		
		mean = calculateMean(quote, lastQuote);
		lastQuote=quote;	
	
		if (this.isTradeable == false) {
			return;
		}
		
		currentBid=quote.getBid();
		currentAsk=quote.getAsk();
		currentPrice=(currentBid+currentAsk)/2D;
		change_from_previous_close = quote.getChange();
		currentVolume = quote.getVolume();
		position = minutePosition(quote);
		
		if (position != 0 ) {
			avgVolume = this.averageVolume.get(position);
		} else {
			avgVolume = 0;
		}
		 
		deltaVolume = avgVolume/volumePortion;
		count++;
		double positionValue = 0;
		if ( (count % 20) == 0 ) {
		    log.info(quote.getSymbol() +  " State " + StrategyState);
		    log.info("Bid : " + Math.round(currentBid*100)/100.0 + " ,Ask : " + Math.round(currentAsk*100)/100.0 +" ; Mean is: " + Math.round(mean*100)/100.0 + " ;difference is " + 
		         Math.round((currentPrice-mean)*100)/100.0 + " ; objective_change is " + this.objective_change);
		    log.info("volume  ratio is " + Math.round(((double)currentVolume/(double)avgVolume)*100)/100.0 + " ;change from previous close is " + change_from_previous_close + " ;lot is " +
		            this.lot );	
		    log.info("profit is " + profit + " loss is  " + loss + " Current date is " + quote.getCurrentDateTime());
		    if (currentVolume > (avgVolume+deltaVolume)) {				
			    log.info("ATTN: Symbol is trading with high volume " + symbol );
			
		    }
          
			
			log.info("The position price we are in is : " + this.price);			
		    
           double delta = account.calculatePortfolioDelta(quote, this);
		   log.info("Position Delta is " + delta);
		   positionValue = account.getPortfolioValue(quote, this, true);
		   log.info("Position value is " + positionValue);
           log.info("        ");
		}
		// Do not trade before 7:35
		if ( (hours == 7) && (minutes <= 34) ) {
			log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
			return;
		}	


		switch (StrategyState) {
		
		case S0:
			
			// We open a position if the value goes above the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a short position
			
		    if ( (currentBid >= (mean + this.objective_change))  
		    		|| (openShortPositionWithPrice && (currentBid >= shortPositionPrice)) ){
	
				log.info("State S0):  attempting to buy one lot of puts for symbol: " + quote.getSymbol() + " at bid " + currentBid);
				this.currentState = States.S0;
				this.desiredState=States.S1;
				this.StrategyState=States.STemp;
				this.possiblePrice=currentPrice;				
				account.option_buy_or_sell(BUY, OPEN, quote, lot, "P", this);
				
			// We open a position if the value goes below  the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a long position
		    			    	
		    } else if ( (currentAsk <= (mean - this.objective_change))  
		    		|| (openLongPositionWithPrice && (currentAsk <= longPositionPrice)) ) {
		    		
				log.info("State S0  attempting to buy  one lot of calls for symbol: " + quote.getSymbol());
				this.currentState = States.S0;
				this.desiredState=States.S2;
				this.StrategyState = States.STemp;
				this.possiblePrice=currentPrice;					
				account.option_buy_or_sell(BUY, OPEN, quote, lot, "C", this);
				
		    } else {
		    	// nothing to do
		    	;
		    }			
			break;
		
		case S1:
			
			// we are in this state because we sold one lot
			// close position if we made "profit" or we lost "loss"
            
            if ( (currentAsk <= this.price-this.profit) ) {	
          				
				log.info("State S1 attempting to close position at profit on symbol" + quote.getSymbol());				
				this.currentState = States.S1;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;				
				account.option_buy_or_sell(SELL, CLOSE, quote, lot, "P", this);
				// We clear the mean at the end of every cycle
				clearMean();
				
		    } else if (currentBid >= (this.price+(objective_change))) {
		    	
		    	if ( (hours == 13) && (minutes >= 30) ) {
		    		// do not add just return
		    		return;
		    	}

		    	log.info("State S1 attempting to buy  another lot of puts for symbol: " + quote.getSymbol());
		    	this.currentState = States.S1;
		    	this.desiredState=States.S3;
		    	this.StrategyState = States.STemp;	
		    	this.possiblePrice=(this.price + currentPrice)/(2.0);   
		    	account.option_buy_or_sell(BUY, OPEN, quote, lot, "P", this);

		    }
		    break;
		    
		case S2: 
			
			// we are in this state because we bought one lot
			// close position if we made "profit" or we lost "loss"
						
            if ( (currentBid >= (this.price+this.profit)) ){
            					
				log.info("State S2 attempting to close position at profit on symbol" + quote.getSymbol());            					
				this.currentState = States.S2;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;	
				account.option_buy_or_sell(SELL, CLOSE, quote, lot, "C", this);
				// We clear the mean at the end of every cycle
				clearMean(); 
				
            } else if (currentAsk <= (this.price-(objective_change))) {
		    	
		    	if ( (hours == 13) && (minutes >= 30) ) {
		    		// do not add just return
		    		return;
		    	}

		    	log.info("State S2 attempting to buy  another lot of calls for symbol: " + quote.getSymbol());
		    	this.currentState = States.S2;
		    	this.desiredState=States.S4;
		    	this.StrategyState = States.STemp;	
		    	this.possiblePrice=(this.price + currentPrice)/(2.0);   
		    	account.option_buy_or_sell(BUY, OPEN, quote, lot, "C", this);

		    }
            
		    break;
		    
		case S3:
			
			if ( (currentAsk <= (this.price - profit)  )) {            	

				log.info("State S3 attempting to close positions on symbol" + quote.getSymbol());
				//log.info("Thread ID is: " + Thread.currentThread().getId());

				this.currentState = States.S3;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;	
				account.option_buy_or_sell(SELL, CLOSE, quote, 2*lot, "P", this);
				clearMean();	
			} else {
				// nothing do but wait;
				;
			}
		    
			break;	
		
		case S4:
			
            if ( (currentBid >= (this.price+profit))  ) {
                    
				log.info("State S4 attempting to close positions on symbol" + quote.getSymbol());
				//log.info("Thread ID is: " + Thread.currentThread().getId());
				
				this.currentState = States.S4;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;	  
				account.option_buy_or_sell(SELL, CLOSE, quote, lot, "C", this);
				clearMean();
            } else {
            	// nothing else to do but wait
            	;
            }
			break;

		case SDelta:
			
			// in this state we adjust the the delta by selling  a portion of the options we own
			// adjustment happens for 20% delta variation
			
			//TODO Fix this we have 2* lot for one type of option while we we do not know how many we have on the other side to be
			// delta neutral so quantity should be gotten from the AccountMgr and should not be static...
			// I do not think that what I am saying above is true... Do not know right now
			
			if (positionValue >= 1000.0 ) {
				// sell everything
				log.info("State SDelta attempting to close positions on symbol" + quote.getSymbol());
				this.currentState = States.SDelta;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;
				account.option_buy_or_sell(SELL, CLOSE, quote, lot, "C", this);
				clearMean();
			}
/*			
			int quantity;
			double delta = account.calculatePortfolioDelta(quote, this);
			int calls = account.getQuantity(quote, "C" );
			int puts = account.getQuantity(quote, "P" );						
			int delta_int = (int) (delta/100);
		
			if (delta <= -(0.2*puts)) {
				// sell delta puts 
				quantity = delta_int;
				this.currentState = States.S0;
				this.desiredState=States.SDelta;
				this.StrategyState=States.STemp;
				//account.sellDeltaAdjustment(quote, quote.getSymbol(), this, SELL, quantity, "P");
				log.info("State SDelta attempting to delta adjust positions on symbol" + quote.getSymbol());
				account.deltaAdjustment(SELL, UPDATE, quote, quantity*100, "P", this);
				
			} else if (delta >= (0.2*calls)) {
				// sell delta calls 
				quantity = delta_int;
				this.currentState = States.S0;
				this.desiredState=States.SDelta;
				this.StrategyState=States.STemp;
				//account.sellDeltaAdjustment(quote, quote.getSymbol(), this, SELL, quantity, "C");
				log.info("State SDelta attempting to delta adjust positions on symbol" + quote.getSymbol());
				account.deltaAdjustment(SELL, UPDATE, quote, quantity*100, "C", this);
				
			} else {
				;
			}
*/			

			break;
		case STemp:
			//Nothing to do in this state we are waiting for the order processing to come back
	    default:
	    break;		    
		}
		
						
	}

	public void closePositions(Level1Quote quote) {
		lastQuote=quote;
		closePositions(quote.getSymbol());
		return;
	}
		
	public void closePositions(String symbol) {
		
	    double delta;
	    int new_lot;
	    
		switch (StrategyState) {
		   	
		    case S1:
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "P", this);
		    	clearMean();
		    	break;
		    	
		    case S2:
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "C", this);
		    	clearMean();
		    	break;
		    	
		    case S3:
		         // we need to get in the opposite position
		    	 
		    	 delta = account.calculatePortfolioDelta(lastQuote, this);
				 log.info("Position Delta is " + delta);
				 //log.info("Position value is " + account.getPortfolioValue(lastQuote, this, true));
				 // delta is negative because we only have puts
				 new_lot = -(int)delta/100;
				 log.info(" Attempting to get on the opposite position on symbol" + symbol + " for number of contracts "+ new_lot);
			     this.desiredState = States.SDelta;
				 this.currentState= StrategyState;
			     this.StrategyState=States.STemp;
			     // The new quantity to buy and to be delta neutral is not known because we do not have the delta of the new option to buy.
			     // We know the  total portfolio delta and we know that we currently have 2*lot in inventory, we also know that we are buying 
			     // the first out of the money option with delta approximately 0.5.
			     // so a reasonable approximation is to buy 2*new_lot*100. 
			     // For example if total delta is 10,000 and we assume that the option we want to buy has a 0.5 delta then we are buying 20,000 options. 
				 account.option_buy_or_sell(BUY, OPEN, lastQuote, 2*new_lot*100, "C", this);
				 break;
				 
		    case S4:
		    	// we need to get in the opposite position
		    	 delta = account.calculatePortfolioDelta(lastQuote, this);
				 log.info("Position Delta is " + delta);
				 //log.info("Position value is " + account.getPortfolioValue(lastQuote, this, true));				 
				 new_lot = (int)delta/100;
				 log.info(" Attempting to get on the opposite position on symbol" + symbol + " for number of contracts "+ new_lot);
			     this.desiredState = States.SDelta;
				 this.currentState= StrategyState;
			     this.StrategyState=States.STemp;
			  // The new quantity to buy and to be delta neutral is not known because we do not have the delta of the new option to buy.
			     // We know the  total portfolio delta and we know that we currently have 2*lot in inventory, we also know that we are buying 
			     // the first out of the money option with delta approximately 0.5.
			     // so a reasonable approximation is to buy 2*new_lot*100. 
			     // For example if total delta is 10,000 and we assume that the option we want to buy has a 0.5 delta then we are buying 20,000 options.
				 account.option_buy_or_sell(BUY, OPEN, lastQuote, 2*new_lot*100, "P", this);
				 break;
		    	
		    default:
               // no positions nothing to do.....
		    	//log.info("Asked to close position but we have no positions on symbol" + symbol);
			    break;						
		}
		return;

	}
	
	
	public void reallyClosePositions(String symbol) {
		
	    
		switch (StrategyState) {
		   	
		    case S1:
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "P", this);
		    	this.possiblePrice = 0;	
		    	clearMean();
		    	break;
		    	
		    case S2:
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "C", this);
		    	this.possiblePrice = 0;	
		    	clearMean();
		    	break;
		    	
		    case S3:
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "P", this);
		    	this.possiblePrice = 0;	
		    	clearMean();
		    	break;
				 
		    case S4:
		    	
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "C", this);
		    	this.possiblePrice = 0;	
		    	clearMean();
		    	break;
		    	
		    case SDelta:
		    	
		    	log.info(" Attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, "C", this);
		    	this.possiblePrice = 0;	
		    	clearMean();
		    	break;
		    	
		    default:
               // no positions nothing to do.....
		    	//log.info("Asked to close position but we have no positions on symbol" + symbol);
			    break;						
		}
		return;

	}
	
	public void closeLongPosition(String symbol, double longPosition ) {
		
	    if ( (longPosition < 0) || (longPosition > 1) ) {
	    	log.info(" Close long Position should be specified with a percentage  number between 0 and 1");
	    	return;
		}
	    
	    // After we sell a portion the strategy remains in SDelta to avoid to sell other times until the configuration flag 
	    // changes back to 0 we use a local state flag that tell us no to go into this selling loops
	    if (this.soldPercentage) {
	    	// we already sold so return.....
	    	return;
	    }
	    
		switch (StrategyState) {
		
		    case SDelta:		    	
		    	log.info(" Attempting to close a percent position on symbol" + symbol + " percentage is " + longPosition);
		    	this.desiredState = States.SDelta;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
				int calls = account.getQuantity(lastQuote, "C" );
				log.info("Total number of Calls reported by Account Mgr " + calls);					
				int delta_int = (int) (calls*longPosition);
				delta_int = (delta_int/100)*100;
				log.info("Number of calls that we are closing " + delta_int);
				this.soldPercentage = true;
				account.deltaAdjustment(SELL, UPDATE, lastQuote, delta_int, "C", this);
			
		    default:
               // this makes sense only in SDelta
		    	//log.info("Asked to close position but we have no positions on symbol" + symbol);
			    break;						
		}
		return;

	}
		
	
		public void closeShortPosition(String symbol, double shortPosition ) {
			
		    if ( (shortPosition < 0) || (shortPosition > 1) ) {
		    	log.info(" Close Short Position should be specified with a percentage  number between 0 and 1");
		    	return;
			}
		    // After we sell a portion the strategy remains in SDelta to avoid to sell other times until the configuration flag 
		    // changes back to 0 we use a local state flag that tell us no to go into this selling loops
		    if (this.soldPercentage) {
		    	// we already sold so return.....
		    	return;
		    }	    
			switch (StrategyState) {
			
			    case SDelta:		    	
			    	log.info(" Attempting to close a percent position on symbol" + symbol + " percentage is " + shortPosition);
			    	this.desiredState = States.SDelta;
				    this.currentState= StrategyState;
			    	this.StrategyState=States.STemp;			    	
					int puts = account.getQuantity(lastQuote, "P" );
					log.info("Total number of Puts reported by Account Mgr " + puts);						
					int delta_int = (int) (puts*shortPosition);
					delta_int = (delta_int/100)*100;
					log.info("Number of puts  that we are closing " + delta_int);
					this.soldPercentage = true;
					account.deltaAdjustment(SELL, UPDATE, lastQuote, delta_int, "P", this);
			    	break;
			    	
			    default:
	               // this makes sense only in SDelta
			    	//log.info("Asked to close position but we have no positions on symbol" + symbol);
				    break;						
			}
			return;

		}
	
	
	@Override	
	public void setStateToS0() {
		this.StrategyState = States.S0;    	
	}	

   @Override
   public void strategyCallback(boolean success, String symbol, String buy_sell, String price) {
   	
   //	log.info("Entering strategyCallback");
   //	log.info("Thread ID is: " + Thread.currentThread().getId());
   	
   	if (success) {
   		log.info(" Order was filled . Moving strategy to State " + this.desiredState + " for symbol " + symbol);
			log.info(" ");
			log.info(" ");
			this.StrategyState = this.desiredState;
			this.price = this.possiblePrice;
			this.currentState=this.desiredState;
   		
   	} else {
   		log.info("Order was NOT filled. Moving strategy BACK to State" + this.currentState + " for symbol " + symbol);
   		this.price=0;
   		this.StrategyState= this.currentState;
   		this.desiredState=this.currentState;
   	}  	
   	
   }
   
   @Override
   public void updateStrategyParameters(double change, double profit, double loss,int lot, boolean tradeable, boolean closePosition, 
		   boolean openLongPosition, boolean openShortPosition, boolean openLongPositionWithPrice, boolean openShortPositionWithPrice,
		   double longPrice, double shortPrice, double closePercentLongPosition, double closePercentShortPosition) {
   	
   	this.objective_change=change;
   	this.profit=profit;
   	this.loss = loss;
   	this.lot=lot; 
   	this.isTradeable = tradeable;
   	if (closePosition==true) {
   		//this.closePositions(this.symbol);
   		reallyClosePositions(symbol);
   	}
   	
   	if (closePercentLongPosition != 0) {
   		closeLongPosition(symbol,  closePercentLongPosition );
   	} else {
   		this.soldPercentage = false;
   	}
   	
   	if (closePercentShortPosition != 0) {
   		closeLongPosition(symbol,  closePercentShortPosition );
   	} else {
   		this.soldPercentage = false;
   	}
   	
   	if (openLongPosition) {
   		if ( (StrategyState == States.S0) ){ 
   			
   			log.info("State S0  attempting to buy  one lot of calls for symbol: " + symbol);
				this.currentState = States.S0;
				this.desiredState=States.S2;
				this.StrategyState = States.STemp;
				this.possiblePrice=lastQuote.getAsk();					
				//account.buy_or_sell(BUY, OPEN, lastQuote, lot, this);
				account.option_buy_or_sell(BUY, OPEN, lastQuote, lot, "C", this);
   			
   		} else {
   			// only open a new position if we are in S0
   			log.info("Asked to open a long position but strategy is not in S0 " + symbol);
   		}
   	}
   	
   	if (openShortPosition) {
   		if ( (StrategyState == States.S0)  ){
   			
   			log.info("State S0:  attempting to sell one lot of puts for symbol: " + symbol);
				this.currentState = States.S0;
				this.desiredState=States.S1;
				this.StrategyState=States.STemp;
				this.possiblePrice=lastQuote.getBid();				
				//account.buy_or_sell(SELL, OPEN, lastQuote, lot, this);
				account.option_buy_or_sell(BUY, OPEN, lastQuote, lot, "P", this);
   			
   		} else {
   			// only open a new position if we are in S0
   			log.info("Asked to open a short position but strategy is not in S0 " + symbol);
   		}
   	}
   	
   	if (openLongPositionWithPrice) {
   		// Price  has to be specified otherwise nothing to do,also we need to be in S0
   		if ( (longPrice != 0) && (StrategyState == States.S0) ){
   			this.openLongPositionWithPrice = true;
   			this.longPositionPrice = longPrice;
   			
   		} else {
   			// only open a new position if we are in S0
   			log.info("Asked to open a long position with Price but strategy is not in S0 or price is not specified " + symbol);
   		}
   	} else {
   		this.openLongPositionWithPrice = false;
   		this.longPositionPrice = Double.MIN_VALUE;
   	}
   	
   	if (openShortPositionWithPrice) {
   		// Price  has to be specified otherwise nothing to do,also we need to be in S0
   		if ( (shortPrice != 0) && (StrategyState == States.S0) ){
   			this.openShortPositionWithPrice = true;
   			this.shortPositionPrice = shortPrice;
   			
   		} else {
   			// only open a new position if we are in S0
   			log.info("Asked to open a short position with Price but strategy is not in S0 or price is not specified " + symbol);
   		}
   	} else {
   		this.openShortPositionWithPrice = false;
   		this.shortPositionPrice = Double.MAX_VALUE;
   	}
   	
   }
	
	public void resetPricesProfits() {
		this.price=0;		
	}
	
   public double getPortfolioValue (String symbol) {
   	
   	//log.info("Last quote is " + lastQuote);
   	//log.info("Last quote symbol is " + lastQuote.getSymbol());
   	
   	return account.getPortfolioValue(symbol, this);  	
   }
   
   public boolean isStateStemp() {
   	if (this.StrategyState == States.STemp) {
   		return true;
   	}
   	return false;
   }
   
   public void setState(int count_calls, int count_puts, double underlying_price) {
		
	   if (count_calls == 0) {
		   if (count_puts == 0) {
			   this.StrategyState = States.S0;
		   } else if (count_puts == 1) {
			   this.StrategyState = States.S1;
		   } else if ( count_puts == 2) {
			   this.StrategyState = States.S3;
		   }
	   } else if (count_calls == 1) {
		   if (count_puts == 0) {
			   this.StrategyState = States.S2;
		   } else {
			   this.StrategyState = States.SDelta;
		   }		   
	   } else if (count_calls == 2) {
		   if (count_puts == 0) {
			   this.StrategyState = States.S4;
		   } else {
			   this.StrategyState = States.SDelta;
		   }
	   }
	   this.price = underlying_price;
	 }

}
