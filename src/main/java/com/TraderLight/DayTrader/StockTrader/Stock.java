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

package com.TraderLight.DayTrader.StockTrader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.Strategy.Strategy;

/**
 *  This class creates the stocks that we will be trading in a trading sessions.
 * 
 * @author Mario Visco
 *
 */

public class Stock {
	
	public static final Logger log = Logging.getLogger(true);
	String symbol;
	double change;
	double profit;
	double loss;
	int lot;
	boolean closePosition;
	boolean openLongPosition;
	boolean openShortPosition;
	boolean openLongPositionWithPrice;
	double priceForLongPosition;
	boolean openShortPositionWithPrice;	
	double priceForShortPosition;
	boolean tradeable;
	int strategyID;
	double price;
	double strike_increment;
	double impVol;
	static long modifiedTime=0L;
	Strategy strategy;
	List<Integer> volumeVector;
	static List<Stock> listOfStocks = new ArrayList<Stock>(); // contains all the stocks of interest for the trading session
    List<BigDecimal> strikes = new ArrayList<BigDecimal>();
    List<String> expirations = new ArrayList<String>();
	
	private Stock() {
		
	}
	
	// Getters Definition	
	public String getSymbol() {
		return this.symbol;
	}
	
	public double getChange() {
		return change;
	}
	
	public double getProfit() {
		return profit;
	}
	
	public double getLoss() {
		return loss;
	}
		
	public boolean getClosePosition() {
		return closePosition;
	}
	
	public boolean getOpenLongPosition() {
		return openLongPosition;
	}
	
	public boolean getOpenShortPosition() {
		return openShortPosition;
	}
	
	public boolean getTradeable() {
		return tradeable;
	}
	
	public int getLot() {
		return lot;
	}
	
	public double getPriceForLongPosition() {
		return priceForLongPosition;
	}
	
	public double getPriceForShortPosition() {
		return priceForShortPosition;
	}
	
	public List<Integer> getVolumeVector() {
		return volumeVector;
	}
	
	public Strategy getStrategy() {
		return strategy;
	}
	
	public static List<Stock> getListOfStocks() {
		return listOfStocks;
	}
	
	public int getStrategyID() {
		return strategyID;
	}
		
	// Setters Definition	
	public void setVolumeVector(List<Integer> volumeVector) {
		this.volumeVector=volumeVector;
	}
	
	public void setSymbol(String symbol) {
		 this.symbol = symbol;
	}
	
	public void  setChange(double change) {
		this.change = change;
	}
	
	public void setProfit(double profit) {
		this.profit =  profit;
	}
	
	public void setLoss(double loss) {
		this.loss =  loss;
	}
	
	
	public void setClosePosition(boolean closePosition) {
		this.closePosition=closePosition;
	}
	
	public void setOpenLongPosition(boolean openLongPosition) {
		this.openLongPosition = openLongPosition;
	}
	
	public void setOpenShortPosition(boolean openShortPosition) {
		this.openShortPosition = openShortPosition;
	}
	
	public void setPriceForLongPosition(double priceForLongPosition) {
		this.priceForLongPosition = priceForLongPosition;
	}
	
	public void setPriceForShortPosition(double priceForShortPosition) {
		this.priceForShortPosition = priceForShortPosition;
	}
	
	public void setTradeable(boolean tradeable) {
		this.tradeable = tradeable;
	}
	
