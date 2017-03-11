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
 *  This class describes an option trading position.
 * 
 * @author Mario Visco
 *
 */

public class OptionPosition {
	
	
	String symbol;
	double priceBought;
	double priceSold;
	int quantity;
	double transactionCost;
	
	
	OptionPosition(String optionSymbol, double priceB, int q, double cost){
		this.symbol = optionSymbol;
		this.priceBought =  priceB;
		this.quantity = q;
		this.transactionCost = cost;
		
	}
	
	// getters

	public String getSymbol() {
		return symbol;
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
	 
	public  double getCost() {
		return transactionCost;
	}

	public void setPriceSold(double priceSold) {
		this.priceSold = priceSold;
	}
	
	public void updateCost(double cost) {
		
		this.transactionCost += cost;
		
	}
	
   public void updateQuantity(int quantity) {
		
		this.quantity += quantity;
		
	}
	
	

}
