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

import com.TraderLight.DayTrader.StockTrader.Logging;

public class RequestOrderStatusTM {
	
	String filledQuantity;
	String status;
	String averageFilledPrice;  // on short positions this is negative
	
	public static final Logger log = Logging.getLogger(true);
	
	public RequestOrderStatusTM() {
		
	}
	
	public String requestOrderStatus(String order_id) throws IOException {
		
		  String urlstr ="https://services.optionshouse.com/services/orderBookService";
  		  
		    URL  url = new URL (urlstr);
		    URLConnection  urlConn = url.openConnection();
		    
		    urlConn.setDoInput (true); // Let the run-time system (RTS) know that we want input.
		    urlConn.setDoOutput (true); // Let the RTS know that we want to do output.
		    urlConn.setUseCaches (false); // No caching, we want the real thing.
		    urlConn.setRequestProperty("Content-Type", "application/xml");
		    urlConn.setRequestProperty("token", TMSessionControl.getToken());
		    urlConn.setRequestProperty("JSESSIONID", TMSessionControl.getSessionid());
		    urlConn.setRequestProperty("sourceapp", TMSessionControl.getSourceApp());
		    String cookie = "JSESSIONID="+TMSessionControl.getSessionid()+"; " + TMSessionControl.getUUID() + "; " + TMSessionControl.getMonster() +";";
		    /*
		    for (String str : cookies) {
		    	cookie = cookie+ str;
		    }
		    */
		    urlConn.setRequestProperty("Cookie", cookie);
		    log.info("cookie is " + cookie);
		    log.info("Sending message  to url " + urlstr);
			DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
			 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			try {
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("loadSpecifiedOrders");
			doc.appendChild(rootElement);
			
			Element orderIds = doc.createElement("orderIds");
			orderIds.appendChild(doc.createTextNode(order_id));
			rootElement.appendChild(orderIds);
			
			Element filtersOn = doc.createElement("filtersOn");
			filtersOn.appendChild(doc.createTextNode("false"));
			rootElement.appendChild(filtersOn);
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			try {
				transformer = tf.newTransformer();
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			try {
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
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
			
			if (res.contains("ns1:XMLFault") || !(res.contains("orderList"))) {
				  log.info("got a response error, returning an empty String");
				  return "";
		    }
			
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

            // Parse the xml response and get the status, filled quantity, and filled price
		    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			try {
				
				// make a stream out of the response string we got
				InputStream in;
				in = new ByteArrayInputStream(res.getBytes("UTF-8"));				
				XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
				
				// go through all the nodes to look for what we need.				
				while (eventReader.hasNext()) {					
			        XMLEvent event = eventReader.nextEvent();			        
			        if (event.isStartElement()) {
			            StartElement startElement = event.asStartElement();
			            // Looking for the status
			            if (startElement.getName().getLocalPart() == "status") {
			            	event = eventReader.nextEvent();
			            	status = event.asCharacters().getData();
			            	log.info("Status is " + status);
			            	continue;
			            }
			            
			            //looking for fillQuantity, there are 2 of them but they should be the same
                        if (startElement.getName().getLocalPart() == "fillQuantity") {
			            	
			            	event = eventReader.nextEvent();
			            	filledQuantity = event.asCharacters().getData();	
			            	log.info("Filled Quantity is " + filledQuantity);
			            	continue;
			            }
                        
                        // looking for average fill price
                        if (startElement.getName().getLocalPart() == "averageFillPrice") {
			            	log.info("Found bid");
			            	event = eventReader.nextEvent();
			            	if (event.asStartElement().getName().getLocalPart() == "amount") {
			            		event = eventReader.nextEvent();
			            		averageFilledPrice = event.asCharacters().getData();
			            		if (Double.parseDouble(averageFilledPrice) < 0) {
			            			// when selling the price comes as a negative number, make it positive
			            			averageFilledPrice = String.valueOf(-Double.parseDouble(averageFilledPrice));
			            		}
			            		log.info("filled Price is " + averageFilledPrice);
			            		// we are done
			            		break;
			            	}
			            
				         }
			        }

			   }
			  
			   //close the input stream
			   in.close();
			} catch (XMLStreamException e) {
				// if there is a problem just print out the exception and keep going
		       log.info("Something went wrong with xml streamer" + e);
			}
		    
			return status;	
		
	}
	
	public String getFilledQuantity() {
		return filledQuantity;
	}
	
	public String getAverageFilledPrice() {
		return averageFilledPrice;
	}

}
