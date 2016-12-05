package com.TraderLight.DayTrader.AccountMgmt;


public class BlackScholes {

    // Black-Scholes formula
    public static double callPrice(double S, double X, double r, double sigma, double T) {
        double d1 = (Math.log(S/X) + (r + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
       // System.out.println("d1 is " + d1);
       // System.out.println("d2 is " + d2);
        return S * Gaussian.Phi(d1) - X * Math.exp(-r * T) * Gaussian.Phi(d2);
    }

    public static double putPrice(double S, double X, double r, double sigma, double T) {
        double d1 = (Math.log(S/X) + (r + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        //System.out.println("d1 is " + d1);
        //System.out.println("d2 is " + d2);
        return (-S * Gaussian.Phi(-d1)) + X * Math.exp(-r * T) * Gaussian.Phi(-d2);
    }
    
    // Use these ones when you have stock with dividend d expressed as annual percentage 
    public static double generalizedCallPrice(double S, double X, double r, double sigma, double T, double d) {
        double d1 = (Math.log(S/X) + (r -d + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        //System.out.println("d1 is " + d1);
        //System.out.println("d2 is " + d2);
        return ( (S * Math.exp(-d * T) * Gaussian.Phi(d1)) - X * Math.exp(-r * T) * Gaussian.Phi(d2) );
    }  
    public static double generalizedPutPrice(double S, double X, double r, double sigma, double T, double d) {
        double d1 = (Math.log(S/X) + (r -d + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));
        double d2 = d1 - sigma * Math.sqrt(T);
        //System.out.println("d1 is " + d1);
        //System.out.println("d2 is " + d2);
        return (-(S * Math.exp(-d * T) * Gaussian.Phi(-d1))) + X * Math.exp(-r * T) * Gaussian.Phi(-d2);
    }
    
    public static double callDelta (double S, double X, double r, double sigma, double T, double d) {
    	
    	double d1 = (Math.log(S/X) + (r -d + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));   	
    	return Gaussian.Phi(d1);
    	
    }
    
    public static double putDelta (double S, double X, double r, double sigma, double T, double d) {
    	
    	double d1 = (Math.log(S/X) + (r -d + sigma * sigma/2) * T) / (sigma * Math.sqrt(T));
    	return (Gaussian.Phi(d1)-1);
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
        double S     = 1125;
        double Xcall     = 1120;
        double Xput = 1130;
        double r     = 0.03;
        double sigmaCall = 0.12;
        double sigmaPut = 0.12;
        double expirationindays=0.01;
        double numberofdaysinyear=365;
        double T     = (expirationindays/numberofdaysinyear);
        double d = 0.0;
        double paidCall = 20;
        double paidPut = 20.30;
        
        // Short Straddle Parameters
        double S1     = S;
        double X1     = 1125;
        double r1     = 0.03;
        double sigma1 = 0.12;
        double expirationindays1=0.01;
        double numberofdaysinyear1=365;
        double T1     = (expirationindays1/numberofdaysinyear1);
        double d1 = 0.0;
        double receivedforCall = 6;
        double receivedforPut = 6.30;
        
        System.out.println("Current price of the stock is: " + S);
        
       // Long Straddle
        double valueLongStraddleBought = paidCall + paidPut;
        double CallPrice = callPrice(S, Xcall, r, sigmaCall, T);
        double PutPrice = putPrice(S, Xput, r, sigmaPut, T);
        double currentLongStraddleValue = CallPrice + PutPrice;
        
        System.out.println("Long Straddle Paramter: Current Call Price " + CallPrice + "Current Put Price: " + PutPrice + " Value paid : " + valueLongStraddleBought + 
        		"Current value :" +  currentLongStraddleValue);
        
        //Short Straddle
        double valueShortStraddleReceived = receivedforCall + receivedforPut;
        double CallPrice1 = callPrice(S1, X1, r1, sigma1, T1);
        double PutPrice1 = putPrice(S1, X1, r1, sigma1, T1);
        double currentShortStraddleValue = CallPrice1 + PutPrice1;
        
        
        System.out.println("Short  Straddle Paramter: Current Call Price: " + CallPrice1 + "Current Put Price: " + PutPrice1 + " Value received: " +  -valueShortStraddleReceived + 
        		"Current value :" +  -currentShortStraddleValue); 
        System.out.println("Difference between long straddle value and short straddle value" + (currentLongStraddleValue-currentShortStraddleValue));
        
        calendarStraddle(valueLongStraddleBought, currentLongStraddleValue, valueShortStraddleReceived, currentShortStraddleValue, 500 );
        
        
        
       // straddleCalculation(paidCall, paidPut, 500, generalizedCallPrice(S, X, r, sigma, T, d), generalizedPutPrice(S, X, r, sigma, T, d) );        

    }




		public static void straddleCalculation( double paidCall, double paidPut, int quantity, double currentCallValue, double CurrentPutValue) {
						
			// Calculate gain or loss
			double paidCallPrice = (double)quantity * paidCall;
			double paidPutPrice = (double)quantity * paidPut;
			double currentCallPrice = currentCallValue * (double)quantity;
			double currentPutPrice = CurrentPutValue * (double)quantity;
			double transactionCost = 7.99 + ((quantity/100) * 0.75);
			
			double Gain = (currentCallPrice - paidCallPrice) + (currentPutPrice - paidPutPrice ) - 2*transactionCost;
			System.out.println("Gain is: " + Gain);
					
			
		}
		
		public static void calendarStraddle (double valueStraddleBought, double currentValueLongStraddle, double valueStraddleSold, double currentValueShortStraddle, int quantity) {
			
			double StraddletransactionCost = 9.99 + ((quantity/100) * 0.75);
			double Gain = ((currentValueLongStraddle - valueStraddleBought) + (-currentValueShortStraddle + valueStraddleSold ))*(quantity) - 2*StraddletransactionCost;
			System.out.println("Gain is: " + Gain);
				
			
		}
		
		
		
}
