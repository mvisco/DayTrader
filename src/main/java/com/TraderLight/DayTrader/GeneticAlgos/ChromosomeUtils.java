package com.TraderLight.DayTrader.GeneticAlgos;

public class ChromosomeUtils {
	
	
	
	public static ChromosomeObject crossover1(ChromosomeObject a , ChromosomeObject b) {
		
		// return a new chromosome with the first half of genes equal to a and second half equal to b
		
		ChromosomeObject c = new ChromosomeObject();
		c.objective_change = a.objective_change;
		c.profit = b.profit;
		c.loss = b.loss;
		return c;			
	}
	
	public static ChromosomeObject crossover2(ChromosomeObject a , ChromosomeObject b) {
		// return a new chromosome with interleaved genes
		ChromosomeObject c = new ChromosomeObject();
		c.objective_change = b.objective_change;
		c.profit = a.profit;	
		c.loss=b.loss;
		return c;				
	}
	
	public static ChromosomeObject mutation(ChromosomeObject a, double infLimit, double supLimit) {
		// return the same chromosome with the profit gene randomly modified  		
		a.profit = a.generateRandomDouble(infLimit, supLimit);		
		return a;
	}
	

}
