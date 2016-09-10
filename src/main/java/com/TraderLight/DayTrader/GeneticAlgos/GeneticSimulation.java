package com.TraderLight.DayTrader.GeneticAlgos;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.TraderLight.DayTrader.AccountMgmt.AccountMgr;
import com.TraderLight.DayTrader.AccountMgmt.StockPosition;
import com.TraderLight.DayTrader.MarketDataProvider.GetFromStorageJDBC;
import com.TraderLight.DayTrader.MarketDataProvider.Level1Quote;
import com.TraderLight.DayTrader.StockTrader.Logging;
import com.TraderLight.DayTrader.StockTrader.Stock;
import com.TraderLight.DayTrader.Strategy.Strategy;




public class GeneticSimulation {
	
	
	public static final Logger log = Logging.getLogger(true);	
	public static Map<String,List<StockPosition>> allTrades=new HashMap<String,List<StockPosition>>();	
	
	List<Stock> listOfStocks;
	AccountMgr account;
	Map<String,Stock> mapOfStocks;
	int morningOpenIndex;
	int closeOpeningIndex;
	GetFromStorageJDBC getRecord;
	String symbol;
	Connection con;
	double objective_change;
	
	
	public GeneticSimulation(String symbol, AccountMgr account,  Map<String,Stock> mapOfStocks, int morningOpenIndex, 
			int closeOpeningIndex, GetFromStorageJDBC getRecord, List<Stock> listOfStocks, Connection con, double change) {
		
		this.symbol = symbol;
		this.account = account;
		this.mapOfStocks = mapOfStocks;
		this.morningOpenIndex = morningOpenIndex;
		this.closeOpeningIndex = closeOpeningIndex;
		this.getRecord = getRecord;
		this.con=con;
		this.listOfStocks=listOfStocks;
		this.objective_change=change;
		
	}
	
