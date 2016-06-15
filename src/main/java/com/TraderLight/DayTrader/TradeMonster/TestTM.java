/*
 * Copyright 2016 Mario Visco, TraderLight LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 **/
package com.TraderLight.DayTrader.TradeMonster;

import java.io.IOException;
import org.apache.log4j.Logger;


import com.TraderLight.DayTrader.StockTrader.Logging;

public class TestTM {
	
     public static final Logger log = Logging.getLogger(true);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		 LoginTradeMonster a = new LoginTradeMonster("x", "y", "z", "j");
		//GetQuoteTM b = new GetQuoteTM();	
		
		GetStockQuoteTM b = new GetStockQuoteTM();
		//CreateSpreadOrderTM spread = new CreateSpreadOrderTM();
		
		//CreateOptionOrderTM c = new CreateOptionOrderTM();
		CreateStockOrderTM c1 = new CreateStockOrderTM();
		CancelOrderTM f = new CancelOrderTM();
		RequestOrderStatusTM d = new RequestOrderStatusTM();
		/*
		
		
		KeepAliveAmeritrade g = new KeepAliveAmeritrade();
		AMTDOptionSeries h = new AMTDOptionSeries();
		*/
		
		
		try {
			a.login();		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		/*
		try {
			String res = h.requestOptionSeries("AAPL");
			h.parseOptionSeries(res);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/	
		
		
       
		try {
			String bid =b.getStockQuote("AAPL");
			//String quote = b.getQuotes("AMZNH2815C535000",true,false);
			
			log.info(bid );
			// String quote1 = b.getQuotes("AMZNH2815C540000",true,false);
			// String bid1 = b.getBid();
			// String ask1 = b.getAsk();
			 //log.info("BIDS AND ASKS ");
			 //log.info(bid + " " + ask + " " +bid1 + " " + ask1);
			 
			// double spread_price = Double.valueOf(quote) - Double.valueOf(quote1);
			 //price_string = String.valueOf(spread_price);
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
				
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
/*		
		String order_id = " ";
		
	
		try {
			 spread.createOrder(true,"1","AMZNG2415C400000","AMZNG2415C400500",price_string,"limit",true);
			 order_id = spread.getOrderID();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
*/	
		/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/		
        String order_id = "";
		try {
			 c1.createOrder(false,"100","QQQ","1.0","market",true);
			 order_id = c1.getOrderID();
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
			d.requestOrderStatus(order_id);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
/*		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*/		
		try {
			f.cancelOrder(order_id);
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
			d.requestOrderStatus(order_id);
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
		/*
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
