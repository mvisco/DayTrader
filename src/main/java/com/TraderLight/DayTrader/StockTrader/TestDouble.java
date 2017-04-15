package com.TraderLight.DayTrader.StockTrader;

public class TestDouble {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		double price = 123.5;
		double increment = 0.5;
		
		int price_int =(int) (price*100);
		int increment_int = (int) (increment*100);
		System.out.println(price_int);
		System.out.println(increment_int);
        System.out.println(price_int%increment_int);
        System.out.println(price_int/increment_int);
	}

}
