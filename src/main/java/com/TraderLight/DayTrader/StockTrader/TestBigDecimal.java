package com.TraderLight.DayTrader.StockTrader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TestBigDecimal {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		final int STRIKESNUMBER = 30; // we will have 61 strikes
		List<BigDecimal> strikes = new ArrayList<BigDecimal>();
		double strike_increment = 2.5;
		double price  = 850.28;
		List<String> expirations = new ArrayList<String>();

		BigDecimal priceBD = new BigDecimal(String.valueOf(price));
		BigDecimal increment = new BigDecimal(String.valueOf(strike_increment));
		RoundingMode rm1;
		rm1 = RoundingMode.valueOf("HALF_DOWN");		
		BigDecimal price_new = priceBD.setScale(0, rm1);

		if (strike_increment == 2.5) {
			//for options that have a 2.5 increment we have to start from a multiple of 5. For example if we GOOGL price 
			// set at 852 we will start from 850 to build the lost of strikes. For other options we can start anywhere on the 
			// int version of price for ex. AAPL at 135.33 we will start at 135 and increment either by 1 or by 0.5
			price_new = price_new.subtract(price_new.remainder(new BigDecimal("5.0")));
		}

		for (int i = -STRIKESNUMBER; i <=STRIKESNUMBER; i++) {
			strikes.add(price_new.add(increment.multiply(new BigDecimal(i))));
			
		}
		
		for (BigDecimal bc : strikes) {
			//BigDecimal new_strike;
			BigDecimal new_strike = new BigDecimal(String.valueOf(bc.stripTrailingZeros()));
			System.out.println(bc);
			System.out.println(new_strike);
			System.out.println(new_strike.toPlainString());
		}
		
		
		final int EXPIRATIONSNUMBER = 10; // we will have 11 expiration dates
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Calendar rightnow = Calendar.getInstance();
		//start from  the first day of current week
		rightnow.add(Calendar.DAY_OF_WEEK, rightnow.getFirstDayOfWeek()-rightnow.get(Calendar.DAY_OF_WEEK));
		// get the friday of this week
		rightnow.add(Calendar.DAY_OF_MONTH, 5);
		for (int i = 0; i <= EXPIRATIONSNUMBER; i++) {
			// add this Friday and next 10 Fridays to the list
			rightnow.add(Calendar.DAY_OF_MONTH, i*7);
			expirations.add(sdf.format(rightnow.getTime()));
			System.out.println(expirations.get(i));
		}
	}

	}


