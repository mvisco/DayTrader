package com.TraderLight.DayTrader.Strategy;

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


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;


/**
 *  This class implements a men reversion strategy doubling down . 
 *  In this strategy orders will  be placed automatically when the security goes above or below a certain threshold. 
 *  The threshold is calculated as the moving average + or - the expected change.
 *  Definition of states:
 *    S0 Initial State for mean reversion;
 *    S1 Sold one lot;
 * 	  S2 Bought one lot;
 *    S3 Sold 2 lots
 *    S4 Bought 2 lots 
 *    STemp used for asynchronous processing to transition between states when order is placed;
 *  
 * 
 * @author Mario Visco
 *
 */

public class MeanReversionStrategyNeq2 extends Strategy{
	
		
	enum States {S0, S1, S2, S3, S4, STemp };
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
	
	private final String OPEN = "open";
	private final String CLOSE = "close";
	private final String UPDATE = "update";
	private final String BUY = "buy";
	private final String SELL = "sell";
	
	
	public MeanReversionStrategyNeq2(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, List<Integer> v) {
		
		super(symbol, symbol_lot, change, isTradeable, account, loss, profit, v);
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
		if ( (count % 60) == 0 ) {
		    log.info(quote.getSymbol() +  " State " + StrategyState);
		    log.info("Bid : " + Math.round(currentBid*100)/100.0 + " ,Ask : " + Math.round(currentAsk*100)/100.0 +" ; Mean is: " + Math.round(mean*100)/100.0 + " ;difference is " + 
		         Math.round((currentPrice-mean)*100)/100.0 + " ; objective_change is " + this.objective_change);
		    log.info("volume  ratio is " + Math.round(((double)currentVolume/(double)avgVolume)*100)/100.0 + " ;change from previous close is " + change_from_previous_close + " ;lot is " +
		            this.lot );	
		    log.info("profit is " + profit + " loss is  " + loss + " Current date is " + quote.getCurrentDateTime());
		    if (currentVolume > (avgVolume+deltaVolume)) {				
			    log.info("ATTN: Symbol is trading with high volume " + symbol );
			
		    }
            if ( (StrategyState != States.S0) ){
			
			    log.info("The position price we are in is : " + this.price);			
		    }
            log.info("        ");
		}
		// Do not trade before 7:35
		if ( (hours == 7) && (minutes <= 34) ) {
			//log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
			return;
		}	
			
		

		switch (StrategyState) {
		
		case S0:
					
			// We open a position if the value goes above the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a short position
			
		    if ( (currentBid >= (mean + this.objective_change))  
		    		|| (openShortPositionWithPrice && (currentBid >= shortPositionPrice)) ){
	
				log.info("State S0:  attempting to sell one lot of stocks for symbol: " + quote.getSymbol() + " at bid " + currentBid);
				log.info("Date is " + getDate);
				this.currentState = States.S0;
				this.desiredState=States.S1;
				this.StrategyState=States.STemp;
				this.possiblePrice=currentBid;				
				account.buy_or_sell(SELL, OPEN, quote, lot, this);
				
			// We open a position if the value goes below  the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a long position
		    			    	
		    } else if ( (currentAsk <= (mean - this.objective_change))  
		    		|| (openLongPositionWithPrice && (currentAsk <= longPositionPrice)) ) {
		    		
				log.info("State S0  attempting to buy  one lot of stocks for symbol: " + quote.getSymbol()+ " at ask " + currentAsk);
				log.info("Date is " + getDate);
				this.currentState = States.S0;
				this.desiredState=States.S2;
				this.StrategyState = States.STemp;
				this.possiblePrice=currentAsk;					
				account.buy_or_sell(BUY, OPEN, quote, lot, this);
				
		    } else {
		    	// nothing to do
		    	;
		    }			
			break;


		case S1:
			
			// we are in this state because we sold one lot
			// close position if we made "profit" or we lost "loss"
            
            if ( (currentAsk <= this.price-this.profit) || (currentAsk >= (this.price+this.loss)) ) {	
          
				if ( (currentAsk <= this.price-this.profit) ) {
					log.info("State S1 attempting to close position at profit on symbol" + quote.getSymbol());
					this.currentState = States.S1;
				    this.desiredState=States.S0;
				    this.StrategyState = States.STemp;
				    this.possiblePrice = 0;				
				    account.buy_or_sell(BUY, CLOSE, quote, lot, this);
				    // We clear the mean at the end of every cycle
				    clearMean();
				} else {
					
					log.info("State S1:  attempting to sell another lot of stocks for symbol: " + quote.getSymbol() + " at bid " + currentBid);
					log.info("Date is " + getDate);
					this.currentState = States.S1;
					this.desiredState=States.S3;
					this.StrategyState=States.STemp;
					this.possiblePrice=(this.price+currentBid)/2.0;				
					account.buy_or_sell(SELL, UPDATE, quote, lot, this);
				}
				
				
            }
            
		    break;
		    
		case S2: 
			
			// we are in this state because we bought one lot
			// close position if we made "profit" or we lost "loss"
						
            if ( (currentBid >= (this.price+this.profit)) || (currentBid <= (this.price - this.loss)) ){
            	
				if (currentBid >= (this.price+this.profit)) {
					log.info("State S2 attempting to close position at profit on symbol" + quote.getSymbol());
					this.currentState = States.S2;
				    this.desiredState=States.S0;
				    this.StrategyState = States.STemp;
				    this.possiblePrice = 0;	
				    account.buy_or_sell(SELL, CLOSE, quote, lot, this);
				    // We clear the mean at the end of every cycle
				    clearMean(); 
				} else {
					
					log.info("State S2  attempting to buy  another lot of stocks for symbol: " + quote.getSymbol()+ " at ask " + currentAsk);
					log.info("Date is " + getDate);
					this.currentState = States.S2;
					this.desiredState=States.S4;
					this.StrategyState = States.STemp;
					this.possiblePrice=(this.price+currentAsk)/2.0;				
					account.buy_or_sell(BUY, UPDATE, quote, lot, this);
					
				}				         		    	
		    }
            
		    break;
		    
		case S3:
			
			if ( (currentAsk <= this.price-this.profit) || (currentAsk >= (this.price+this.loss)) ) {	
				if ( (currentAsk <= this.price-this.profit) ) {
					log.info("State S3 attempting to close position at profit on symbol" + quote.getSymbol());
				} else {
					log.info("State S3 attempting to close position at loss on symbol" + quote.getSymbol());
				}
				
				this.currentState = States.S3;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;				
				account.buy_or_sell(BUY, CLOSE, quote, lot, this);
				// We clear the mean at the end of every cycle
				clearMean();
			}
			
			
			break;
			
		case S4:
			
			 if ( (currentBid >= (this.price+this.profit)) || (currentBid <= (this.price - this.loss)) ){
	            	
					if (currentBid >= (this.price+this.profit)) {
						log.info("State S2 attempting to close position at profit on symbol" + quote.getSymbol());
					} else {
						log.info("State S2 attempting to close position at loss on symbol" + quote.getSymbol());
					}
	            					
					this.currentState = States.S4;
					this.desiredState=States.S0;
					this.StrategyState = States.STemp;
					this.possiblePrice = 0;	
					account.buy_or_sell(SELL, CLOSE, quote, lot, this);
					// We clear the mean at the end of every cycle
					clearMean();          		    	
			    }
			
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
	    
		switch (StrategyState) {

		    case S1:
		    case S3:
		    	log.info("State S1 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(BUY, CLOSE, lastQuote, lot, this);
		    	clearMean();
		    	break;
		    	
		    case S2:
		    case S4:
		    	log.info("State S2 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(SELL, CLOSE, lastQuote, lot, this);
		    	clearMean();
		    	break;
 
		    default:
                // no positions nothing to do.....
		    	log.info("Asked to close position but we have no positions on symbol" + symbol);
			    break;						
		}
		return;
	}
		
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
			this.price=this.possiblePrice;
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
		   double longPrice, double shortPrice) {
    	
    	this.objective_change=change;
    	this.profit=profit;
    	this.loss = loss;
    	this.lot=lot; 
    	this.isTradeable = tradeable;
    	if (closePosition==true) {
    		this.closePositions(this.symbol);
    	}
    	
    	if (openLongPosition) {
    		if ( (StrategyState == States.S0) ){ 
    			
    			log.info("State S0  attempting to buy  one lot of stocks for symbol: " + symbol);
				this.currentState = States.S0;
				this.desiredState=States.S2;
				this.StrategyState = States.STemp;
				this.possiblePrice=lastQuote.getAsk();					
				account.buy_or_sell(BUY, OPEN, lastQuote, lot, this);
    			
    		} else {
    			// only open a new position if we are in S0
    			log.info("Asked to open a long position but strategy is not in S0 " + symbol);
    		}
    	}
    	
    	if (openShortPosition) {
    		if ( (StrategyState == States.S0)  ){
    			
    			log.info("State S0:  attempting to sell one lot of stocks for symbol: " + symbol);
				this.currentState = States.S0;
				this.desiredState=States.S1;
				this.StrategyState=States.STemp;
				this.possiblePrice=lastQuote.getBid();				
				account.buy_or_sell(SELL, OPEN, lastQuote, lot, this);
    			
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
    



    



}