	public void setLot(int lot) {
		this.lot = lot;
	}
	
	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
	}
	
	/**
	 * This method populate listOfStocks with all the stocks tracked during the trading session.
	 * <p>
	 * It uses an xml configuration file located in the project root
	 * as a descriptor for the all the tracked stocks. For each stock in the XML file it creates a stock object and adds it to the List.
	 * Normally gets called by main() to get all the tracked stocks for the trading session in the list.
	 * <p>
	 * @author Mario Visco
	 *
	 * @param XML File Name
	 */
	
	public static void populateStock(String fileName) {
		
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		Stock stock = null;
		
		// store the last time the xml file has been modified
		File file = new File(fileName);
        Path filePath = file.toPath();
        try
        {
        	BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        	modifiedTime=attributes.lastModifiedTime().toMillis();
        }
        catch (IOException e)
        {
            log.info(fileName + " Cannot get file attributes");
  	        e.printStackTrace();     
        }
	    
		try {
			
			InputStream in = new FileInputStream(fileName);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
						
			while (eventReader.hasNext()) {
		        XMLEvent event = eventReader.nextEvent();
		        
		        if (event.isStartElement()) {
		            StartElement startElement = event.asStartElement();
		            // If we have an item element, we create a new item
		            if (startElement.getName().getLocalPart() == "stock") {
		            	 stock = new Stock();
		              continue;
		            }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("symbol")) {
		        		event = eventReader.nextEvent();
		                stock.symbol = event.asCharacters().getData();
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("change")) {
		        		event = eventReader.nextEvent();
		                stock.change = Double.valueOf(event.asCharacters().getData());		                
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("profit")) {
		        		event = eventReader.nextEvent();
		                stock.profit = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("loss")) {
		        		event = eventReader.nextEvent();
		                stock.loss = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("lot")) {
		        		event = eventReader.nextEvent();
		                stock.lot = Integer.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("closePosition")) {
		        		event = eventReader.nextEvent();
		                stock.closePosition = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("openLongPosition")) {
		        		event = eventReader.nextEvent();
		                stock.openLongPosition = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("openShortPosition")) {
		        		event = eventReader.nextEvent();
		                stock.openShortPosition = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("openLongPositionWithPrice")) {
		        		event = eventReader.nextEvent();
		                stock.openLongPositionWithPrice = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("priceForLongPosition")) {
		        		event = eventReader.nextEvent();
		                stock.priceForLongPosition = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("openShortPositionWithPrice")) {
		        		event = eventReader.nextEvent();
		                stock.openShortPositionWithPrice = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("priceForShortPosition")) {
		        		event = eventReader.nextEvent();
		                stock.priceForShortPosition = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("strategy")) {
		        		event = eventReader.nextEvent();
		                stock.strategyID = Integer.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("price")) {
		        		event = eventReader.nextEvent();
		                stock.price = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("strike_increment")) {
		        		event = eventReader.nextEvent();
		                stock.strike_increment = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("impVol")) {
		        		event = eventReader.nextEvent();
		                stock.impVol = Double.valueOf(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }
		        
		        if (event.isStartElement()) {
		        	if (event.asStartElement().getName().getLocalPart().equals("tradeable")) {
		        		event = eventReader.nextEvent();
		                stock.tradeable = Boolean.parseBoolean(event.asCharacters().getData());
		                continue;
		        	
		             }
			     }


		        if (event.isEndElement()) {
		            EndElement endElement = event.asEndElement();
		            if (endElement.getName().getLocalPart() == "stock") {
		              listOfStocks.add(stock);
		            }
		          }
		        
			}
		 
		} catch (FileNotFoundException e) {
		      log.info(fileName + " file not found " + e);
	          log.info("Exiting program.....");
	          System.exit(1);
		} catch (XMLStreamException e) {
		      log.info("Something went wrong with xml streamer" + e);
	    }
			
		return;
	}
	
	/**
	 * This method update listOfStocks to drive potential actions executed manually by the trader.
	 * <p>
	 * It uses an xml configuration file located in the project root
	 * as a descriptor for the all the tracked stocks. For each stock in the listOfStocks update its parameters.
	 * It gets called by main() every second.
	 * <p>
	 * @author Mario Visco
	 *
	 * @param XML File Name
	 */
	
	public static void updateStock(String fileName) {
		
		// get the last time the xml file has been modified
		File file = new File(fileName);
        Path filePath = file.toPath();
        try
        {
        	BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        	long new_modifiedTime=attributes.lastModifiedTime().toMillis();
        	if (new_modifiedTime == modifiedTime) {
        		// The xml file has not been modified so let's no waste processing time and return
        		return;
        	}
        	modifiedTime=new_modifiedTime;		
        }
        catch (IOException e)
        {
        	// IF we cannot get file attributes, let's log it and continue 
            log.info(fileName + " Cannot get file attributes");
  	        e.printStackTrace();     
        }
		
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();  
		try {
				
			InputStream in = new FileInputStream(fileName);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			Stock modifiedStock = new Stock();
							
			while (eventReader.hasNext()) {
			       XMLEvent event = eventReader.nextEvent();
			        
			       if (event.isStartElement()) {
			            StartElement startElement = event.asStartElement();
			            if (startElement.getName().getLocalPart() == "stock") {
			              continue;
			            }
				   }
			        
			       if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("symbol")) {
			        		event = eventReader.nextEvent();
			                String symbol = event.asCharacters().getData();
			                for (Stock stock : listOfStocks) {
			        			if (stock.symbol.contentEquals(symbol)) {
			        			    modifiedStock=stock;
			        			    break;
			        		     }		        			
			        		}
			                continue;
			        	
			             }
				   }
			        
			       if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("change")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.change = Double.valueOf(event.asCharacters().getData());	
			                continue;		        	
			             }
				   }
			        
			       if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("profit")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.profit = Double.valueOf(event.asCharacters().getData());             
			                continue;		        	
			             }
				   }
			        
			       if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("loss")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.loss = Double.valueOf(event.asCharacters().getData());		        		             
			                continue;		        	
			             }
				     }
			        			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("lot")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.lot = Integer.valueOf(event.asCharacters().getData());              
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("closePosition")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.closePosition = Boolean.parseBoolean(event.asCharacters().getData());            
			                continue;		        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("openLongPosition")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.openLongPosition = Boolean.parseBoolean(event.asCharacters().getData());               
			                continue;		        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("openShortPosition")) {
			        		event = eventReader.nextEvent();
			                modifiedStock.openShortPosition = Boolean.parseBoolean(event.asCharacters().getData());
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("openLongPositionWithPrice")) {
			        		event = eventReader.nextEvent();
			                modifiedStock.openLongPositionWithPrice = Boolean.parseBoolean(event.asCharacters().getData());
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("priceForLongPosition")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.priceForLongPosition = Double.valueOf(event.asCharacters().getData());             
			                continue;		        	
			             }
				     }
			        
			        
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("openShortPositionWithPrice")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.openShortPositionWithPrice = Boolean.parseBoolean(event.asCharacters().getData());               
			                continue;		        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("priceForShortPosition")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.priceForShortPosition = Double.valueOf(event.asCharacters().getData());             
			                continue;		        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("strategy")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.strategyID = Integer.valueOf(event.asCharacters().getData());             
			                continue;		        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("price")) {
			        		event = eventReader.nextEvent();
			                modifiedStock.price = Double.valueOf(event.asCharacters().getData());
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("strike_increment")) {
			        		event = eventReader.nextEvent();
			                modifiedStock.strike_increment = Double.valueOf(event.asCharacters().getData());
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("impVol")) {
			        		event = eventReader.nextEvent();
			                modifiedStock.impVol = Double.valueOf(event.asCharacters().getData());
			                continue;
			        	
			             }
				     }
			        
			        if (event.isStartElement()) {
			        	if (event.asStartElement().getName().getLocalPart().equals("tradeable")) {
			        		event = eventReader.nextEvent();
			        		modifiedStock.tradeable = Boolean.parseBoolean(event.asCharacters().getData());			               
			                continue;
			        	
			             }
				     }
			        
			        if (event.isEndElement()) {
			            EndElement endElement = event.asEndElement();
			            if (endElement.getName().getLocalPart() == "stock") {
			              ;
			            }
			          }
			        
				}
			 
			} catch (FileNotFoundException e) {
			    log.info(fileName + " file not found " + e);
		        
			} catch (XMLStreamException e) {
				log.info("Something went wrong with the xml parsing" + e);
			    
		    }
		
			return;
		}
	
    public double getImpVol() {
		return impVol;
	}

	public void setImpVol(double impVol) {
		this.impVol = impVol;
	}

	public void createStrikes() {

		final int STRIKESNUMBER = 30; // we will have 61 strikes

		BigDecimal priceBD = new BigDecimal(String.valueOf(this.price));
		BigDecimal increment = new BigDecimal(String.valueOf(this.strike_increment));
		RoundingMode rm1;
		rm1 = RoundingMode.valueOf("HALF_DOWN");		
		BigDecimal price_new = priceBD.setScale(0, rm1);

		if (this.strike_increment == 2.5) {
			//for options that have a 2.5 increment we have to start from a multiple of 5. For example if we GOOGL price 
			// set at 852 we will start from 850 to build the lost of strikes. For other options we can start anywhere on the 
			// int version of price for ex. AAPL at 135.33 we will start at 135 and increment either by 1 or by 0.5
			price_new = price_new.subtract(price_new.remainder(new BigDecimal("5.0")));
		}

		for (int i = -STRIKESNUMBER; i <=STRIKESNUMBER; i++) {
			strikes.add(price_new.add(increment.multiply(new BigDecimal(i))));
		}

	}
	
 

	public double getStrike_increment() {
		return strike_increment;
	}

	public void setStrike_increment(double strike_increment) {
		this.strike_increment = strike_increment;
	}

	public void createExpirations() {

		final int EXPIRATIONSNUMBER = 10; // we will have 11 expiration dates
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Calendar rightnow = Calendar.getInstance();
		//start from  the first day of current week
		rightnow.add(Calendar.DAY_OF_WEEK, rightnow.getFirstDayOfWeek()-rightnow.get(Calendar.DAY_OF_WEEK));
		// get the friday of this week
		rightnow.add(Calendar.DAY_OF_MONTH, 5);
		for (int i = 0; i <= EXPIRATIONSNUMBER; i++) {
			// add this Friday and next 10 Fridays to the list
			rightnow.add(Calendar.DAY_OF_MONTH, i*7);
			expirations.add(sdf.format(rightnow.getTime()));
		}
	}

		public List<BigDecimal> getStrikes() {
			return strikes;
		}
		
	    public List<String> getExpirations() {
				return expirations;
		}
	
	}
	

	
	
	


