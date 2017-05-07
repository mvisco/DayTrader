package com.TraderLight.DayTrader.Ameritrade;


import java.io.IOException;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;



public class GetQuoteAmeritrade {
	
	public static final Logger log = Logging.getLogger(true);
	public String bidS;
	public String askS;
	public String iv;
	
	
	
	public  String  getQuotes(String symbols, boolean buy,boolean closeTransaction,double optionBidAskSpread) throws IOException, InvalidSessionException {
		
		  double quote;
		  double bid1;
		  double ask1;
			
		  log.info("Requesting quote for " + symbols);
		  String str = SessionControl.getUrl()+"100/Quote;jsessionid="+SessionControl.getSessionid()+"?source="+SessionControl.getSourceApp()+"&symbol="+symbols;
		  String res=URLUtil.getfromURL(str);
		  //System.out.println("res is " + res);
		  log.info("Response is " + res);
		  // return unpack(res);
		  XMLNode root=new XMLNodeBuilder(res).getRoot();
		  bidS = root.getChildwithNameNonNull("quote-list").getChildwithNameNonNull("quote").getChildwithName("bid").getValue();
		  askS = root.getChildwithNameNonNull("quote-list").getChildwithNameNonNull("quote").getChildwithName("ask").getValue();
		  iv = root.getChildwithNameNonNull("quote-list").getChildwithNameNonNull("quote").getChildwithName("implied-volatility").getValue();
		  
		  log.info("bid is " + bidS + " ask is "+ askS + " impVol is " + iv);
		  
		  Double imp_vol = Double.parseDouble(iv)/100.0;
		  iv = String.valueOf(imp_vol);
		  
		  if (closeTransaction) {
				log.info("Asked to close transaction so return bid or ask");
				if (buy) {
					return askS;
				} else {
					return bidS;
				}
		  }
		  
		  
		  
		  
		  double bid = Double.parseDouble(bidS);
		  double ask = Double.parseDouble(askS);
		  double diff = Math.round( (ask-bid) * 100.0 ) / 100.0;
		  
		  if (buy) {
				// if buying try to get the best price return the bid+0.02 or if option price is greater than 3 return bid+0.1 to throw a bone 
				// to the market makers, in all other cases return the bid
				// We will sit on it for 5 minutes in the order executor to see if gets filled....
				if (bid >= 3.0) {
					//double number1 = Math.round((bid+0.1) * 100.0) / 100.0;
					return String.valueOf(bid+0.1);
				} else {
					double number2 = Math.round((bid+0.02) * 100.0) / 100.0;
					return String.valueOf(number2);
				}
			}
			
			// The rest of this code should really apply only to the sell case where we try to get in the middle
		  
		  if ( (diff == 0.01 ) || (diff == 0.02) ){
				if (buy) {
					return askS;
				} else {
					return bidS;
				}
		  }
		  if ( (diff == 0.03) ) {
			  
			    bid1 = Double.parseDouble(bidS) + 0.01;
				ask1 = Double.parseDouble(askS)-0.01;
				bid1=Math.round(bid1*100)/100D;
				ask1=Math.round(ask1*100)/100D;
				if (buy) {
					return String.valueOf(ask1);
				} else {
					return String.valueOf(bid1);
				}
	   	  }
		  
		  if ( (diff == 0.04) || (diff == 0.05) ) {
			  
			  // if option value is greater or equal to 3 just return the ask or the bid. This is the case of where  the diff should be 0.05....
			  if (Double.parseDouble(bidS) >= 3.0) {
					if (buy) {
						return askS;
					} else {
						return bidS;
					}
			    }
			    bid1 = Double.parseDouble(bidS) + 0.02;
				ask1 = Double.parseDouble(askS)-0.02;
				bid1=Math.round(bid1*100)/100D;
				ask1=Math.round(ask1*100)/100D;
				if (buy) {
					return String.valueOf(ask1);
				} else {
					return String.valueOf(bid1);
				}
	   	  }
		  
		  
		  //log.info("bid, ask and iv " + bid+ " " + ask + " " + iv);	
		  
		  // The rest of the cases we try to get in the middle except for when  buying and the spread is too high, see below
		  quote = (Double.parseDouble(bidS) + Double.parseDouble(askS))/2;
		  int quote_int=(int) quote;
		  
		  
		  
			if (quote_int > 3) {
				  // we need increment of 5 cents
				  return(setPrecision(quote,2,buy));
			} 
			  
			//Otherwise increments of 1 cent are acceptable
			quote=Math.round(quote * 100.0) / 100.0;		
			return(String.valueOf(quote));
			  
		  
		  
		}
	
	public String getBid() {
		return this.bidS;
	}
	
	public String getAsk() {
		return this.askS;
	}
	
	public String getIv() {
		return this.iv;
	}

	public static String setPrecision(double nbr, int decimal,boolean buy){
	    
	    int integer_Part = (int) nbr;
	    double float_Part = nbr - integer_Part;
	    float_Part = Math.round(float_Part * 100.0) / 100.0;
	    int floating_point = (int) (Math.pow(10, decimal) * float_Part);
	    
	    if (floating_point == 0) {
	    	return String.valueOf(integer_Part);
	    }
	    if (buy) {
	    	// get closer to the ask
	    	floating_point=floating_point+(5-(floating_point%5));
	    } else {
	    	// get closer to the bid
	    	floating_point=floating_point-(floating_point%5);
	    }
	    if (floating_point >= 100) {
	    	// this should happen only when we enter this function with nbr like 2.98 so with decimal part higher than .97. This happens for example 
	    	// when bid is 1.98 and ask is 1.99
	    	integer_Part=integer_Part+1;
	    	floating_point=0;	    	
	    }
	    String final_nbr = String.valueOf(integer_Part) + "." + String.valueOf(floating_point);
	    return final_nbr;
	    
	}
	
    public static String setIncrementOf10(double nbr, int decimal, boolean buy){
	    
	    int integer_Part = (int) nbr;
	    double float_Part = nbr - integer_Part;
	    // double  does  weird  thing sometime with operations. You can get approximated results for defect that will be truncated later and will give wrong 
	    // price. For example 6.30 - 6 will return 0.299999999999998. So rounding it first that seems to work......
	    float_Part = Math.round(float_Part * 100.0) / 100.0;
	    int floating_point = (int) (Math.pow(10, decimal) * float_Part);
	    if (buy) {
	    	// get closer to the ask
	    	floating_point=floating_point+(10-(floating_point%10));
	    } else {
	    	// get closer to the bid
	    	floating_point=floating_point-(floating_point%10);
	    }
	    if (floating_point >= 100) {
	    	// this should happen only when we enter this function with nbr like 4.975 so with decimal part higher than .94. This happens when bid is 4.90 and ask is 5.05 
	    	integer_Part=integer_Part+1;
	    	floating_point=0;	    	
	    }
	    String final_nbr = String.valueOf(integer_Part) + "." + String.valueOf(floating_point);
	    return final_nbr;
	    
	}	
	
	
	
}
