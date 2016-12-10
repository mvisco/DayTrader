package com.TraderLight.DayTrader.Strategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;



public class MeanReversionStrategyNeq4 extends Strategy{
	
	
	enum States {S0, S1, S2, S3, S4, S5, S6, S7, S8, STemp };
	States StrategyState ;
	// We use currentState as a place to store the state from which we are coming when we go to STemp
	// StrategyState will be STemp in that case.
	States currentState;
	States desiredState;
	double possiblePrice;
	public double firstPrice = 0;
	public double secondPrice = 0;
	public double thirdPrice=0;
	public double fourthPrice = 0;
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
	//double previousClose;
	
	private final String OPEN = "open";
	private final String CLOSE = "close";
	private final String UPDATE = "update";
	private final String BUY = "buy";
	private final String SELL = "sell";
	
	
	public MeanReversionStrategyNeq4(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, double impVol, List<Integer> v) {
		
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
		//if ( ((count % 60) == 0 ) && (display)){
		    log.info(quote.getSymbol() +  " State " + StrategyState);
		    log.info("Bid : " + Math.round(currentBid*100)/100.0 + " ,Ask : " + Math.round(currentAsk*100)/100.0 +" ; Mean is: " + Math.round(mean*100)/100.0 + " ;difference is " + 
		         Math.round((currentPrice-mean)*100)/100.0 + " ; objective_change is " + this.objective_change);
		    log.info("volume  ratio is " + Math.round(((double)currentVolume/(double)avgVolume)*100)/100.0 + " ;previous close price is  " + previousClose + " ;lot is " +
		            this.lot );	
		    log.info("profit is " + profit + " loss is  " + loss + " Current date is " + quote.getCurrentDateTime());
		    if (currentVolume > (avgVolume+deltaVolume)) {				
			    log.info("ATTN: Symbol is trading with high volume " + symbol );
			
		    }
            if ( (StrategyState != States.S0) ){
			
			    log.info("The position price we are in is : " + this.price);			
		    }
            log.info("        ");
		//}
		// Do not trade before 7:35
		if ( (hours == 7) && (minutes <= 34) ) {
			//log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
			return;
		}	
			
		

		switch (StrategyState) {
		
		case S0:
					
			// We open a position if the value goes above the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a short position
			// Do not trade before 7:35
			if ( (hours >= 12) ) {
				//log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
				return;
			}
			
			
		    if ( ((currentBid >= (mean + this.objective_change) ) && ((currentBid - this.previousClose) >= this.objective_change))
		    		|| (openShortPositionWithPrice && (currentBid >= shortPositionPrice)) ){
	
		    	if (display) {
				    log.info("State S0:  attempting to sell one lot of stocks for symbol: " + quote.getSymbol() + " at bid " + currentBid);
				    log.info("Date is " + getDate);
		    	}
				this.currentState = States.S0;
				this.desiredState=States.S1;
				this.StrategyState=States.STemp;
				this.possiblePrice=currentBid;
				this.firstPrice = currentBid;
				account.buy_or_sell(SELL, OPEN, quote, lot, this);
				
			// We open a position if the value goes below  the mean by objective_change in automatic fashion 
			// or if there is a manual  intervention that tells us to open a long position
		    			    	
		    } else if ( ((currentAsk <= (mean - this.objective_change)) && ((this.previousClose - currentAsk) >= this.objective_change)) 
		    		|| (openLongPositionWithPrice && (currentAsk <= longPositionPrice)) ) {
		    	if (display) {	
				    log.info("State S0  attempting to buy  one lot of stocks for symbol: " + quote.getSymbol()+ " at ask " + currentAsk);
				    log.info("Date is " + getDate);
		    	}
				this.currentState = States.S0;
				this.desiredState=States.S2;
				this.StrategyState = States.STemp;
				this.possiblePrice=currentAsk;
				this.firstPrice = currentAsk;
				account.buy_or_sell(BUY, OPEN, quote, lot, this);
				
		    } else {
		    	// nothing to do
		    	;
		    }			
			break;


		case S1:
			
			// we are in this state because we sold one lot
			// close position if we made "profit" or we lost "loss"

				if ( (currentAsk <= this.price-this.profit) ) {
					if (display) {
					    log.info("State S1 attempting to close position at profit on symbol" + quote.getSymbol());
					}
					this.currentState = States.S1;
				    this.desiredState=States.S0;
				    this.StrategyState = States.STemp;
				    this.possiblePrice = 0;				
				    account.buy_or_sell(BUY, CLOSE, quote, lot, this);
				    // We clear the mean at the end of every cycle
				    clearMean();
				} else if ((currentBid >= (this.price+this.objective_change)) ){
					if (display) {
					   log.info("State S1:  attempting to sell another lot of stocks for symbol: " + quote.getSymbol() + " at bid " + currentBid);
					   log.info("Date is " + getDate);
					}
					this.currentState = States.S1;
					this.desiredState=States.S3;
					this.StrategyState=States.STemp;
					this.possiblePrice=(this.price+currentBid)/2.0;	
					this.secondPrice = currentBid;
					account.buy_or_sell(SELL, UPDATE, quote, lot, this);
				}
				
				
            
            
		    break;
		    
		case S2: 
			
			// we are in this state because we bought one lot
			// close position if we made "profit" or we lost "loss"
						
           
            	
				if (currentBid >= (this.price+this.profit)) {
					if (display) {
					    log.info("State S2 attempting to close position at profit on symbol" + quote.getSymbol());
					}
					this.currentState = States.S2;
				    this.desiredState=States.S0;
				    this.StrategyState = States.STemp;
				    this.possiblePrice = 0;	
				    account.buy_or_sell(SELL, CLOSE, quote, lot, this);
				    // We clear the mean at the end of every cycle
				    clearMean(); 
				} else if (currentAsk <= (this.price - this.objective_change) ){
					
					if (display) {
					    log.info("State S2  attempting to buy  another lot of stocks for symbol: " + quote.getSymbol()+ " at ask " + currentAsk);
					    log.info("Date is " + getDate);
					}
					this.currentState = States.S2;
					this.desiredState=States.S4;
					this.StrategyState = States.STemp;
					this.possiblePrice=(this.price+currentAsk)/2.0;	
					this.secondPrice = currentAsk;
					account.buy_or_sell(BUY, UPDATE, quote, lot, this);
					
				}				         		    	
		    
            
		    break;
		    
		case S3:
			
			if ( (currentAsk <= this.price-this.profit) ) {	
				
				log.info("State S3 attempting to close position at profit on symbol" + quote.getSymbol());
				this.currentState = States.S3;
				this.desiredState=States.S0;
				this.StrategyState = States.STemp;
				this.possiblePrice = 0;				
				account.buy_or_sell(BUY, CLOSE, quote, lot, this);
				// We clear the mean at the end of every cycle
				clearMean();
			} else if (currentAsk <= (this.secondPrice - profit)) {
				log.info("State S3 attempting to close one leg  at profit on symbol" + quote.getSymbol());
				this.currentState = States.S3;
				this.desiredState=States.S1;
				this.StrategyState = States.STemp;
				this.possiblePrice = this.firstPrice;				
				account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");
			} else  if ( (currentAsk > (this.price + objective_change)  )) {
				log.info("State S3 attempting to sell another lot on symbol" + quote.getSymbol());
				this.currentState = States.S3;
				this.desiredState=States.S5;
				this.StrategyState = States.STemp;	
				this.possiblePrice=(this.price + currentPrice)/(2.0);  
				account.buy_or_sell(SELL, UPDATE, quote, 2*lot, this);
	    		this.thirdPrice=currentPrice;
			}
			
			
			break;
			
		case S4:
			
			 if ( (currentBid >= (this.price+this.profit))  ){
				 
				    log.info("State S4 attempting to close position at profit on symbol" + quote.getSymbol());            					
					this.currentState = States.S4;
					this.desiredState=States.S0;
					this.StrategyState = States.STemp;
					this.possiblePrice = 0;	
					account.buy_or_sell(SELL, CLOSE, quote, lot, this);
					// We clear the mean at the end of every cycle
					clearMean();
			 } else if (currentBid >= (this.secondPrice + profit)) {
				    log.info("State S4 attempting to close one leg at profit on symbol" + quote.getSymbol());            					
					this.currentState = States.S4;
					this.desiredState=States.S2;
					this.StrategyState = States.STemp;
					this.possiblePrice = this.firstPrice;
					account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");
				 
				 
			 }  else if ( (currentBid < this.price-(this.objective_change)) ){
			    	log.info("State S4 attempting to buy another lot on symbol" + quote.getSymbol());
			    	this.currentState = States.S4;
					this.desiredState=States.S6;
					this.StrategyState = States.STemp;
					this.possiblePrice= (this.price + currentPrice)/(2.0); 
					account.buy_or_sell(BUY, UPDATE, quote, 2*lot, this);
		    		this.thirdPrice=currentPrice;
			    	
			    }
			
			break;
			
		case S5:
			
            if ( (currentAsk <= (this.price - profit)  )) {     
           		log.info("State S5 attempting to close position at profit on symbol" + quote.getSymbol());
        		//log.info("Thread ID is: " + Thread.currentThread().getId());
           		account.buy_or_sell(BUY, CLOSE, quote, lot, this);
        		this.currentState = States.S5;
        		this.desiredState=States.S0;
        		this.StrategyState = States.STemp;
        		this.possiblePrice = 0;
        		
        		clearMean();
        		
            } else if (currentAsk <= (this.thirdPrice - profit)) {
				log.info("State S5 attempting to close one leg  at profit on symbol" + quote.getSymbol());
				this.currentState = States.S5;
				this.desiredState=States.S3;
				this.StrategyState = States.STemp;
				this.possiblePrice=(this.firstPrice + this.secondPrice)/(2.0);
				account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");
            
            
            } else  if ( (currentAsk > (this.price + objective_change)  )) {
		    			    	
		    	
            	log.info("State S5 attempting to sell  4 more lots  for symbol: " + quote.getSymbol()); 
            	//log.info("Thread ID is: " + Thread.currentThread().getId());

            	this.currentState = States.S5;
            	this.desiredState=States.S7;
            	this.StrategyState = States.STemp;	
            	this.possiblePrice=(this.price + currentPrice)/(2.0); 
            	account.buy_or_sell(SELL, UPDATE, quote, 4*lot, this);
            	this.fourthPrice=currentPrice;	
		    }
			
			
			break;
			
			
		case S6:
			
	        if ( (currentBid >= this.price+(this.profit)) ){
            	
           		log.info("State S6 attempting to close position at profit on symbol" + quote.getSymbol());
           		//log.info("Thread ID is: " + Thread.currentThread().getId());
				
           		this.currentState = States.S4;
           		this.desiredState=States.S0;
           		this.StrategyState = States.STemp;
           		this.possiblePrice = 0;	
           		account.buy_or_sell(SELL, CLOSE, quote, lot, this);
           		clearMean();
	       
	        } else if (currentBid >= (this.thirdPrice + profit)) {
				log.info("State S6 attempting to close one leg  at profit on symbol" + quote.getSymbol());
				this.currentState = States.S6;
				this.desiredState=States.S4;
				this.StrategyState = States.STemp;
				this.possiblePrice=(this.firstPrice + this.secondPrice)/(2.0);
				account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");		
           
	       
	       } else if ( (currentBid < this.price-(this.objective_change)) ){
	    	   
	    	  
	    	   log.info("State is S6 attempting to buy 4 more  lots of calls for symbol: " + quote.getSymbol()); 
	    	   //log.info("Thread ID is: " + Thread.currentThread().getId());


	    	   this.currentState = States.S6;
	    	   this.desiredState=States.S8;
	    	   this.StrategyState = States.STemp;
	    	   this.possiblePrice= (this.price + currentPrice)/(2.0); 
	    	   account.buy_or_sell(BUY, UPDATE, quote, 4*lot, this);
	    	   this.fourthPrice=currentPrice;
			    	
          	
	       }
			
			
			
			break;
			
			
		case S7: 
            if ( (currentAsk <= (this.price - profit)  )) {     
          		log.info("State S7 attempting to close position at profit on symbol" + quote.getSymbol());		
          		this.currentState = States.S7;
          		this.desiredState=States.S0;
          		this.StrategyState = States.STemp;
          		this.possiblePrice = 0;
          		account.buy_or_sell(BUY, CLOSE, quote, lot, this);
          		clearMean();
           
           
           } else if (currentAsk <= (this.fourthPrice - profit)) {
				log.info("State S7 attempting to close one leg  at profit on symbol" + quote.getSymbol());
				this.currentState = States.S7;
				this.desiredState=States.S5;
				this.StrategyState = States.STemp;
				this.possiblePrice=( (this.firstPrice + this.secondPrice)/(2.0) + this.thirdPrice)/2.0;
				account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");
           }
			
			break;		
			
		case S8:
			
			 if ( (currentBid >= this.price+(this.profit)) ){
	            	
           		log.info("State S8 attempting to close position at profit on symbol" + quote.getSymbol());
           		//log.info("Thread ID is: " + Thread.currentThread().getId());
				
           		this.currentState = States.S8;
           		this.desiredState=States.S0;
           		this.StrategyState = States.STemp;
           		this.possiblePrice = 0;	
           		account.buy_or_sell(SELL, CLOSE, quote, lot, this);
           		clearMean();
	        }  else if (currentBid >= (this.fourthPrice + profit)) {
				log.info("State S8 attempting to close one leg  at profit on symbol" + quote.getSymbol());
				this.currentState = States.S8;
				this.desiredState=States.S6;
				this.StrategyState = States.STemp;
				this.possiblePrice=( (this.firstPrice + this.secondPrice)/(2.0) + this.thirdPrice)/2.0;
				account.sellOptionOneLeg(quote, quote.getSymbol(), this, "sell");
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
		    case S5:
		    case S7:
		    	log.info("State S1 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(BUY, CLOSE, lastQuote, lot, this);
		    	clearMean();
		    	break;
		    	
		    case S2:
		    case S4:
		    case S6:
		    case S8:
		    	log.info("State S2 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(SELL, CLOSE, lastQuote, lot, this);
		    	clearMean();
		    	break;
 
		    default:
                // no positions nothing to do.....
		    	//log.info("Asked to close position but we have no positions on symbol" + symbol);
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
    public void strategyCallbackOneLeg(boolean success, String symbol, String buy_sell, String price) {
    	
    	if (success) {	
    		if ( (this.currentState == States.S1) || (this.currentState == States.S2) ) {
    			this.StrategyState = this.desiredState;
    			this.price=this.possiblePrice;
    			this.currentState=this.desiredState;
    			this.firstPrice=0;

    		} else if ( (this.currentState == States.S3) || (this.currentState == States.S4)) {

    			// we have two positions this is the first one we sold    				
    			this.StrategyState = this.desiredState;
    			this.price=this.possiblePrice;
    			this.secondPrice=0;
    			if ((this.currentState == States.S3)) {
    				this.currentState=States.S1;   					
    			}
    			if ((this.currentState == States.S4)) {
    				this.currentState=States.S2;
    			}

    		} else if ( (this.currentState == States.S5) || (this.currentState == States.S6) ) {

    			// We have 3 positions open this is the first one we sold
    			this.StrategyState = this.desiredState;
    			this.price=this.possiblePrice;
    			this.thirdPrice=0;    				
    			if ((this.currentState == States.S5)) {
    				this.currentState=States.S3;

    			}
    			if ((this.currentState == States.S6)) {
    				this.currentState=States.S4;

    			}

    		}  else if ( (this.currentState == States.S7) || (this.currentState == States.S8) ) {

    			// We have 4 positions open this is the first one we sold
    			this.StrategyState = this.desiredState;
    			this.price=this.possiblePrice;
    			this.thirdPrice=0;    				
    			if ((this.currentState == States.S7)) {
    				this.currentState=States.S5;

    			}
    			if ((this.currentState == States.S8)) {
    				this.currentState=States.S6;

    			}

    		}



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
    
    @Override
    public void updateChangeProfitLoss(double change, double profit, double loss) {
    	this.objective_change = change;
    	this.profit = profit;
    	this.loss = loss;
		return;
	}

 /*
    @Override
	public void getPreviousClose(Date date) {
			
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		// if Monday go get the Friday before. Remember that the field 7 is the DAY_OF_THE_WEEK. it could be accessed also as 
		// Calendar.DAY_OF_THE_WEEK
		
		if (cal.get(Calendar.DAY_OF_WEEK) == 2) {
			cal.add(Calendar.DAY_OF_WEEK, -3);
		} else {
			// just get the day before
			cal.add(Calendar.DAY_OF_WEEK, -1);
		}
		
		int day = cal.get(Calendar.DATE);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		
		//String str="http://ichart.finance.yahoo.com/table.csv?s=QQQ&a=10&b=25&c=2016&d=11&e=02&f=2016&g=d";
		String str = "http://ichart.finance.yahoo.com/table.csv?s="+"QQQ"+"&a="+month+"&b="+day+"&c="+year+"&d="+month+"&e="+day+"&f="+year;
		URL url=null;
		try {
			url = new URL(str);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		try {
			System.out.println(inputStreamtoString(url.openStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  //System.out.println("res is " + res);
		  //log.info("Response is " + res);
		*/
    /*
		String str_response="";
		try {
			str_response = inputStreamtoString(url.openStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		String delim = " \n,";
		StringTokenizer st2 = new StringTokenizer(str_response, delim);
		double d = 0;
		int n=0;
		while (st2.hasMoreElements()) {
			//System.out.println(st2.nextElement()+ " " + this.symbol);
			String next = (String) st2.nextElement();
			log.info("Content for the Yahoo Query for symbol "+ symbol);
			log.info(next);
			if ( n == 11) {
				d = Double.parseDouble((String)st2.nextElement());
				log.info("This is the previous price " + d);
			}
			n++;
			//response.add((String)st2.nextElement());
		}
		this.previousClose = d;
		
		
	}
	 
		private  String inputStreamtoString(InputStream fi) throws IOException
		{
			ByteArrayOutputStream bout=new ByteArrayOutputStream();

			byte buffer[] = new byte[1000];
			int len;
			while( (len = fi.read(buffer)) != -1 ) {
				bout.write(buffer,0,len);
			}

			bout.close();
			fi.close();
			return bout.toString();
		}
*/


}

