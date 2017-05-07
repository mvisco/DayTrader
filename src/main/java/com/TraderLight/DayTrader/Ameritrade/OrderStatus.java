package com.TraderLight.DayTrader.Ameritrade;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class OrderStatus {
	
	public static final Logger log = Logging.getLogger(true);
	public String status;
	
	public OrderStatus() {
		
	}

	public String requestStatus(String order_id) throws IOException {
		
		String str = SessionControl.getUrl()+"100/OrderStatus;jsessionid="+SessionControl.getSessionid()+"?source="+SessionControl.getSourceApp()+"&orderid="+order_id;
		String res=URLUtil.getfromURL(str);		
		log.info("Response is " + res);
		XMLNode root=new XMLNodeBuilder(res).getRoot();
		this.status = root.getChildwithNameNonNull("orderstatus-list").getChildwithNameNonNull("orderstatus").getChildwithName("display-status").getValue();
		log.info("Order Status is  " + this.status);
		return this.status;
	}
	
    public int checkPartialFill(String order_id) throws IOException {
		
    	XMLNode[] fills;
    	String q;
    	int quantity=0;
		String str="https://apis.tdameritrade.com/apps/100/OrderStatus;jsessionid="+SessionControl.getSessionid()+"?source=MAVI"+"&orderid="+order_id;
		String res=URLUtil.getfromURL(str);		
		log.info("Response is " + res);
		XMLNode root=new XMLNodeBuilder(res).getRoot();
		this.status = root.getChildwithNameNonNull("orderstatus-list").getChildwithNameNonNull("orderstatus").getChildwithName("display-status").getValue();
		log.info("Order Status is  " + this.status);
		fills = root.getChildwithNameNonNull("orderstatus-list").getChildwithNameNonNull("orderstatus").getChildrenwithName("fills");
		if (fills.length > 0) {
		    for (int i=0;i < fills.length; i++) {
			    q = fills[i].getChildwithName("fill-quantity").getValue();
			    quantity = quantity + (int)Double.parseDouble(q);
		    }
		}
		return quantity;
	}
	
	
}
