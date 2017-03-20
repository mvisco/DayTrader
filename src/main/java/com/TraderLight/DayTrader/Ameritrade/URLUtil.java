package com.TraderLight.DayTrader.Ameritrade;


import java.io.DataOutputStream;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class URLUtil {

	public static final Logger log = Logging.getLogger(true);
	
	public URLUtil() {
		
	}
	
	public static String sendURLPostRequest(String urlstr,OrderedHashMap paramOHM) throws IOException
	{
		
	    URL  url = new URL (urlstr);
	    URLConnection  urlConn = url.openConnection();
	    
	    urlConn.setDoInput (true); // Let the run-time system (RTS) know that we want input.
	    urlConn.setDoOutput (true); // Let the RTS know that we want to do output.
	    urlConn.setUseCaches (false); // No caching, we want the real thing.
	    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	    
	    // Send POST output.
		DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());

		StringBuffer data=new StringBuffer();
		
		for(int i=0;i<paramOHM.size();i++){
		  String key=(String) paramOHM.getKey(i);	
		  String value=(String) paramOHM.getValue(i);		
		  data.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));	
		  if(i<paramOHM.size()-1){
			 data.append("&");
		  }
		}
		log.info("data in the post is: " + data.toString());
		
	    printout.writeBytes (data.toString());
	    printout.flush ();
	    printout.close ();
	    
	    // Get response data.		
		String resp=StringHelper.inputStreamtoString(urlConn.getInputStream ());
		log.info("Response is: " + resp);
		return resp;
		}
	
	public static String getfromURL(String str) throws IOException
	{
		URL url=new URL(str);
		return StringHelper.inputStreamtoString(url.openStream());
	}
	
			
}
