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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.TraderLight.DayTrader.StockTrader.Logging;;

/**
 *  This class places an order with Option House and store the order_id.
 * 
 * @author Mario Visco
 *
 */
public class CreateStockOrderTM {
	public static final Logger log = Logging.getLogger(true);
	String order_id="";
	
	public void createOrder(boolean buy, String amount, String stock_symbol, String price, String order_type, boolean open) throws IOException {
		
		String action;
		String ordtype;

		    String urlstr = TMSessionControl.getURL() + "services/orderStockService";
	  		String sourceApp = TMSessionControl.getSourceApp();
		    URL  url = new URL (urlstr);
		    URLConnection  urlConn = url.openConnection();
		    
		    urlConn.setDoInput (true); // Let the run-time system (RTS) know that we want input.
		    urlConn.setDoOutput (true); // Let the RTS know that we want to do output.
		    urlConn.setUseCaches (false); // No caching, we want the real thing.
		    urlConn.setRequestProperty("Content-Type", "application/xml");
		    urlConn.setRequestProperty("token", TMSessionControl.getToken());
		    urlConn.setRequestProperty("sourceapp", sourceApp);
		    urlConn.setRequestProperty("JSESSIONID", TMSessionControl.getSessionid());
		    
		    String cookie = "JSESSIONID="+TMSessionControl.getSessionid()+"; " + TMSessionControl.getUUID() + "; " + TMSessionControl.getMonster() +";";
		    /*
		    for (String str : cookies) {
		    	cookie = cookie+ str;
		    }
		    */
		    log.info("Sending message  to url " + urlstr);
		    urlConn.setRequestProperty("Cookie", cookie);
		    log.info("cookie is " + cookie);
		
			DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
			 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			try {
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				
				e.printStackTrace();
			}
	 
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("sendOrder");
			doc.appendChild(rootElement);
	 
			// Account type element
			Element accountType = doc.createElement("accountType");
			accountType.appendChild(doc.createTextNode("MARGIN"));
			rootElement.appendChild(accountType);
			
			// Modify Order Element
			Element modifyOrder = doc.createElement("modifyOrder");
			modifyOrder.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(modifyOrder);
			
			// Original Order ID Element
			Element originalOrderId = doc.createElement("originalOrderId");
			originalOrderId.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(originalOrderId);
			
			// Combine Like Legs Element
			Element combineLikeLegs = doc.createElement("combineLikeLegs");
			combineLikeLegs.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(combineLikeLegs);
			
			// Account number Element
			Element accountNumber = doc.createElement("accountNumber");
			accountNumber.appendChild(doc.createTextNode("4ZE50892"));
			rootElement.appendChild(accountNumber);

			// dispalyQuantity Element
			Element displayQuantity = doc.createElement("displayQuantity");
			displayQuantity.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(displayQuantity);
			
			// gtdate Element
			Element gtdDate = doc.createElement("gtdDate");
			gtdDate.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(gtdDate);
			
			// userId Element
			Element userId = doc.createElement("userId");
			String user_id = TMSessionControl.getuserId();
			userId.appendChild(doc.createTextNode(user_id));
			rootElement.appendChild(userId);			
			
			// limitPrice Element
			Element limitPrice = doc.createElement("limitPrice");
			limitPrice.appendChild(doc.createTextNode(price));
			rootElement.appendChild(limitPrice);
			
			// stop trigger price Element
			Element stopTriggerPrice = doc.createElement("stopTriggerPrice");
			stopTriggerPrice.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(stopTriggerPrice);			

			// trailing amount Element
			Element trailingAmount = doc.createElement("trailingAmount");
			trailingAmount.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(trailingAmount);
			
			// discretion amount Element
			Element discretionAmount = doc.createElement("discretionAmount");
			discretionAmount.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(discretionAmount);	
			
			// offset amount Element
			Element offsetAmount = doc.createElement("offsetAmount");
			offsetAmount.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(offsetAmount);	
			
			// Source Element
			Element source = doc.createElement("source");
			source.appendChild(doc.createTextNode(sourceApp));
			rootElement.appendChild(source);				
			
			// order ID  Element
			Element orderId = doc.createElement("orderId");
			orderId.appendChild(doc.createTextNode("NaN"));
			rootElement.appendChild(orderId);	
			
			// priceType  Element
			String pt = "";
			if (order_type.contentEquals("limit")) {
				pt = "LM";
			} else{
				pt = "MK";
			}
			Element priceType = doc.createElement("priceType");
			priceType.appendChild(doc.createTextNode(pt));
			rootElement.appendChild(priceType);	
			
			// quantity   Element
			Element quantity = doc.createElement("quantity");
			quantity.appendChild(doc.createTextNode(amount));
			rootElement.appendChild(quantity);			

			// holdOrder   Element
			Element holdOrder = doc.createElement("holdOrder");
			holdOrder.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(holdOrder);
		
			// duplicateOrder   Element
			Element duplicateOrder = doc.createElement("duplicateOrder");
			duplicateOrder.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(duplicateOrder);
			
			// discretionFlag   Element
			Element discretionFlag = doc.createElement("discretionFlag");
			discretionFlag.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(discretionFlag);
			
			// solicitedFlag   Element
			Element solicitedFlag = doc.createElement("solicitedFlag");
			solicitedFlag.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(solicitedFlag);
			
			// isBlockOrder   Element
			Element isBlockOrder = doc.createElement("isBlockOrder");
			isBlockOrder.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(isBlockOrder);
			
			// instrumentType   Element
			Element  instrumentType = doc.createElement("instrumentType");
			instrumentType.appendChild(doc.createTextNode("Option"));
			rootElement.appendChild(instrumentType);
			
			// orderLegEntries   Element
			Element  orderLegEntries = doc.createElement("orderLegEntries");
			rootElement.appendChild(orderLegEntries);	
			
			// symbol   Element of orderLogEntries
			Element  symbol = doc.createElement("symbol");
			symbol.appendChild(doc.createTextNode(stock_symbol));
			orderLegEntries.appendChild(symbol);
			
			if (buy) {
				action = "BUY";				
			} else {
				action = "SELL";				
			}			
			if (open) {
				ordtype= "OPEN";
			} else {
				ordtype= "CLOSE";
			}
			
			// orderSide   Element of orderLogEntries
			Element  orderSide = doc.createElement("orderSide");
			orderSide.appendChild(doc.createTextNode(action));
			orderLegEntries.appendChild(orderSide);
			
			// openOrClose   Element of orderLogEntries
			Element  openOrClose = doc.createElement("openOrClose");
			openOrClose.appendChild(doc.createTextNode(ordtype));
			orderLegEntries.appendChild(openOrClose);	
			
			// quantityRatio   Element of orderLogEntries
			Element  quantityRatio = doc.createElement("quantityRatio");
			quantityRatio.appendChild(doc.createTextNode("1"));
			orderLegEntries.appendChild(quantityRatio);	
			
			// instrumentType   Element of orderLogEntries
			Element  instrument = doc.createElement("instrumentType");
			instrument.appendChild(doc.createTextNode("Equity"));
			orderLegEntries.appendChild(instrument);				
			
			// timeInForce   Element
			Element  timeInForce = doc.createElement("timeInForce");
			timeInForce.appendChild(doc.createTextNode("DAY"));
			rootElement.appendChild(timeInForce);
			
			// marketSession   Element
			Element  marketSession = doc.createElement("marketSession");
			marketSession.appendChild(doc.createTextNode("REG"));
			rootElement.appendChild(marketSession);
			
			// noteVo   Element
			Element  noteVo = doc.createElement("noteVo");
			rootElement.appendChild(noteVo);	
			
			//    userId of noteVo
			Element  user = doc.createElement("userId");
			user.appendChild(doc.createTextNode(user_id));
			noteVo.appendChild(user);
			
			//    objectType of noteVo
			Element  objectType = doc.createElement("objectType");
			objectType.appendChild(doc.createTextNode("null"));
			noteVo.appendChild(objectType);	
			
			//    noteText of noteVo
			Element  noteText = doc.createElement("noteText");
			noteVo.appendChild(noteText);	
			
			//    objectIds of noteVo
			Element  objectIds = doc.createElement("objectIds");
			objectIds.appendChild(doc.createTextNode("NaN"));
			noteVo.appendChild(objectIds);	

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			try {
				transformer = tf.newTransformer();
			} catch (TransformerConfigurationException e) {
				
				e.printStackTrace();
			}
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			try {
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			} catch (TransformerException e) {
				
				e.printStackTrace();
			}
			String output = writer.getBuffer().toString().replaceAll("\n|\r", "");

			log.info("data in the post is: " + output);
			
		    printout.writeBytes (output);
		    printout.flush ();
		    printout.close ();
		    
		    // Get the response data.
		    String res = "";
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			while ((line = reader.readLine()) != null) {
			        res += line;
			}
			reader.close();
			log.info("Response is: " + res);
		    
			List<String> inc_cookies = new ArrayList<String>();
			inc_cookies = urlConn.getHeaderFields().get("Set-Cookie");
			TMSessionControl.setCookie(inc_cookies);
		    for (String str : inc_cookies) {
		    	log.info("Cookie " + str);
		    	if (str.contains("monster")) {
		    		
		    		String str1 = str.substring(0, str.indexOf(";"));
		    		log.info(" This is the monster cookie -------------------------------" + str1);
		    		TMSessionControl.setMonster(str1);
		    	}
                if (str.contains("uuid")) {
		    		
		    		String str2 = str.substring(0, str.indexOf(";"));
		    		log.info(" This is the uuid cookie -------------------------------" + str2);
		    		TMSessionControl.setUUID(str2);
		    		
		    	}
		    }
		    			
			if (res.contains("ns1:XMLFault") || !(res.contains("actualOrderId"))) {
				  log.info("got a response error, storing order_id as an empty String");
				  this.order_id = "";
				  return;
		    }
			
            // Parse the xml response and get the order id
		    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			try {
				
				// make a stream out of the response string we got
				InputStream in;
				in = new ByteArrayInputStream(res.getBytes("UTF-8"));				
				XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
				
				// get the actualOrderId.				
				while (eventReader.hasNext()) {					
			        XMLEvent event = eventReader.nextEvent();
			        if (event.isStartElement()) {
			            StartElement startElement = event.asStartElement();			            
			            if (startElement.getName().getLocalPart() == "actualOrderId") {			            	
			            	event = eventReader.nextEvent();
			            	order_id = event.asCharacters().getData();
			            	log.info("order_id is " + order_id);
			            	break;
			            }			            
				     }
			   }
			   //close the input stream
			   in.close();
			} catch (XMLStreamException e) {
				// if there is a problem just print out the exception and keep going
		       log.info("Something went wrong with xml streamer" + e);
			}
					
	}
	
	public String getOrderID() {
		return this.order_id;
	}

}
