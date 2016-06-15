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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

public class CancelOrderTM {
	
	public static final Logger log = Logging.getLogger(true);
	
	public CancelOrderTM() {
		
	}
	
	public void cancelOrder(String order_id) throws IOException {
		
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
			Element rootElement = doc.createElement("cancelOrder");
			rootElement.appendChild(doc.createTextNode(order_id));
			doc.appendChild(rootElement);
			
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
			
		
	}

}
