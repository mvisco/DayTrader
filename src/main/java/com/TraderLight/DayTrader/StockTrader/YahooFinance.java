package com.TraderLight.DayTrader.StockTrader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

public class YahooFinance {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String initialDate = "2016-11-29";
		String finalDate = "2016-12-05";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
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
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(iDate);
		
		
		// if Monday go get the Friday before. Remember that the field 7 is the DAY_OF_THE_WEEK
		if (cal.get(7) == 2) {
			cal.add(7, -3);
		} else {
			// just get the day before
			cal.add(7, -1);
		}
		
		System.out.println(cal.toString());
		System.out.println(cal.getTime());
		System.out.println(cal.get(7));
		
		int day = cal.get(Calendar.DATE);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		
		System.out.println( day + " " + month + " " + year);
		
		//String str="http://ichart.finance.yahoo.com/table.csv?s=QQQ&a=10&b=25&c=2016&d=10&e=02&f=2016&g=d";
		String str = "http://ichart.finance.yahoo.com/table.csv?s="+"QQQ"+"&a="+month+"&b="+day+"&c="+year+"&d="+month+"&e="+day+"&f="+year;
		URL url=null;
		try {
			url = new URL(str);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			System.out.println(inputStreamtoString(url.openStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("res is " + res);
		//log.info("Response is " + res);

		String str_response="";
		try {
			str_response = inputStreamtoString(url.openStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		String delim = " \n,";
		StringTokenizer st2 = new StringTokenizer(str_response, delim);
		int n=0;
		while (st2.hasMoreElements()) {
			System.out.println(st2.nextElement());
			if ( n == 11) {
				double d = Double.parseDouble((String)st2.nextElement());
				System.out.println("This is it " + d);
			}
			n++;
			//response.add((String)st2.nextElement());
		}

	}
	
	public static String inputStreamtoString(InputStream fi) throws IOException
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

}
