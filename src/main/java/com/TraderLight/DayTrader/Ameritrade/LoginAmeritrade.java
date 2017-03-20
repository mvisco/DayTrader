package com.TraderLight.DayTrader.Ameritrade;

import java.io.IOException;

import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;

public class LoginAmeritrade {
	
    public static final Logger log = Logging.getLogger(true);	
    private String username;
    private String password;
    private String urlstr;
    private String sourceApp;
    

    public LoginAmeritrade(String username, String password, String urlstr, String sourceApp) {
    	this.username = username;
    	this.password = password;
    	this.urlstr = urlstr;
    	this.sourceApp = sourceApp;
    }
	
	public  void login() throws IOException {
		
		  OrderedHashMap ohm=new OrderedHashMap();
		  ohm.put("userid",username);
		  ohm.put("password",password);
		  ohm.put("source",sourceApp);  
		  ohm.put("version","1.0");
		  
		  String url = urlstr+"100/LogIn?"+"source="+sourceApp+"&version=1.0";
		  log.info("Sending login request " );
		  String res=URLUtil.sendURLPostRequest(url,ohm);
		  
		  XMLNode root=new XMLNodeBuilder(res).getRoot();
		  
		  SessionControl.setSessionid(root.getChildwithNameNonNull("xml-log-in").getChildwithNameNonNull("session-id").getValue());
		  SessionControl.setSegment(root.getChildwithNameNonNull("xml-log-in").getChildwithNameNonNull("accounts").getChildwithName("account").getChildwithNameNonNull("segment").getValue());
		  SessionControl.setCompany(root.getChildwithNameNonNull("xml-log-in").getChildwithNameNonNull("accounts").getChildwithName("account").getChildwithNameNonNull("company").getValue());
		  SessionControl.setSourceApp(sourceApp);
		  SessionControl.setUrl(urlstr);
		}
	
	public  void logout() throws IOException {
				
		String url = "https://apis.tdameritrade.com/apps/100/LogOut?source=MAVI";
		OrderedHashMap ohm=new OrderedHashMap();
		URLUtil.sendURLPostRequest(url,ohm);		
	}
	
	
	
	
	

}
