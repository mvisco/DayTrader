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

package com.TraderLight.DayTrader.AccountMgmt;

/**
 *  This class describes each trading position.
 * 
 * @author Mario Visco
 *
 */

public class StockPosition {
	
	private String symbol;
	private int quantity;
	private boolean long_short;
	private double priceBought;
	private double priceSold;
	private boolean open_close;
		
	public StockPosition(boolean long_short, String symbol, int q, double price) {
		
		this.open_close = true;
		this.long_short = long_short;
		this.symbol = symbol;
		this.quantity = q;
		if (long_short) {
			this.priceBought = price;
		} else {
			this.priceSold = price;
		}
	}
	
	public void updatePosition(int q, double price) {
		
		// this assumes update is of the same quantity that we currently have
		this.quantity = this.quantity+q;
		if (long_short) {
			this.priceBought = (priceBought+price)/2.0;
		} else {
			this.priceSold = (priceSold+price)/2.0;
		}		
	}
	
	public void closePosition(double p) {
		this.open_close = false;
		if (long_short) {
			this.priceSold=p;
		} else {
			this.priceBought = p;
		}
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public boolean getStatus() {
		return open_close;
	}
	
	public boolean getTypeofPosition() {
		return long_short;
	}
	
	public double getPriceBought() {
		return priceBought;
	}
	
	public double getPriceSold() {
		return priceSold;
	}
	
	public int getQuantity() {
		return quantity;
	}

	public double getGain() {
		return (priceSold - priceBought)*quantity;
	}

}
