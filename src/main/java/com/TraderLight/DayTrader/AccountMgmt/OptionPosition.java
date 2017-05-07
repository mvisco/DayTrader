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

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.StockTrader.Logging;






import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

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
	double impVol;
	double underlying_price;
	Date date_acquired;
	public static final Logger log = Logging.getLogger(true);
	Connection con;
	Statement stmt;
	ResultSet rs;



	OptionPosition(String optionSymbol, double priceB, int q, double cost, double impVol, double underlying_price, Date date_acquired){
		this.symbol = optionSymbol;
		this.priceBought =  priceB;
		this.quantity = q;
		this.transactionCost = cost;
		this.impVol=impVol;
		this.date_acquired = date_acquired;
		this.underlying_price = underlying_price;

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
	
	public double getUnderlying_price() {
		return underlying_price;
	}


	public void updateQuantity(int quantity) {

		this.quantity += quantity;

	}

	public void StoreInDB() {

		this.con = null;	
		//Register the JDBC driver for MySQL.
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	public Connection establishConnection(String db) {

		// Only establish  connection if connection not already established
		if (this.con == null ) {		
			
			try {
				this.con = DriverManager.getConnection(db,"root", "password");
				this.stmt = con.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			 
		}
		return (this.con);
	}

	public void closeConnection() {		
		try{
			if(this.con!=null)
				this.con.close();
		}catch(SQLException se){
			se.printStackTrace();
		}	
	}

	public  void storeOptionPosition(OptionPosition option ) {

		//Get a Statement object
		try {

			String symbol = option.getSymbol();
			String stock_symbol = getStockSymbol(symbol);	
			Timestamp date = new java.sql.Timestamp(date_acquired.getTime());
			String insertSQL = "INSERT INTO position (id,symbol,option_symbol,quantity,price_bought,price_sold,imp_vol,transaction_cost, underlying_price,date_acquired ) " 
					+  "VALUES (NULL,?,?,?,?,?,?,?,?,?)";

			PreparedStatement pstmt = con.prepareStatement(insertSQL);

			// Set the values, id is auto increment so we set it to null above
			pstmt.setString(1, stock_symbol);
			pstmt.setString(2, symbol);
			pstmt.setInt(3,option.getQuantity());
			pstmt.setDouble(4,option.getPriceBought());		
			pstmt.setDouble(5,option.getPriceSold());
			pstmt.setDouble(6, option.impVol);
			pstmt.setDouble(7, option.transactionCost);
			pstmt.setDouble(8, option.underlying_price);
			pstmt.setTimestamp(9, date);

			// Insert 
			log.info("Storing in DB option position for symbol " + symbol);
			pstmt.executeUpdate();
			pstmt.close();

			// log.info("Done with the insert");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			 e.printStackTrace();
		}
	}
	
	public  void storeOptionPosition(OptionPosition option, Date date ) {

		//Get a Statement object
		try {

			String symbol = option.getSymbol();
			String stock_symbol = getStockSymbol(symbol);
			Timestamp param = new java.sql.Timestamp(date.getTime());
			String insertSQL = "INSERT INTO trade (id,date_time,symbol,option_symbol,quantity,price_bought,price_sold,imp_vol,transaction_cost) " 
					+  "VALUES (NULL,?,?,?,?,?,?,?,?)";

			PreparedStatement pstmt = con.prepareStatement(insertSQL);

			// Set the values, id is auto increment so we set it to null above
			pstmt.setTimestamp(1, param);
			pstmt.setString(2, stock_symbol);
			pstmt.setString(3, symbol);
			pstmt.setInt(4,option.getQuantity());
			pstmt.setDouble(5,option.getPriceBought());		
			pstmt.setDouble(6,option.getPriceSold());
			pstmt.setDouble(7, option.impVol);
			pstmt.setDouble(8, option.transactionCost);

			// Insert 
			log.info("Storing in DB option position for symbol " + symbol);
			pstmt.executeUpdate();
			pstmt.close();

			// log.info("Done with the insert");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			 e.printStackTrace();
		}
	}

	public String getStockSymbol(String optionSymbol) {

		String[] symbolSplit = optionSymbol.split(":");
		return symbolSplit[0];

	}
	
	public  void updateOptionPosition(OptionPosition option ) {
		
		String selectOption = "SELECT * FROM position where OPTION_SYMBOL='"+option.getSymbol()+"'";
		int id;
		
		try {
			Statement statement = option.con.createStatement();
			ResultSet rs = statement.executeQuery(selectOption);
			while (rs.next()) {
			   id  = rs.getInt("ID");
			   String update = "update position set quantity="+option.getQuantity()+", imp_vol="+option.impVol+ " where id="+id;
			   statement.executeUpdate(update);
			   break;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public  void deleteOptionPosition(OptionPosition option ) {
		
		String selectOption = "SELECT * FROM position where OPTION_SYMBOL='"+option.getSymbol()+"'";
		int id;
		
		try {
			Statement statement = option.con.createStatement();
			ResultSet rs = statement.executeQuery(selectOption);
			while (rs.next()) {
			   id  = rs.getInt("ID");
			   String update = "delete from position where id=" +id;
			   statement.executeUpdate(update);
			   break;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public static void main(String[] args) {
		
		OptionPosition option = new OptionPosition("AAPL:20170413:142:P",3.5, 10000, 100, 0.15, 130, Calendar.getInstance().getTime() );
		option.StoreInDB();
		option.establishConnection("jdbc:mysql://localhost/Positions");
/*
		String selectOption = "SELECT * FROM position where SYMBOL='"+"GOOGL"+"'";
		try {
			Statement statement = option.con.createStatement();
			ResultSet rs = statement.executeQuery(selectOption);
			while (rs.next()) {
			   log.info( rs.getInt("ID"));
			   log.info(rs.getString("SYMBOL"));
			   log.info(rs.getString("OPTION_SYMBOL"));
			   log.info(rs.getInt("QUANTITY"));
			   log.info(rs.getDouble("PRICE_BOUGHT"));
			   log.info(rs.getDouble("PRICE_SOLD"));
			   log.info(rs.getDouble("IMP_VOL"));
			   log.info(rs.getDouble("TRANSACTION_COST"));
			   
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
		option.storeOptionPosition(option);
		log.info("Created Position");

		//option.updateQuantity(6000);
		//option.impVol = 0.20;
		//option.updateOptionPosition(option);
		//log.info("Updated Position");
/*
		//option.deleteOptionPosition(option);
		log.info("Deleted Position");
*/		
		option.closeConnection();
		
		
	}
}
