package com.TraderLight.DayTrader.Strategy;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;




public class StockGammaScalping extends Strategy{
	
		
	enum States {S0, S1, S2, S3, S4, STemp, SInit0, SInit1, SInitial };
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
	
	
	public StockGammaScalping(String symbol, int symbol_lot, double change, boolean isTradeable, AccountMgr account, double loss, 
			double profit, double impVol, List<Integer> v) {
		
		super(symbol, symbol_lot, change, isTradeable, account, loss, profit, impVol, v);
		this.StrategyState = States.SInit0;
		this.currentState = States.SInit0;
		this.desiredState = States.SInit0;
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
		if ( (count % 10) == 0 ) {
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
           double delta = account.calculatePortfolioDelta(quote, this);
			log.info("Portfolio Delta is " + delta);
			log.info("Portfolio value is " + account.getPortfolioValue(quote, this));
           log.info("        ");
		}
		// Do not trade before 7:35
		if ( (hours == 7) && (minutes <= 34) ) {
			log.info("Not time yet to trade, time is: " + quote.getCurrentDateTime());
			return;
		}	
			
		//int  price1 = (int)(currentBid *10);
    	int k1 = 1;
    	
    	double q = currentBid - (int)currentBid;
        if ( (q == 0.0) || (q == 0.5)) {
        	 k1 = 0;
        }
		switch (StrategyState) {
		
		case SInit0:
			
			 if ( k1 == 0) {
					
					log.info("State SInit0):  attempting to buy one lot of puts for symbol: " + quote.getSymbol() + " at bid " + currentBid);
					log.info("Date is " + getDate);
					this.currentState = States.SInit0;
					this.desiredState=States.SInit1;
					this.StrategyState=States.STemp;
					this.possiblePrice=currentPrice;
					account.buyPutOption(quote, lot, false, this, false, SELL);
					//account.buy_or_sell(SELL, OPEN, quote, lot, this);
					
					
				
					
			    } else {
			    	// nothing to do
			    	;
			    }			
				break;
				
		case SInit1:
			
			log.info("State SInit1):  attempting to buy stocks for symbol: " + quote.getSymbol() + " at ask " + currentAsk);
			this.currentState = States.SInit1;
			this.desiredState=States.S0;
			this.StrategyState=States.STemp;
			//this.possiblePrice=currentPrice;
			int stock_quantity = (int) Math.abs((account.calculatePortfolioDelta(quote, this)));
			account.buy_or_sell(BUY, OPEN, quote, stock_quantity, this);
			break;
		
		case S0:
			
			// in this state we adjust the the delta by selling  a portion of the stocks we own
			// adjustment happens for 10% delta variation
			double delta = account.calculatePortfolioDelta(quote, this);
			//log.info("Portfolio Delta is " + delta);
			int quantity = (lot*2)/10;
			if (delta <= -0.1*lot) {
				// buy 10% more of stocks 
				this.currentState = States.S0;
				this.desiredState=States.S0;
				this.StrategyState=States.STemp;
				account.buy_or_sell_delta(BUY, UPDATE, quote, (int) Math.abs(delta), this );
				//account.sellDeltaAdjustment(quote, quote.getSymbol(), this, SELL, (int) Math.abs(delta), "P");
				
			} else if (delta >= 0.1*lot) {
				// sell 10% of the stocks 
				this.currentState = States.S0;
				this.desiredState=States.S0;
				this.StrategyState=States.STemp;
				account.buy_or_sell_delta(SELL, UPDATE, quote, (int) Math.abs(delta), this);
				//account.sellDeltaAdjustment(quote, quote.getSymbol(), this, SELL, quantity, "C");
				
				
			} else {
				;
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
		    	log.info("State S1 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(BUY, CLOSE, lastQuote, lot, this);
		    	clearMean();
		    	break;
		    	
		    case S2:
		    case S0:
		    	log.info("State S2 attempting to close position on symbol" + symbol);
		    	this.desiredState = States.S0;
			    this.currentState= StrategyState;
		    	this.StrategyState=States.STemp;
		    	account.buy_or_sell(SELL, CLOSE, lastQuote, lot, this);
		    	account.option_buy_or_sell(SELL, CLOSE, lastQuote, lot, this);
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
   	this.StrategyState = States.SInit0;    	
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
