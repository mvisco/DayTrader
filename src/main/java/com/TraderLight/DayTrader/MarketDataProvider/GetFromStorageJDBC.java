package com.TraderLight.DayTrader.MarketDataProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import com.TraderLight.DayTrader.StockTrader.Logging;



public class GetFromStorageJDBC {
	public static final Logger log = Logging.getLogger(true);
	Connection con;
	Statement stmt;
	ResultSet rs;
	
	
	public GetFromStorageJDBC() {
		
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
			// String url = "jdbc:mysql://192.168.1.108/traderlight2016-1";			 
			// String url = "jdbc:mysql://localhost/traderlight2014";
			String url = "jdbc:mysql://localhost/" + db;
				try {
					this.con = DriverManager.getConnection(url,"root", "password");
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
	

	public  List<Level1Quote> getLevel1QuoteList(Connection dbConnection) {
		
		 
		List<Level1Quote> l = new ArrayList<Level1Quote>();
		//Get a Statement object
		 try {
			//log.info("Executing query");
			 String query;
			 
			 // get the last 50 quotes from DB and provide them to the caller
			 query = "SELECT * from levelonequote ORDER BY id DESC LIMIT 50";
			 
			// log.info("query is " + query);
			rs = stmt.executeQuery(query);
			 while(rs.next()){
				 Level1Quote quote = new Level1Quote();
				 quote.symbol = rs.getString("symbol");
				 quote.last = rs.getDouble("last");
				// quote.id = rs.getInt("id");
				 quote.currentDateTime = rs.getTimestamp("current_date_time");
				 quote.ask = rs.getDouble("ask");
				 quote.askSize = rs.getInt("ask_size");
				 quote.avgTrade = rs.getInt("avg_trades");
				 quote.bid = rs.getDouble("bid");
				 quote.bidSize = rs.getInt("bid_size");
				 quote.change = rs.getDouble("change");
				 quote.high = rs.getDouble("high");
				 quote.high52week = rs.getDouble("high_52_week");
				 //quote.isin = rs.getString("stock_isin");
				 quote.lastDateTime = rs.getTimestamp("last_date_time");
				 quote.lastVolume = rs.getInt("last_volume");
				 quote.low = rs.getDouble("low");
				 quote.low52week = rs.getDouble("low_52_week");
				 quote.numTrades = rs.getInt("num_trades");
				 quote.open = rs.getDouble("open");
				 //TODO find the right return value type for tick. We do not really use it so 0 is ok for now
				 quote.tick = 0;
				 quote.volume = rs.getInt("vol");	
				// log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
				 l.add(quote);
			 }
			// log.info("Done with the query");
			
		   } catch (SQLException e) {
			     // TODO Auto-generated catch block
			     e.printStackTrace();
			     return null;
		   }
		 //  log.info("symbol is " + quote.getSymbol());
		 //  log.info("Volume is " + quote.getVolume());
		  // log.info("index is  " + index);
		 
		return l;

		}
}
