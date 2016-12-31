package com.TraderLight.DayTrader.StockTrader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class IronCondorSimulation {
	
    public static final Logger log = Logging.getLogger(true);	
	
    public static void main(String[] args) {
		
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
		String initialDate = "2016-12-12";
		String finalDate = "2016-12-16";
		String symbol = "QQQ";
		Date iDate=null;
		try {
			iDate = sdf.parse(initialDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Date fDate=null;
		try {
			fDate = sdf.parse(finalDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double iClose = getClose(iDate, symbol);
		double fClose = getClose(fDate, symbol);
		
		
		
		
    }
    
    
	 public static double getClose(Date date, String symbol ) {
			
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
		String str = "http://ichart.finance.yahoo.com/table.csv?s="+symbol+"&a="+month+"&b="+day+"&c="+year+"&d="+month+"&e="+day+"&f="+year;
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
			//log.info("Content for the Yahoo Query for symbol "+ symbol);
			//log.info(next);
			if ( n == 11) {
				d = Double.parseDouble((String)st2.nextElement());
				log.info("This is the previous price " + d + " for symbol " + symbol);
			}
			n++;
			//response.add((String)st2.nextElement());
		}
		return d;
		
		
	}
	 
	private static  String inputStreamtoString(InputStream fi) throws IOException {
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

}
