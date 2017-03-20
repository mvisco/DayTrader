package com.TraderLight.DayTrader.Ameritrade;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class TestMethod {
	
	public static final Logger log = Logging.getLogger(true);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		String bid="2.0";
		String ask="2.02";
		boolean buy=false;
		double quote;
		double bid1;
		double ask1;
		DecimalFormat format = new DecimalFormat();
	    format.setDecimalSeparatorAlwaysShown(false);
	    
		double diff = Math.round( (Double.parseDouble(ask)-Double.parseDouble(bid)) * 100.0 ) / 100.0;
		log.info("diff is " + diff);
		  if (diff == 0.01 ) {
				if (buy) {
					log.info("quote is ask " + ask);					
			        log.info("quote is ask without the .0 " + format.format(Double.parseDouble(ask)));
					return;
				} else {
					log.info("quote is bid  " + bid);
					return;
				}
		  }
		  if ( (diff == 0.02) || (diff == 0.03) ) {
				bid1 = Double.parseDouble(bid) + 0.01;
				ask1 = Double.parseDouble(ask)-0.01;
				bid1=Math.round(bid1*100)/100D;
				ask1=Math.round(ask1*100)/100D;
				if (buy) {
					log.info("quote is ask1 " + ask1);
					log.info("quote is ask1 without the .0 " + format.format(ask1));
					return;
				} else {
					log.info("quote is bid1  " + bid1);
					log.info("quote is bid1 without the .0 " + format.format(bid1));
					return;
				}
	   	  }
		  

		  
		  if ( (diff == 0.04) || (diff == 0.05) ) {
			    bid1 = Double.parseDouble(bid) + 0.02;
				ask1 = Double.parseDouble(ask)-0.02;
				bid1=Math.round(bid1*100)/100D;
				ask1=Math.round(ask1*100)/100D;
				if (buy) {
					log.info("quote is ask1 " + ask1);
					return;
				} else {
					log.info("quote is bid1  " + bid1);
					return;
				}
		  }
		  
		 
		quote = (Double.parseDouble(bid) + Double.parseDouble(ask))/2;
		log.info("quote is " + quote);	
		
		int quote_int=(int) quote;
		if (quote_int < 3) {
			  
			  String quote3 = setPrecision(quote,2,buy);
			  log.info("Quote is quote3 " + quote3);
			  return;
		}
		String quote_str = setIncrementOf10(quote,2,buy);
		log.info("Quote is quote_str  " + quote_str);

	}

	
	public static String setPrecision(double nbr, int decimal,boolean buy){
	    
	    int integer_Part = (int) nbr;
	    double float_Part = nbr - integer_Part;
	    int floating_point = (int) (Math.pow(10, decimal) * float_Part);
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
		    log.info("int is " + integer_Part);
		    double  float_Part =  nbr - integer_Part;
		    
		    log.info("float_part is " + float_Part);
		    
		    float_Part = Math.round(float_Part * 100.0) / 100.0;
		    
		    log.info("float_part after the rounding is " + float_Part);
		    
		    int floating_point = (int) (Math.pow(10, decimal) * float_Part);
		    
		    log.info("floating_point is " + floating_point);
		    
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
