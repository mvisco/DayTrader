package com.TraderLight.DayTrader.MarketDataProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
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
	
	public Connection establishConnection() {
		
		// Only establish  connection if connection not already established
		if (this.con == null ) {		
			// String url = "jdbc:mysql://192.168.1.108/traderlight2016-1";			 
			 String url = "jdbc:mysql://localhost/traderlight2014";
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
	
	public  Level1Quote getLevel1Quote(int index, Connection dbConnection, Level1Quote quote ) {
	
	 //Get a Statement object
	 try {
		//log.info("Executing query");
		rs = stmt.executeQuery("SELECT * from levelonequote where id=" + index);
		 while(rs.next()){
			 quote.symbol = rs.getString("symbol");
			 quote.last = rs.getDouble("last");
			 quote.id = rs.getInt("id");
			 quote.currentDateTime = rs.getTimestamp("current_date_time");
			 quote.ask = rs.getDouble("ask");
			 quote.askSize = rs.getInt("ask_size");
			 quote.avgTrade = rs.getInt("avg_trades");
			 quote.bid = rs.getDouble("bid");
			 quote.bidSize = rs.getInt("bid_size");
			 quote.change = rs.getDouble("change");
			 quote.high = rs.getDouble("high");
			 quote.high52week = rs.getDouble("high_52_week");
			 quote.isin = rs.getString("stock_isin");
			 quote.lastDateTime = rs.getTimestamp("last_date_time");
			 quote.lastVolume = rs.getInt("last_volume");
			 quote.low = rs.getDouble("low");
			 quote.low52week = rs.getDouble("low_52_week");
			 quote.numTrades = rs.getInt("num_trades");
			 quote.open = rs.getDouble("open");
			 //TODO find the right return value type for tick. We donot really use it so 0 is ok for now
			 quote.tick = 0;
			 quote.volume = rs.getInt("vol");	
			// log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
		 }
		// log.info("Done with the query");
		
	   } catch (SQLException e) {
		     // TODO Auto-generated catch block
		     e.printStackTrace();
		     return null;
	   }
	  // log.info("symbol is " + quote.getSymbol());
	 //  log.info("Volume is " + quote.getVolume());
	  // log.info("index is  " + index);
	return quote;

	}

	public static void main(String[] args) {
		
		Level1Quote quote = new Level1Quote();
		GetFromStorageJDBC getRecord = new GetFromStorageJDBC();
		
		// get connection
		Connection con = getRecord.establishConnection();
		//get record
			
		log.info("Getting First  object");
		quote = getRecord.getLevel1Quote(1002450, con, quote);
		log.info("Done");
		log.info("Getting Second object");
		quote = getRecord.getLevel1Quote(1002451, con, quote);
		log.info("Done");
		log.info("Getting Third object");
		quote = getRecord.getLevel1Quote(1002452, con, quote);
		log.info("Done");		
		log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
		getRecord.closeConnection();
		
	}
	
	public  Level1Quote getLevel1Quote(int index, Connection dbConnection, Level1Quote quote, String symbol ) {
		
		 //Get a Statement object
		 try {
			//log.info("Executing query");
			 
			rs = stmt.executeQuery("SELECT * from levelonequote where (id=" + index + " and symbol='" + symbol + "')");
			 while(rs.next()){
				 quote.symbol = rs.getString("symbol");
				 quote.last = rs.getDouble("last");
				 quote.id = rs.getInt("id");
				 quote.currentDateTime = rs.getTimestamp("current_date_time");
				 quote.ask = rs.getDouble("ask");
				 quote.askSize = rs.getInt("ask_size");
				 quote.avgTrade = rs.getInt("avg_trades");
				 quote.bid = rs.getDouble("bid");
				 quote.bidSize = rs.getInt("bid_size");
				 quote.change = rs.getDouble("change");
				 quote.high = rs.getDouble("high");
				 quote.high52week = rs.getDouble("high_52_week");
				 quote.isin = rs.getString("stock_isin");
				 quote.lastDateTime = rs.getTimestamp("last_date_time");
				 quote.lastVolume = rs.getInt("last_volume");
				 quote.low = rs.getDouble("low");
				 quote.low52week = rs.getDouble("low_52_week");
				 quote.numTrades = rs.getInt("num_trades");
				 quote.open = rs.getDouble("open");
				 //TODO find the right return value type for tick. We donot really use it so 0 is ok for now
				 quote.tick = 0;
				 quote.volume = rs.getInt("vol");	
				// log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
			 }
			// log.info("Done with the query");
			
		   } catch (SQLException e) {
			     // TODO Auto-generated catch block
			     e.printStackTrace();
			     return null;
		   }
		   //log.info("symbol is " + quote.getSymbol());
		   //log.info("Volume is " + quote.getVolume());
		  // log.info("index is  " + index);
		return quote;

		}
	
	
	public  Level1Quote getLevel1Quote(int index, Connection dbConnection, Level1Quote quote, String symbol, int range ) {
		
		 //Get a Statement object
		 try {
			//log.info("Executing query");
			 int end_index = index+range;
			 
			 String query = "SELECT * from levelonequote where id BETWEEN " + index + " AND " + end_index + " and symbol ='" + symbol + "'";
			// log.info("query is " + query);
			rs = stmt.executeQuery(query);
			 while(rs.next()){
				 quote.symbol = rs.getString("symbol");
				 quote.last = rs.getDouble("last");
				 quote.id = rs.getInt("id");
				 quote.currentDateTime = rs.getTimestamp("current_date_time");
				 quote.ask = rs.getDouble("ask");
				 quote.askSize = rs.getInt("ask_size");
				 quote.avgTrade = rs.getInt("avg_trades");
				 quote.bid = rs.getDouble("bid");
				 quote.bidSize = rs.getInt("bid_size");
				 quote.change = rs.getDouble("change");
				 quote.high = rs.getDouble("high");
				 quote.high52week = rs.getDouble("high_52_week");
				 quote.isin = rs.getString("stock_isin");
				 quote.lastDateTime = rs.getTimestamp("last_date_time");
				 quote.lastVolume = rs.getInt("last_volume");
				 quote.low = rs.getDouble("low");
				 quote.low52week = rs.getDouble("low_52_week");
				 quote.numTrades = rs.getInt("num_trades");
				 quote.open = rs.getDouble("open");
				 //TODO find the right return value type for tick. We donot really use it so 0 is ok for now
				 quote.tick = 0;
				 quote.volume = rs.getInt("vol");	
				// log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
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
		 
		return quote;

		}
	
	public  Level1Quote getLevel1Quote(Connection dbConnection, Level1Quote quote, String symbol, Date date1, Date date2) {
		
		 //Get a Statement object
		 try {
			//log.info("Executing query");
			 			 
			 String query = "SELECT * from levelonequote where CURRENT_DATE_TIME BETWEEN '" + date1 + "' AND '" + date2 + "' and symbol ='" + symbol + "'";
			// log.info("query is " + query);
			rs = stmt.executeQuery(query);
			 while(rs.next()){
				 quote.symbol = rs.getString("symbol");
				 quote.last = rs.getDouble("last");
				 quote.id = rs.getInt("id");
				 quote.currentDateTime = rs.getTimestamp("current_date_time");
				 quote.ask = rs.getDouble("ask");
				 quote.askSize = rs.getInt("ask_size");
				 quote.avgTrade = rs.getInt("avg_trades");
				 quote.bid = rs.getDouble("bid");
				 quote.bidSize = rs.getInt("bid_size");
				 quote.change = rs.getDouble("change");
				 quote.high = rs.getDouble("high");
				 quote.high52week = rs.getDouble("high_52_week");
				 quote.isin = rs.getString("stock_isin");
				 quote.lastDateTime = rs.getTimestamp("last_date_time");
				 quote.lastVolume = rs.getInt("last_volume");
				 quote.low = rs.getDouble("low");
				 quote.low52week = rs.getDouble("low_52_week");
				 quote.numTrades = rs.getInt("num_trades");
				 quote.open = rs.getDouble("open");
				 //TODO find the right return value type for tick. We donot really use it so 0 is ok for now
				 quote.tick = 0;
				 quote.volume = rs.getInt("vol");	
				// log.info("Quote is " + quote.id + " " + quote.symbol + " " + quote.currentDateTime + " Last date time " + quote.lastDateTime);
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
		 
		return quote;

		}
	

}
