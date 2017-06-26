package com.TraderLight.DayTrader.StockTrader;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.Ameritrade.CancelOrder;
import com.TraderLight.DayTrader.Ameritrade.CreateOptionOrder;
import com.TraderLight.DayTrader.Ameritrade.GetQuoteAmeritrade;
import com.TraderLight.DayTrader.Ameritrade.KeepAliveAmeritrade;
import com.TraderLight.DayTrader.Ameritrade.LoginAmeritrade;
import com.TraderLight.DayTrader.Ameritrade.OrderStatus;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.StockTrader.SystemConfig;

public class TestAmeritrade {
	
     public static final Logger log = Logging.getLogger(true);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SystemConfig.populateSystemConfig("config.properties");
        SystemConfig sysconfig = SystemConfig.getSystemConfig();
        
        LoginAmeritrade a = new LoginAmeritrade(sysconfig.TDLogin, sysconfig.TDPassword, 
				sysconfig.TDAuthURL, sysconfig.TDSourceApp, sysconfig.TDAcct);
		GetQuoteAmeritrade b = new GetQuoteAmeritrade();
		CreateOptionOrder c = new CreateOptionOrder();
		OrderStatus d = new OrderStatus();
		CancelOrder f = new CancelOrder();
		//KeepAliveAmeritrade g = new KeepAliveAmeritrade();
		
		try {
			a.login();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			b.getQuotes("MSFT_062317P70",true,false, 0.1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
/*		
		try {
			g.keepAlive();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			c.createOrder(true,"1","GOOG_020714C1165","3","768684601");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			d.requestStatus(c.getOrderID());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			f.requestOrderCancel(c.getOrderID());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		
		try {
			d.requestStatus(c.getOrderID());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/		
		try {
			a.logout();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
