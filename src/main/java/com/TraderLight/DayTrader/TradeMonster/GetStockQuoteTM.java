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
import com.TraderLight.DayTrader.StockTrader.Logging;

/**
 *  This class queries Options House for a stock quote (bid and ask) .
 * 
 * @author Mario Visco
 *
 */

public class GetStockQuoteTM {
	
	public static final Logger log = Logging.getLogger(true);
	String bid;
	String ask;
	
	public GetStockQuoteTM() {
		
	}
	
	public  String  getStockQuote(String symbol) throws IOException {
			
		  
		    String urlstr = TMSessionControl.getURL() + "services/quotesService";
		  		  		  
		    URL  url = new URL (urlstr);
		    URLConnection  urlConn = url.openConnection();
		    
		    urlConn.setDoInput (true); // Let the run-time system (RTS) know that we want input.
		    urlConn.setDoOutput (true); // Let the RTS know that we want to do output.
		    urlConn.setUseCaches (false); // No caching, we want the real thing.
		    urlConn.setRequestProperty("Content-Type", "application/xml");
		    urlConn.setRequestProperty("token", TMSessionControl.getToken());
		    urlConn.setRequestProperty("JSESSIONID", TMSessionControl.getSessionid());
		    
		    String cookie = "JSESSIONID="+TMSessionControl.getSessionid()+"; " + TMSessionControl.getUUID() + "; " + TMSessionControl.getMonster() +";";
		    log.info("Sending message  to url " + urlstr);
		    log.info("This is the cookie used in sending GetQuoteTM()" + cookie);
		    urlConn.setRequestProperty("Cookie", cookie);	    
	    
		    // Send POST output.
			DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
			 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			try {
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {				
				log.info("Something went wrong with the builder" + e);				
			}
	 
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("getQuotes");
			doc.appendChild(rootElement);
	 
			// item elements
			Element item = doc.createElement("item");
			rootElement.appendChild(item);
	 
	 
			// symbol element
			Element symbol1 = doc.createElement("symbol");
			symbol1.appendChild(doc.createTextNode(symbol));
			item.appendChild(symbol1);
	 
			// instrumentType elements
			Element instrument = doc.createElement("instrumentType");
			instrument.appendChild(doc.createTextNode("Equity"));
			item.appendChild(instrument);
			
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
				
				log.info("Something went wrong with transformer " + e);
				
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
					    
		    if (res.contains("ns1:XMLFault") || !(res.contains("item"))) {
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

            // Parse the xml response and get the bid and ask
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
			            // Looking for the askPrice
			            if (startElement.getName().getLocalPart() == "askPrice") {
			            	event = eventReader.nextEvent();
			            	if (event.asStartElement().getName().getLocalPart() == "amount") {
			            		event = eventReader.nextEvent();
			            		ask = event.asCharacters().getData();
			            	}
			            	continue;
			            }
			            
				     }
			        if (event.isStartElement()) {
			            StartElement startElement = event.asStartElement();
			            // If we have an item element, we create a new item
			            if (startElement.getName().getLocalPart() == "bidPrice") {
			            	log.info("Found bid");
			            	event = eventReader.nextEvent();
			            	if (event.asStartElement().getName().getLocalPart() == "amount") {
			            		event = eventReader.nextEvent();
			            		bid = event.asCharacters().getData();
			            		log.info(event.asCharacters().getData());
			            	}
			            	continue;
			            }			            
				     }
			   }
			   //close the input stream
			   in.close();
			} catch (XMLStreamException e) {
				// if there is a problem just print out the exception and keep going
		       log.info("Something went wrong with xml streamer" + e);
			}
            
		    log.info("bid and ask " + bid + " " + ask);
		    return bid;
	}

}