	public void execute() {
		
		
		log.info("Running genetic algo for symbol " + symbol + " on indexes " + morningOpenIndex + " " + closeOpeningIndex);
		log.info("Allocating chromosomes");
		int maxNumberOfChromosomes = 5;
		double infLimit=objective_change-0.50*(objective_change);
		double supLimit=objective_change+0.5*(objective_change);
		
		List<ChromosomeObject> listChromosomes = new ArrayList<ChromosomeObject>();
		PopulationChromosomes population = new PopulationChromosomes(listChromosomes,maxNumberOfChromosomes);
		
		for (int i =1; i <= maxNumberOfChromosomes; i++) {
			ChromosomeObject c = new ChromosomeObject();
			
			if (i == 1) {
				// populate the first one based on deterministic values
				c.objective_change = this.objective_change;
				c.profit= this.objective_change;
				c.loss=this.objective_change;
				population.addChromosome(c);				
			} else {
				c.objective_change = c.generateRandomDouble(infLimit, supLimit);
				c.profit = c.generateRandomDouble(infLimit, supLimit);
				c.loss = c.generateRandomDouble(infLimit, supLimit);
				c.profit= c.generateRandomDouble(infLimit, supLimit);
				population.addChromosome(c);
			}		
		}
		
		//log.info("Print the list of initial chromosomes for symbol " + symbol);
		//log.info("  ");
		//population.printChromosomes();
		
		//get the quotes for this symbol
		List<Level1Quote> l = new ArrayList<Level1Quote>();
		l=getRecord.getLevel1QuoteList(con, symbol, morningOpenIndex, closeOpeningIndex);
		
		// do the simulation with the first generation of chromosomes
	    for (ChromosomeObject c : population.getChromosomes()) {
	    	 
	    	 
	    	c.fitness_value = this.runSimulation(c,l);
		
	    }
	    log.info("Print the list of initial chromosomes with updated fitness value for symbol " + symbol);
		log.info("  ");
		population.printChromosomes();
	  // calculate next generations of chromosome
	  // sort Chromosomes first
	  population.sortChromosomes();
	  // get the two with higher fitness and recombine but only if both fitness are positive
	  
	  ChromosomeObject a = population.chromosomes.get(population.chromosomes.size() -1);
	  ChromosomeObject b = population.chromosomes.get(population.chromosomes.size() -2);
	  ChromosomeObject newChromosome;
	  //if (a.fitness()>0 && b.fitness() > 0) {	 
	      // clear the population of existing chromosomes
	      population.chromosomes.clear();
	      // add the new chromosomes
	      newChromosome = ChromosomeUtils.crossover1(a, b);
	      population.chromosomes.add(newChromosome);
	  	     
	      newChromosome = ChromosomeUtils.crossover1(b, a);
	      population.chromosomes.add(newChromosome);
	  	     
	      newChromosome = ChromosomeUtils.crossover2(a, b);
	      population.chromosomes.add(newChromosome);
	  	     
	      newChromosome = ChromosomeUtils.crossover1(b, a);
	      population.chromosomes.add(newChromosome);
	  	     
	      newChromosome = ChromosomeUtils.mutation(a, infLimit, supLimit);
	      population.chromosomes.add(newChromosome);
	  	     
	      //log.info("  ");
	      //log.info("This is the next generation of Chromosomes for symbol " + symbol);
	  	     
	     //population.printChromosomes();
	  	     
	     // do the simulation with the second generation of chromosomes
	     for (ChromosomeObject c : population.getChromosomes()) {
	  	    	 
		   c.fitness_value = this.runSimulation(c,l);
	     }
	  	
	     log.info("Print the second generation of chromosomes with updated fitness value");
	     log.info("  ");
	     population.sortChromosomes();
	     population.printChromosomes();
	    // get the highest fit
	    a = population.chromosomes.get(population.chromosomes.size() -1);
	    log.info("---------------------------------");
	    log.info("This is the final chromosome for symbol " + symbol);
	    log.info("---------------------------------");
	    a.printChromosome();
	    
	//  }
	    // update strategy to run 
	  Strategy strategy=null;
		for (Stock stock : listOfStocks ) {
			if (stock.getSymbol().contentEquals(symbol)) {
				// we got the stock we need
				 strategy = stock.getStrategy();
				 if (a.fitness()<=0) {
					 strategy.setTradeableFlag(false);
				 } else {
					 strategy.setTradeableFlag(true);
					 strategy.updateChangeProfitLoss(a.objective_change, a.profit, a.loss);
				 }
				 break;
			}
		}
		
	}	
		
	double runSimulation(ChromosomeObject c, List<Level1Quote> l) {
		
		// update Strategy with the chromosome values
		Strategy strategy=null;		
		for (Stock stock : listOfStocks ) {
			if (stock.getSymbol().contentEquals(symbol)) {
				// we got the stock we need
				 strategy = stock.getStrategy();
				 strategy.updateChangeProfitLoss(c.objective_change, c.profit, c.loss);
				 strategy.updateDisplay(false);
				 strategy.setTradeableFlag(true);
				 break;
			}
		}		
		
		int i =0;
		for (Level1Quote newQuote : l) {
			i++;
			if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
				continue;
			}
			if ( !newQuote.getSymbol().contentEquals(symbol)) {
				continue;
			}
			if (i >= (l.size() - 50)) {
				// Close all positions we are in the last 50 quotes
				if ( (newQuote == null) || newQuote.getSymbol().contentEquals("") ) {
					continue;
				}
				if (mapOfStocks.containsKey(newQuote.getSymbol()) && (mapOfStocks.get(newQuote.getSymbol()).getTradeable()==true)) {
					mapOfStocks.get(newQuote.getSymbol()).getStrategy().closePositions(newQuote);
					
				}
				continue;
			}
			
			// send quote to strategy
			if (mapOfStocks.containsKey(newQuote.getSymbol())) {
				mapOfStocks.get(newQuote.getSymbol()).getStrategy().stateTransition(newQuote);
							
			}
			
		}
		
	    account.analyzeTrades(false);
	    double totalGain=account.getTotalGain();
		account.resetAcctMgr();
		strategy.setStateToS0();
		strategy.clearMean();		
		return totalGain;
		
	}	
		
	
	
	

}
