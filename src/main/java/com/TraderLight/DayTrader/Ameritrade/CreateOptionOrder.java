package com.TraderLight.DayTrader.Ameritrade;

import java.io.IOException;

import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;

public class CreateOptionOrder {
	
	
	public static final Logger log = Logging.getLogger(true);
	String order_id = "";
	
	public CreateOptionOrder() {
		
	}
	
	public void createOrder(boolean buy, String amount, String option_symbol, String option_price, String order) throws IOException {
				
		String action;
		String quantity;
		String symbol;
		String ordtype;
		String price;
		String expire;
		String account;
		String specialInstruction;
		
		// For URL encoding remember that = is %3D while ~ is %7E
		
		if (buy) {
			action="action%3Dbuytoopen%7E";
		} else {
			action="action%3Dselltoclose%7E";
		}
		quantity="quantity%3D"+amount+"%7E";
		symbol="symbol%3D"+option_symbol+"%7E";
		if (order.contentEquals("limit")) {			
		    ordtype="ordtype%3DLimit%7E";
		    price="price%3D"+option_price+"%7E";
		    expire="expire%3Dday%7E";
		    // use aon only if quantity is greater than 1 otherwise the order gets rejected
		   int numberOfContracts = Integer.parseInt(amount);
		   if (numberOfContracts == 1) {
			    specialInstruction="spinstructions%3Dnone%7E";
		   } else {
			   specialInstruction="spinstructions%3Daon%7E";
		   }
		} else {
			ordtype="ordtype%3DMarket%7E";
			price="price%3D0%7E";
			specialInstruction="spinstructions%3Dnone%7E";
			expire="expire%3Dday%7E";
		}
		String acct =SessionControl.getAcct(); 
		
		account="accountid%3D"+acct;
		
		String orderString= action+quantity+symbol+ordtype+price+expire+specialInstruction+account;		
		String str = SessionControl.getUrl()+"100/OptionTrade;jsessionid="+SessionControl.getSessionid()+"?source="+SessionControl.getSourceApp()+"&orderstring="+orderString;		
		log.info("Order message sent to AMTD is: " + str);
		String res=URLUtil.getfromURL(str);
		log.info("Response is " + res);
		XMLNode root=new XMLNodeBuilder(res).getRoot();
		this.order_id = root.getChildwithNameNonNull("order-wrapper").getChildwithNameNonNull("order").getChildwithName("order-id").getValue();
		log.info("order-id is " + this.order_id);
		
	}
	
	
	public String getOrderID() {
		
		return(this.order_id);
	}

}
