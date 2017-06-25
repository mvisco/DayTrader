package com.TraderLight.DayTrader.AccountMgmt;



import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;


public class OptionCostProvider {
	
	public static final Logger log = Logging.getLogger(true);
	double r = 0.02; //assume r constant
	double d = 0; //assume dividend yield 0 for all stocks
		
	
	public OptionCostProvider() {
		
	}
	
	
	 public double getCallCost(double S, double X, double T, double sigma) {
		
		//S is the current stock price
		// X is exercise price
		// T is in days of fraction of days
		// log.info("Getting Call Price: " + "S " + S + " X " + X + " T "+ T);
		
		return BlackScholes.generalizedCallPrice(S, X, r, sigma, T, d);
		
	}
	
	public double getPutCost(double S, double X, double T, double sigma) {
		
		//S is the current stock price
		// X is exercise price
		// T is in days of fraction of days
		//log.info("Getting Put Price: " + "S " + S + " X " + X + " T "+ T);
		return BlackScholes.generalizedPutPrice(S, X, r, sigma, T, d);
		
	}
	
	public double getCallDelta(double S, double X, double T, double sigma) {
				
		return BlackScholes.callDelta(S, X, r, sigma, T, d);
	}
	
	public double getPutDelta(double S, double X, double T, double sigma) {
		
		return BlackScholes.putDelta(S, X, r, sigma, T, d);
	}
	
    public static void main(String[] args) {
    	/* S: share Price
    	 * X: strike price
    	 * r: risk free interest rate
    	 * sigma: volatility
    	 * T: time until expiration in years
    	 * d: dividend percent in year
    	 */
    	// Long Straddle Parameters
        double S     = 1120;
        double Xcall     = 1115;
        double Xput = 1125;
        double expirationindays1=1;
        double numberofdaysinyear1=365;
        double T1     = (expirationindays1/numberofdaysinyear1);
        double sigma = 0.30;
        OptionCostProvider optionCost = new OptionCostProvider();
        System.out.println("Current price of the call is: " + optionCost.getCallCost(S,Xcall,T1,sigma));
        System.out.println("Current price of the put  is: " + optionCost.getPutCost(S,Xput,T1,sigma));
        System.out.println("Current delta of the call  is: " + optionCost.getCallDelta(S,Xcall,T1,sigma));
        System.out.println("Current delta of the put   is: " + optionCost.getPutDelta(S,Xput,T1,sigma));
        
    }

}
