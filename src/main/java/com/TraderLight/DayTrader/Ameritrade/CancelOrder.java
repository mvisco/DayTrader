package com.TraderLight.DayTrader.Ameritrade;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;

public class CancelOrder {
	
	public static final Logger log = Logging.getLogger(true);
	public String status;
	
	public CancelOrder() {
		
	}

	public void requestOrderCancel(String order_id) throws IOException {
		
		String str = SessionControl.getUrl()+"100/OrderCancel;jsessionid="+SessionControl.getSessionid()+"?source="+SessionControl.getSourceApp()+"&orderid="+order_id;		
		String res=URLUtil.getfromURL(str);		
		log.info("Response is " + res);
		XMLNode root=new XMLNodeBuilder(res).getRoot();
		String message = root.getChildwithNameNonNull("cancel-order-messages").getChildwithNameNonNull("order").getChildwithName("message").getValue();
		log.info("Order Cancellation message is "  + message);
	}
	
}
