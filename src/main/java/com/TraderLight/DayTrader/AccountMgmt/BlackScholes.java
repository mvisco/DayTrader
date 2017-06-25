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
    
 
		
}
