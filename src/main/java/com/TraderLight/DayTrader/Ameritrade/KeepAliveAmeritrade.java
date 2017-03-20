package com.TraderLight.DayTrader.Ameritrade;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class KeepAliveAmeritrade {
	
	public static final Logger log = Logging.getLogger(true);    
    private String urlstr;
    
	public KeepAliveAmeritrade(String urlstr) {
		this.urlstr=urlstr;
	}
		
	public boolean  keepAlive() throws IOException  {
		
		String str = urlstr+"KeepAlive;jsessionid="+SessionControl.getSessionid()+"?source="+SessionControl.getSourceApp();
		String res=URLUtil.getfromURL(str);		
		log.info("Response is " + res);
		if (res.contentEquals("LoggedOn")) {
			return true;
		} else {
			return false;
		}
	}

}
