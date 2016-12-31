package com.TraderLight.DayTrader.StockTrader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;



public class YahooTest {
	
	
	public static void main(String[] args) throws IOException {
		
		String str="http://ichart.finance.yahoo.com/table.csv?s=QQQ&a=10&b=25&c=2016&d=11&e=02&f=2016&g=d";
		URL url=null;
		try {
			url = new URL(str);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(inputStreamtoString(url.openStream()));
		  //System.out.println("res is " + res);
		  //log.info("Response is " + res);
		
		String str_response = inputStreamtoString(url.openStream());		
		String delim = " \n,";
		StringTokenizer st2 = new StringTokenizer(str_response, delim);
		
		List<String> response = new ArrayList<String>();
       
		while (st2.hasMoreElements()) {
			//System.out.println(st2.nextElement());
			response.add((String)st2.nextElement());
		}
	
		List<Double> listOfClose = new ArrayList<Double>();		
		for (int i = 1; i < (response.size() -1); i++ ) {
			//System.out.println(response[i]);
			if ( i >= 13) {
				if ( (i == 13) || ( ((i -13) % 7) == 0)) {
					//System.out.println("response in loop " + i);
					
					//System.out.println(response.get(i-1));
					listOfClose.add(Double.parseDouble(response.get(i-1)));
				}
			}
		}
		
		listOfClose = revertList(listOfClose);
		
		for (Double d : listOfClose) {
			System.out.println(d);
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

	public static List<Double> revertList(List<Double> l) {
		
		List<Double> l1 = new ArrayList<Double>();	
		
		for (int j = (l.size() -1) ; j >= 0 ; j--) {
			l1.add(l.get(j));
		}
		
		l = l1;
		
		return l1;
		
	}
}
